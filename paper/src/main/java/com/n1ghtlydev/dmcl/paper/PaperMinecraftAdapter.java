package com.n1ghtlydev.dmcl.paper;

import com.n1ghtlydev.dmcl.core.domain.BridgeMessage;
import com.n1ghtlydev.dmcl.core.domain.RenderedMcText;
import com.n1ghtlydev.dmcl.core.domain.Scope;
import com.n1ghtlydev.dmcl.core.port.AvatarService;
import com.n1ghtlydev.dmcl.core.port.LifecycleEvent;
import com.n1ghtlydev.dmcl.core.port.MinecraftPort;
import com.n1ghtlydev.dmcl.core.port.OnlinePlayer;
import com.n1ghtlydev.dmcl.core.port.PlayerEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

final class PaperMinecraftAdapter implements MinecraftPort {
    private final Plugin plugin;
    private final AvatarService avatars;

    private volatile Consumer<BridgeMessage> chatHandler = m -> {};
    private volatile Consumer<LifecycleEvent> lifecycleHandler = e -> {};
    private volatile Consumer<PlayerEvent> playerHandler = e -> {};

    PaperMinecraftAdapter(Plugin plugin, AvatarService avatars) {
        this.plugin = plugin;
        this.avatars = avatars;
    }

    Consumer<BridgeMessage> chatHandler() { return chatHandler; }
    Consumer<LifecycleEvent> lifecycleHandler() { return lifecycleHandler; }
    Consumer<PlayerEvent> playerHandler() { return playerHandler; }

    @Override
    public void broadcast(Scope scope, RenderedMcText text) {
        var c = PaperTextConverter.toComponent(text);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getServer().sendMessage(c));
    }

    @Override
    public void sendTo(UUID player, RenderedMcText text) {
        var c = PaperTextConverter.toComponent(text);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(player);
            if (p != null) p.sendMessage(c);
        });
    }

    @Override
    public Set<OnlinePlayer> getOnlinePlayers() {
        var out = new HashSet<OnlinePlayer>();
        for (Player p : Bukkit.getOnlinePlayers()) out.add(new OnlinePlayer(p.getUniqueId(), p.getName()));
        return out;
    }

    @Override public String headUrlFor(UUID uuid) { return avatars.headUrlFor(uuid); }
    @Override public void onChat(Consumer<BridgeMessage> handler) { this.chatHandler = handler; }
    @Override public void onLifecycle(Consumer<LifecycleEvent> handler) { this.lifecycleHandler = handler; }
    @Override public void onPlayerEvent(Consumer<PlayerEvent> handler) { this.playerHandler = handler; }
}
