package com.n1ghtlydev.dmcl.bootstrap;

import com.n1ghtlydev.dmcl.adapter.avatar.McHeadsAvatarService;
import com.n1ghtlydev.dmcl.adapter.config.ConfigValidator;
import com.n1ghtlydev.dmcl.adapter.config.DmclConfig;
import com.n1ghtlydev.dmcl.adapter.config.InMemoryChannelMap;
import com.n1ghtlydev.dmcl.adapter.config.SecretResolver;
import com.n1ghtlydev.dmcl.adapter.config.TomlConfigLoader;
import com.n1ghtlydev.dmcl.adapter.jda.JdaDiscordAdapter;
import com.n1ghtlydev.dmcl.adapter.jda.JdaMessageListener;
import com.n1ghtlydev.dmcl.adapter.jda.SlashCommandRouter;
import com.n1ghtlydev.dmcl.adapter.jda.WebhookCache;
import com.n1ghtlydev.dmcl.adapter.sqlite.SqliteLinkRepo;
import com.n1ghtlydev.dmcl.core.orchestrator.BridgeOrchestrator;
import com.n1ghtlydev.dmcl.core.port.AvatarService;
import com.n1ghtlydev.dmcl.core.port.Clock;
import com.n1ghtlydev.dmcl.core.port.LinkRepo;
import com.n1ghtlydev.dmcl.core.port.MinecraftPort;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/** Platform-independent startup shared by every loader/plugin target. */
public final class DmclBootstrap {
    private static final String TOKEN_PLACEHOLDER = "PASTE_YOUR_DISCORD_BOT_TOKEN_HERE";

    private final DmclConfig cfg;

    private DmclBootstrap(DmclConfig cfg) { this.cfg = cfg; }

    /** Returns null (after logging why) when the bridge must stay disabled. */
    public static DmclBootstrap load(Path configDir, Logger log) throws Exception {
        Path tomlPath = configDir.resolve("dmcl.toml");
        ensureDefaultConfig(tomlPath, log);
        var resolver = new SecretResolver(System.getenv(),
            configDir.resolve("dmcl.env"), configDir.resolve("dmcl.secrets.toml"));
        DmclConfig cfg;
        try {
            cfg = new TomlConfigLoader(resolver).load(tomlPath);
        } catch (RuntimeException e) {
            log.error("DMCL disabled: config could not load. Edit {} and restart. Cause: {}",
                tomlPath, e.getMessage());
            return null;
        }
        if (TOKEN_PLACEHOLDER.equals(cfg.discordToken()) || cfg.discordToken().isBlank()) {
            log.error("DMCL disabled: no Discord bot token set. Edit {} and either paste your token inline "
                + "(token = \"MTxxxx...\") or use env:DMCL_DISCORD_TOKEN with the env var set.", tomlPath);
            return null;
        }
        List<String> errs = ConfigValidator.validate(cfg);
        if (!errs.isEmpty()) {
            errs.forEach(e -> log.error("DMCL config error: {}", e));
            log.error("DMCL disabled until config errors above are fixed.");
            return null;
        }
        return new DmclBootstrap(cfg);
    }

    /** True when the link database should live in the world folder rather than the game dir. */
    public boolean dbPerWorld() { return !"global".equals(cfg.storage().dbPath()); }

    public Running start(Path dbDir, Function<AvatarService, MinecraftPort> mcFactory) throws Exception {
        Files.createDirectories(dbDir);
        var links = new SqliteLinkRepo("jdbc:sqlite:" + dbDir.resolve("links.db"));
        links.migrate();
        JDA jda = JDABuilder.createDefault(cfg.discordToken(),
                GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MESSAGE_REACTIONS)
            .setActivity(Activity.watching(cfg.status()))
            .build()
            .awaitReady();

        var webhooks = new WebhookCache(jda);
        var channels = new InMemoryChannelMap(cfg.channels());
        var discord = new JdaDiscordAdapter(jda, webhooks, channels);
        var avatars = new McHeadsAvatarService(cfg.avatars().size());
        var mc = mcFactory.apply(avatars);

        var orch = new BridgeOrchestrator(discord, mc, links, channels,
            Clock.system(), cfg.behavior().pingSound());
        orch.start();

        jda.addEventListener(new JdaMessageListener(discord.inboundHandler(),
            discord.reactionHandler(), webhooks, channels));
        var router = new SlashCommandRouter(orch, links, mc);
        jda.addEventListener(router);
        router.register(jda);
        return new Running(orch, links, avatars, jda);
    }

    public record Running(BridgeOrchestrator orchestrator, LinkRepo links, AvatarService avatars, JDA jda) {
        public void stop() throws Exception {
            orchestrator.shutdown();
            jda.shutdown();
            if (links instanceof AutoCloseable c) c.close();
        }
    }

    private static void ensureDefaultConfig(Path tomlPath, Logger log) throws Exception {
        if (Files.exists(tomlPath)) return;
        Files.createDirectories(tomlPath.getParent());
        try (var in = DmclBootstrap.class.getResourceAsStream("/dmcl.default.toml")) {
            if (in == null) throw new IllegalStateException("dmcl.default.toml missing from jar");
            Files.copy(in, tomlPath);
        }
        log.info("Wrote default DMCL config to {} - please edit before next start", tomlPath);
    }
}
