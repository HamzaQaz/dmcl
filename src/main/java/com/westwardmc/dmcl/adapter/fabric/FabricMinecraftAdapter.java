package com.westwardmc.dmcl.adapter.fabric;

import com.westwardmc.dmcl.core.domain.BridgeMessage;
import com.westwardmc.dmcl.core.domain.RenderedMcText;
import com.westwardmc.dmcl.core.domain.Scope;
import com.westwardmc.dmcl.core.port.AvatarService;
import com.westwardmc.dmcl.core.port.LifecycleEvent;
import com.westwardmc.dmcl.core.port.MinecraftPort;
import com.westwardmc.dmcl.core.port.OnlinePlayer;
import com.westwardmc.dmcl.core.port.PlayerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class FabricMinecraftAdapter implements MinecraftPort {
    private final MinecraftServer server;
    private final AvatarService avatars;

    private Consumer<BridgeMessage> chatHandler = m -> {};
    private Consumer<LifecycleEvent> lifecycleHandler = e -> {};
    private Consumer<PlayerEvent> playerHandler = e -> {};

    public FabricMinecraftAdapter(MinecraftServer server, AvatarService avatars) {
        this.server = server;
        this.avatars = avatars;
    }

    public Consumer<BridgeMessage> chatHandler() { return chatHandler; }
    public Consumer<LifecycleEvent> lifecycleHandler() { return lifecycleHandler; }
    public Consumer<PlayerEvent> playerHandler() { return playerHandler; }

    @Override
    public void broadcast(Scope scope, RenderedMcText text) {
        var t = McTextConverter.toText(text);
        server.execute(() -> server.getPlayerManager().broadcast(t, false));
    }

    @Override
    public void sendTo(UUID player, RenderedMcText text) {
        var t = McTextConverter.toText(text);
        server.execute(() -> {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
            if (p != null) p.sendMessage(t, false);
        });
    }

    @Override
    public Set<OnlinePlayer> getOnlinePlayers() {
        var out = new HashSet<OnlinePlayer>();
        for (var p : server.getPlayerManager().getPlayerList()) {
            out.add(new OnlinePlayer(p.getUuid(), p.getName().getString()));
        }
        return out;
    }

    @Override public String headUrlFor(UUID uuid) { return avatars.headUrlFor(uuid); }
    @Override public void onChat(Consumer<BridgeMessage> handler) { this.chatHandler = handler; }
    @Override public void onLifecycle(Consumer<LifecycleEvent> handler) { this.lifecycleHandler = handler; }
    @Override public void onPlayerEvent(Consumer<PlayerEvent> handler) { this.playerHandler = handler; }
}
