package com.n1ghtlydev.dmcl.neoforge;

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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mod(value = "dmcl", dist = Dist.DEDICATED_SERVER)
public final class DmclNeoForge {
    private static final Logger LOG = LoggerFactory.getLogger("dmcl");

    private DmclBootstrap boot;
    private volatile DmclBootstrap.Running running;
    private volatile VanillaMinecraftAdapter adapter;

    public DmclNeoForge() {
        try {
            boot = DmclBootstrap.load(FMLPaths.CONFIGDIR.get(), LOG);
        } catch (Exception e) {
            LOG.error("DMCL disabled: unexpected init error. Server will continue without the bridge.", e);
        }
        // Commands are registered whether or not the bridge is enabled; they report "not running".
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent e) {
        McLinkCommand.register(e.getDispatcher(), () -> running);
    }

    @SubscribeEvent
    public void onStarting(ServerStartingEvent e) {
        if (boot == null) return;
        var server = e.getServer();
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

    @SubscribeEvent
    public void onStarted(ServerStartedEvent e) {
        if (adapter != null) adapter.lifecycleHandler().accept(new LifecycleEvent.Started());
    }

    @SubscribeEvent
    public void onStopped(ServerStoppedEvent e) {
        if (adapter != null) adapter.lifecycleHandler().accept(new LifecycleEvent.Stopped());
    }

    @SubscribeEvent
    public void onStopping(ServerStoppingEvent e) {
        try {
            if (running != null) running.stop();
        } catch (Exception ex) {
            LOG.warn("DMCL shutdown error", ex);
        }
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent e) {
        if (adapter == null) return;
        var p = e.getPlayer();
        var author = new Author(Optional.of(p.getUUID()), Optional.empty(), p.getName().getString(),
            running.avatars().headUrlFor(p.getUUID()));
        adapter.chatHandler().accept(new BridgeMessage(
            "mc:" + System.nanoTime(), Source.MINECRAFT, author,
            e.getRawText(), List.of(), Optional.empty(), Scope.GLOBAL, Instant.now(), false));
    }

    @SubscribeEvent
    public void onJoin(PlayerLoggedInEvent e) {
        if (adapter != null && e.getEntity() instanceof ServerPlayer p)
            adapter.playerHandler().accept(new PlayerEvent.Joined(p.getUUID(), p.getName().getString()));
    }

    @SubscribeEvent
    public void onLeave(PlayerLoggedOutEvent e) {
        if (adapter != null && e.getEntity() instanceof ServerPlayer p)
            adapter.playerHandler().accept(new PlayerEvent.Left(p.getUUID(), p.getName().getString()));
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent e) {
        if (adapter != null && e.getEntity() instanceof ServerPlayer p)
            adapter.playerHandler().accept(new PlayerEvent.Died(p.getUUID(), p.getName().getString(),
                e.getSource().getLocalizedDeathMessage(p).getString()));
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent e) {
        var display = e.getAdvancement().value().display();
        if (adapter != null && display.isPresent() && e.getEntity() instanceof ServerPlayer p)
            adapter.playerHandler().accept(new PlayerEvent.Advanced(p.getUUID(), p.getName().getString(),
                McCompat.advancementTitle(display.get()).getString()));
    }
}
