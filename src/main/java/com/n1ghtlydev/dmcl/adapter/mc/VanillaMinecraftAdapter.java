package com.n1ghtlydev.dmcl.adapter.mc;

import com.n1ghtlydev.dmcl.core.domain.BridgeMessage;
import com.n1ghtlydev.dmcl.core.domain.RenderedMcText;
import com.n1ghtlydev.dmcl.core.domain.Scope;
import com.n1ghtlydev.dmcl.core.port.AvatarService;
import com.n1ghtlydev.dmcl.core.port.LifecycleEvent;
import com.n1ghtlydev.dmcl.core.port.MinecraftPort;
import com.n1ghtlydev.dmcl.core.port.OnlinePlayer;
import com.n1ghtlydev.dmcl.core.port.PlayerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class VanillaMinecraftAdapter implements MinecraftPort {
    private final MinecraftServer server;
    private final AvatarService avatars;

    private Consumer<BridgeMessage> chatHandler = m -> {};
    private Consumer<LifecycleEvent> lifecycleHandler = e -> {};
    private Consumer<PlayerEvent> playerHandler = e -> {};

    public VanillaMinecraftAdapter(MinecraftServer server, AvatarService avatars) {
        this.server = server;
        this.avatars = avatars;
    }

    public Consumer<BridgeMessage> chatHandler() { return chatHandler; }
    public Consumer<LifecycleEvent> lifecycleHandler() { return lifecycleHandler; }
    public Consumer<PlayerEvent> playerHandler() { return playerHandler; }

    @Override
    public void broadcast(Scope scope, RenderedMcText text) {
        var t = McTextConverter.toComponent(text);
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(t, false));
    }

    @Override
    public void sendTo(UUID player, RenderedMcText text) {
        var t = McTextConverter.toComponent(text);
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(player);
            if (p != null) p.sendSystemMessage(t);
        });
    }

    @Override
    public Set<OnlinePlayer> getOnlinePlayers() {
        var out = new HashSet<OnlinePlayer>();
        for (var p : server.getPlayerList().getPlayers()) {
            out.add(new OnlinePlayer(p.getUUID(), p.getName().getString()));
        }
        return out;
    }

    @Override public String headUrlFor(UUID uuid) { return avatars.headUrlFor(uuid); }
    @Override public void onChat(Consumer<BridgeMessage> handler) { this.chatHandler = handler; }
    @Override public void onLifecycle(Consumer<LifecycleEvent> handler) { this.lifecycleHandler = handler; }
    @Override public void onPlayerEvent(Consumer<PlayerEvent> handler) { this.playerHandler = handler; }
}
