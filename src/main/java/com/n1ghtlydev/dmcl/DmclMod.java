package com.n1ghtlydev.dmcl;

import com.n1ghtlydev.dmcl.adapter.fabric.ChatEventHook;
import com.n1ghtlydev.dmcl.adapter.fabric.FabricMinecraftAdapter;
import com.n1ghtlydev.dmcl.adapter.fabric.LifecycleHook;
import com.n1ghtlydev.dmcl.adapter.fabric.McLinkCommand;
import com.n1ghtlydev.dmcl.adapter.fabric.PlayerEventHook;
import com.n1ghtlydev.dmcl.bootstrap.DmclBootstrap;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DmclMod implements DedicatedServerModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("dmcl");

    private static volatile FabricMinecraftAdapter fabricAdapter;

    public static FabricMinecraftAdapter fabricAdapter() { return fabricAdapter; }

    private DmclBootstrap boot;
    private DmclBootstrap.Running running;

    @Override
    public void onInitializeServer() {
        try {
            boot = DmclBootstrap.load(FabricLoader.getInstance().getConfigDir(), LOG);
            if (boot == null) return;
            ServerLifecycleEvents.SERVER_STARTING.register(this::start);
            ServerLifecycleEvents.SERVER_STOPPING.register(this::stop);
        } catch (Exception e) {
            LOG.error("DMCL disabled: unexpected init error. Server will continue without the bridge.", e);
        }
    }

    private void start(MinecraftServer server) {
        try {
            var dbDir = boot.dbPerWorld()
                ? server.getWorldPath(LevelResource.ROOT).resolve("dmcl")
                : FabricLoader.getInstance().getGameDir().resolve("dmcl");
            running = boot.start(dbDir, avatars -> fabricAdapter = new FabricMinecraftAdapter(server, avatars));
            ChatEventHook.register(fabricAdapter, running.avatars());
            LifecycleHook.register(fabricAdapter);
            PlayerEventHook.register(fabricAdapter);
            McLinkCommand.register(running.orchestrator(), running.links());
            LOG.info("DMCL ready");
        } catch (Exception e) {
            LOG.error("DMCL start failed", e);
        }
    }

    private void stop(MinecraftServer server) {
        try {
            if (running != null) running.stop();
        } catch (Exception e) {
            LOG.warn("DMCL shutdown error", e);
        }
    }
}
