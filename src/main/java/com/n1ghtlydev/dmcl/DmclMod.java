package com.n1ghtlydev.dmcl;

import com.n1ghtlydev.dmcl.adapter.fabric.ChatEventHook;
import com.n1ghtlydev.dmcl.adapter.mc.VanillaMinecraftAdapter;
import com.n1ghtlydev.dmcl.adapter.fabric.LifecycleHook;
import com.n1ghtlydev.dmcl.adapter.mc.McLinkCommand;
import com.n1ghtlydev.dmcl.adapter.fabric.PlayerEventHook;
import com.n1ghtlydev.dmcl.bootstrap.DmclBootstrap;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DmclMod implements DedicatedServerModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("dmcl");

    private static volatile VanillaMinecraftAdapter mcAdapter;

    public static VanillaMinecraftAdapter mcAdapter() { return mcAdapter; }

    private DmclBootstrap boot;
    private volatile DmclBootstrap.Running running;

    @Override
    public void onInitializeServer() {
        try {
            boot = DmclBootstrap.load(FabricLoader.getInstance().getConfigDir(), LOG);
            if (boot == null) return;
            // Registered up front: the command tree is built before SERVER_STARTING fires.
            CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
                McLinkCommand.register(dispatcher, () -> running));
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
            running = boot.start(dbDir, avatars -> mcAdapter = new VanillaMinecraftAdapter(server, avatars));
            ChatEventHook.register(mcAdapter, running.avatars());
            LifecycleHook.register(mcAdapter);
            PlayerEventHook.register(mcAdapter);
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
