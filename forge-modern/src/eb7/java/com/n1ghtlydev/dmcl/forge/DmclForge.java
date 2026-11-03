package com.n1ghtlydev.dmcl.forge;

import com.n1ghtlydev.dmcl.adapter.mc.McCompat;
import com.n1ghtlydev.dmcl.adapter.mc.McLinkCommand;
import com.n1ghtlydev.dmcl.adapter.mc.VanillaMinecraftAdapter;
import com.n1ghtlydev.dmcl.bootstrap.DmclBootstrap;
import com.n1ghtlydev.dmcl.core.domain.Author;
import com.n1ghtlydev.dmcl.core.domain.BridgeMessage;
import com.n1ghtlydev.dmcl.core.domain.Scope;
import com.n1ghtlydev.dmcl.core.domain.Source;
import com.n1ghtlydev.dmcl.core.port.LifecycleEvent;
import com.n1ghtlydev.dmcl.core.port.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mod("dmcl")
public final class DmclForge {
    private static final Logger LOG = LoggerFactory.getLogger("dmcl");

    private DmclBootstrap boot;
    private volatile DmclBootstrap.Running running;
    private volatile VanillaMinecraftAdapter adapter;

    public DmclForge() {
        try {
            boot = DmclBootstrap.load(FMLPaths.CONFIGDIR.get(), LOG);
        } catch (Exception e) {
            LOG.error("DMCL disabled: unexpected init error. Server will continue without the bridge.", e);
        }
        // EventBus 7 (Forge 1.21.6+): listeners go on each event's BUS. Commands are registered whether or not the bridge is enabled; they report "not running".
        RegisterCommandsEvent.BUS.addListener(this::onCommands);
        ServerStartingEvent.BUS.addListener(this::onStarting);
        ServerStartedEvent.BUS.addListener(this::onStarted);
        ServerStoppedEvent.BUS.addListener(this::onStopped);
        ServerStoppingEvent.BUS.addListener(this::onStopping);
        ServerChatEvent.BUS.addListener(this::onChat);
        PlayerLoggedInEvent.BUS.addListener(this::onJoin);
        PlayerLoggedOutEvent.BUS.addListener(this::onLeave);
        LivingDeathEvent.BUS.addListener(this::onDeath);
        AdvancementEvent.AdvancementEarnEvent.BUS.addListener(this::onAdvancement);
    }

    public void onCommands(RegisterCommandsEvent e) {
        McLinkCommand.register(e.getDispatcher(), () -> running);
    }

    public void onStarting(ServerStartingEvent e) {
        var server = e.getServer();
        // Forge has no dedicated-server-only @Mod, so skip singleplayer/LAN hosts here.
        if (boot == null || !server.isDedicatedServer()) return;
        try {
            var dbDir = boot.dbPerWorld()
                ? server.getWorldPath(LevelResource.ROOT).resolve("dmcl")
                : FMLPaths.GAMEDIR.get().resolve("dmcl");
            running = boot.start(dbDir, avatars -> adapter = new VanillaMinecraftAdapter(server, avatars));
            LOG.info("DMCL ready");
        } catch (Exception ex) {
            LOG.error("DMCL start failed", ex);
        }
    }

    public void onStarted(ServerStartedEvent e) {
        if (adapter != null) adapter.lifecycleHandler().accept(new LifecycleEvent.Started());
    }

    public void onStopped(ServerStoppedEvent e) {
        if (adapter != null) adapter.lifecycleHandler().accept(new LifecycleEvent.Stopped());
    }

    public void onStopping(ServerStoppingEvent e) {
        try {
            if (running != null) running.stop();
        } catch (Exception ex) {
            LOG.warn("DMCL shutdown error", ex);
        }
    }

    public boolean onChat(ServerChatEvent e) {
        if (adapter == null) return false;
        var p = e.getPlayer();
        var author = new Author(Optional.of(p.getUUID()), Optional.empty(), p.getName().getString(),
            running.avatars().headUrlFor(p.getUUID()));
        adapter.chatHandler().accept(new BridgeMessage(
            "mc:" + System.nanoTime(), Source.MINECRAFT, author,
            e.getRawText(), List.of(), Optional.empty(), Scope.GLOBAL, Instant.now(), false));
        return false;
    }

    public void onJoin(PlayerLoggedInEvent e) {
        if (adapter != null && e.getEntity() instanceof ServerPlayer p)
            adapter.playerHandler().accept(new PlayerEvent.Joined(p.getUUID(), p.getName().getString()));
    }

    public void onLeave(PlayerLoggedOutEvent e) {
        if (adapter != null && e.getEntity() instanceof ServerPlayer p)
            adapter.playerHandler().accept(new PlayerEvent.Left(p.getUUID(), p.getName().getString()));
    }

    public boolean onDeath(LivingDeathEvent e) {
        if (adapter != null && e.getEntity() instanceof ServerPlayer p)
            adapter.playerHandler().accept(new PlayerEvent.Died(p.getUUID(), p.getName().getString(),
                e.getSource().getLocalizedDeathMessage(p).getString()));
        return false;
    }

    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent e) {
        var display = e.getAdvancement().value().display();
        if (adapter != null && display.isPresent() && e.getEntity() instanceof ServerPlayer p)
            adapter.playerHandler().accept(new PlayerEvent.Advanced(p.getUUID(), p.getName().getString(),
                McCompat.advancementTitle(display.get()).getString()));
    }
}
