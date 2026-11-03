package com.n1ghtlydev.dmcl.adapter.fabric;

import com.n1ghtlydev.dmcl.adapter.mc.VanillaMinecraftAdapter;
import com.n1ghtlydev.dmcl.core.port.PlayerEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerEventHook {
    private PlayerEventHook() {}

    public static void register(VanillaMinecraftAdapter adapter) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var p = handler.player;
            adapter.playerHandler().accept(new PlayerEvent.Joined(p.getUUID(), p.getName().getString()));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var p = handler.player;
            adapter.playerHandler().accept(new PlayerEvent.Left(p.getUUID(), p.getName().getString()));
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, src) -> {
            if (entity instanceof ServerPlayer p) {
                String msg = src.getLocalizedDeathMessage(p).getString();
                adapter.playerHandler().accept(new PlayerEvent.Died(p.getUUID(), p.getName().getString(), msg));
            }
        });
    }
}
