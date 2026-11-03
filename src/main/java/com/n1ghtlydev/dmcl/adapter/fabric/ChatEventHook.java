package com.n1ghtlydev.dmcl.adapter.fabric;

import com.n1ghtlydev.dmcl.core.domain.Author;
import com.n1ghtlydev.dmcl.core.domain.BridgeMessage;
import com.n1ghtlydev.dmcl.core.domain.Scope;
import com.n1ghtlydev.dmcl.core.domain.Source;
import com.n1ghtlydev.dmcl.core.port.AvatarService;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class ChatEventHook {
    private ChatEventHook() {}

    public static void register(FabricMinecraftAdapter adapter, AvatarService avatars) {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            var author = new Author(
                Optional.of(sender.getUUID()),
                Optional.empty(),
                sender.getName().getString(),
                avatars.headUrlFor(sender.getUUID()));
            var bm = new BridgeMessage(
                "mc:" + System.nanoTime(),
                Source.MINECRAFT,
                author,
                message.decoratedContent().getString(),
                List.of(),
                Optional.empty(),
                Scope.GLOBAL,
                Instant.now(),
                false);
            adapter.chatHandler().accept(bm);
        });
    }
}
