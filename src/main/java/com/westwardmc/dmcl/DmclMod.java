package com.westwardmc.dmcl;

import com.westwardmc.dmcl.adapter.config.ConfigValidator;
import com.westwardmc.dmcl.adapter.config.DmclConfig;
import com.westwardmc.dmcl.adapter.config.InMemoryChannelMap;
import com.westwardmc.dmcl.adapter.config.SecretResolver;
import com.westwardmc.dmcl.adapter.config.TomlConfigLoader;
import com.westwardmc.dmcl.adapter.fabric.ChatEventHook;
import com.westwardmc.dmcl.adapter.fabric.FabricMinecraftAdapter;
import com.westwardmc.dmcl.adapter.fabric.LifecycleHook;
import com.westwardmc.dmcl.adapter.fabric.McHeadsAvatarService;
import com.westwardmc.dmcl.adapter.fabric.McLinkCommand;
import com.westwardmc.dmcl.adapter.fabric.PlayerEventHook;
import com.westwardmc.dmcl.adapter.jda.JdaDiscordAdapter;
import com.westwardmc.dmcl.adapter.jda.JdaMessageListener;
import com.westwardmc.dmcl.adapter.jda.SlashCommandRouter;
import com.westwardmc.dmcl.adapter.jda.WebhookCache;
import com.westwardmc.dmcl.adapter.sqlite.SqliteLinkRepo;
import com.westwardmc.dmcl.core.orchestrator.BridgeOrchestrator;
import com.westwardmc.dmcl.core.port.Clock;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DmclMod implements DedicatedServerModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("dmcl");

    private static volatile FabricMinecraftAdapter fabricAdapter;

    public static FabricMinecraftAdapter fabricAdapter() { return fabricAdapter; }
    public static void setFabricAdapter(FabricMinecraftAdapter adapter) { fabricAdapter = adapter; }

    private DmclConfig cfg;
    private SqliteLinkRepo links;
    private JDA jda;
    private BridgeOrchestrator orch;

    private static final String TOKEN_PLACEHOLDER = "PASTE_YOUR_DISCORD_BOT_TOKEN_HERE";

    @Override
    public void onInitializeServer() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Path tomlPath = configDir.resolve("dmcl.toml");
            ensureDefaultConfig(tomlPath);

            var resolver = new SecretResolver(System.getenv(),
                configDir.resolve("dmcl.env"), configDir.resolve("dmcl.secrets.toml"));
            try {
                cfg = new TomlConfigLoader(resolver).load(tomlPath);
            } catch (RuntimeException e) {
                LOG.error("DMCL disabled: config could not load. Edit {} and restart. Cause: {}",
                    tomlPath, e.getMessage());
                return;
            }

            if (TOKEN_PLACEHOLDER.equals(cfg.discordToken()) || cfg.discordToken().isBlank()) {
                LOG.error("DMCL disabled: no Discord bot token set. Edit {} and either paste your token inline "
                    + "(token = \"MTxxxx...\") or use env:DMCL_DISCORD_TOKEN with the env var set.", tomlPath);
                return;
            }

            List<String> errs = ConfigValidator.validate(cfg);
            if (!errs.isEmpty()) {
                errs.forEach(e -> LOG.error("DMCL config error: {}", e));
                LOG.error("DMCL disabled until config errors above are fixed.");
                return;
            }

            ServerLifecycleEvents.SERVER_STARTING.register(this::start);
            ServerLifecycleEvents.SERVER_STOPPING.register(this::stop);
        } catch (Exception e) {
            LOG.error("DMCL disabled: unexpected init error. Server will continue without the bridge.", e);
        }
    }

    private void start(MinecraftServer server) {
        try {
            Path dbDir = "global".equals(cfg.storage().dbPath())
                ? FabricLoader.getInstance().getGameDir().resolve("dmcl")
                : server.getSavePath(WorldSavePath.ROOT).resolve("dmcl");
            Files.createDirectories(dbDir);
            links = new SqliteLinkRepo("jdbc:sqlite:" + dbDir.resolve("links.db"));
            links.migrate();

            jda = JDABuilder.createDefault(cfg.discordToken(),
                    GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .setActivity(Activity.watching(cfg.status()))
                .build()
                .awaitReady();

            var webhooks = new WebhookCache(jda);
            var channels = new InMemoryChannelMap(cfg.channels());
            var discord = new JdaDiscordAdapter(jda, webhooks, channels);
            var avatars = new McHeadsAvatarService(cfg.avatars().size());
            var fabric = new FabricMinecraftAdapter(server, avatars);
            setFabricAdapter(fabric);

            orch = new BridgeOrchestrator(discord, fabric, links, channels,
                Clock.system(), cfg.behavior().pingSound());
            orch.start();

            jda.addEventListener(new JdaMessageListener(discord.inboundHandler(),
                discord.reactionHandler(), webhooks, channels));
            var router = new SlashCommandRouter(orch, links, fabric);
            jda.addEventListener(router);
            router.register(jda);

            ChatEventHook.register(fabric, avatars);
            LifecycleHook.register(fabric);
            PlayerEventHook.register(fabric);
            McLinkCommand.register(orch, links);

            LOG.info("DMCL ready");
        } catch (Exception e) {
            LOG.error("DMCL start failed", e);
        }
    }

    private void stop(MinecraftServer server) {
        try {
            if (orch != null) orch.shutdown();
            if (jda != null) jda.shutdown();
            if (links != null) links.close();
        } catch (Exception e) {
            LOG.warn("DMCL shutdown error", e);
        }
    }

    private void ensureDefaultConfig(Path tomlPath) throws Exception {
        if (Files.exists(tomlPath)) return;
        Files.createDirectories(tomlPath.getParent());
        try (var in = DmclMod.class.getResourceAsStream("/dmcl.default.toml")) {
            if (in == null) throw new IllegalStateException("dmcl.default.toml missing from jar");
            Files.copy(in, tomlPath);
        }
        LOG.info("Wrote default DMCL config to {} - please edit before next start", tomlPath);
    }
}
