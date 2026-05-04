package com.westwardmc.dmcl.adapter.fabric;

import com.westwardmc.dmcl.core.port.PlayerEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PlayerEventHook {
    private PlayerEventHook() {}

    public static void register(FabricMinecraftAdapter adapter) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var p = handler.player;
            adapter.playerHandler().accept(new PlayerEvent.Joined(p.getUuid(), p.getName().getString()));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var p = handler.player;
            adapter.playerHandler().accept(new PlayerEvent.Left(p.getUuid(), p.getName().getString()));
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, src) -> {
            if (entity instanceof ServerPlayerEntity p) {
                String msg = src.getDeathMessage(p).getString();
                adapter.playerHandler().accept(new PlayerEvent.Died(p.getUuid(), p.getName().getString(), msg));
            }
        });
    }
}
