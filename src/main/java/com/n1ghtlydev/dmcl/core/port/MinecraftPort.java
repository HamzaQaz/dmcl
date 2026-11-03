package com.n1ghtlydev.dmcl.core.port;

import com.n1ghtlydev.dmcl.core.domain.BridgeMessage;
import com.n1ghtlydev.dmcl.core.domain.RenderedMcText;
import com.n1ghtlydev.dmcl.core.domain.Scope;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface MinecraftPort {
    void broadcast(Scope scope, RenderedMcText text);
    void sendTo(UUID player, RenderedMcText text);
    Set<OnlinePlayer> getOnlinePlayers();
    String headUrlFor(UUID uuid);

    void onChat(Consumer<BridgeMessage> handler);
    void onLifecycle(Consumer<LifecycleEvent> handler);
    void onPlayerEvent(Consumer<PlayerEvent> handler);
}
