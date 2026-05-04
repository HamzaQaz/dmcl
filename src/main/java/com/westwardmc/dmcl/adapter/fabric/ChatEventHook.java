package com.westwardmc.dmcl.adapter.fabric;

import com.westwardmc.dmcl.core.domain.Author;
import com.westwardmc.dmcl.core.domain.BridgeMessage;
import com.westwardmc.dmcl.core.domain.Scope;
import com.westwardmc.dmcl.core.domain.Source;
import com.westwardmc.dmcl.core.port.AvatarService;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class ChatEventHook {
    private ChatEventHook() {}

    public static void register(FabricMinecraftAdapter adapter, AvatarService avatars) {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            var author = new Author(
                Optional.of(sender.getUuid()),
                Optional.empty(),
                sender.getName().getString(),
                avatars.headUrlFor(sender.getUuid()));
            var bm = new BridgeMessage(
                "mc:" + System.nanoTime(),
                Source.MINECRAFT,
                author,
                message.getContent().getString(),
                List.of(),
                Optional.empty(),
                Scope.GLOBAL,
                Instant.now(),
                false);
            adapter.chatHandler().accept(bm);
        });
    }
}
