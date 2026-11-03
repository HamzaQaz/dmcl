package com.n1ghtlydev.dmcl.paper;

import com.n1ghtlydev.dmcl.bootstrap.DmclBootstrap;
import com.n1ghtlydev.dmcl.core.domain.Author;
import com.n1ghtlydev.dmcl.core.domain.BridgeMessage;
import com.n1ghtlydev.dmcl.core.domain.Scope;
import com.n1ghtlydev.dmcl.core.domain.Source;
import com.n1ghtlydev.dmcl.core.port.LifecycleEvent;
import com.n1ghtlydev.dmcl.core.port.PlayerEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class DmclPlugin extends JavaPlugin implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private DmclBootstrap.Running running;
    private PaperMinecraftAdapter adapter;

    @Override
    public void onEnable() {
        try {
            var boot = DmclBootstrap.load(getDataFolder().toPath(), getSLF4JLogger());
            if (boot == null) return;
            var dbDir = boot.dbPerWorld()
                ? Bukkit.getWorlds().get(0).getWorldFolder().toPath().resolve("dmcl")
                : getDataFolder().toPath().resolve("data");
            running = boot.start(dbDir, avatars -> adapter = new PaperMinecraftAdapter(this, avatars));
            Bukkit.getPluginManager().registerEvents(this, this);
            getSLF4JLogger().info("DMCL ready");
        } catch (Exception e) {
            getSLF4JLogger().error("DMCL start failed", e);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (adapter != null) adapter.lifecycleHandler().accept(new LifecycleEvent.Stopped());
            if (running != null) running.stop();
        } catch (Exception e) {
            getSLF4JLogger().warn("DMCL shutdown error", e);
        }
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent e) {
        if (adapter != null && e.getType() == ServerLoadEvent.LoadType.STARTUP)
            adapter.lifecycleHandler().accept(new LifecycleEvent.Started());
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        if (adapter == null) return;
        var p = e.getPlayer();
        var author = new Author(Optional.of(p.getUniqueId()), Optional.empty(), p.getName(),
            running.avatars().headUrlFor(p.getUniqueId()));
        adapter.chatHandler().accept(new BridgeMessage(
            "mc:" + System.nanoTime(), Source.MINECRAFT, author,
            PLAIN.serialize(e.message()), List.of(), Optional.empty(),
            Scope.GLOBAL, Instant.now(), false));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (adapter != null) adapter.playerHandler().accept(
            new PlayerEvent.Joined(e.getPlayer().getUniqueId(), e.getPlayer().getName()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (adapter != null) adapter.playerHandler().accept(
            new PlayerEvent.Left(e.getPlayer().getUniqueId(), e.getPlayer().getName()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (adapter == null) return;
        Component msg = e.deathMessage();
        adapter.playerHandler().accept(new PlayerEvent.Died(e.getPlayer().getUniqueId(),
            e.getPlayer().getName(), msg == null ? "" : PLAIN.serialize(msg)));
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        var display = e.getAdvancement().getDisplay();
        if (adapter == null || display == null) return;
        adapter.playerHandler().accept(new PlayerEvent.Advanced(e.getPlayer().getUniqueId(),
            e.getPlayer().getName(), PLAIN.serialize(display.title())));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (running == null) {
            sender.sendMessage("DMCL is not running; check the server log.");
            return true;
        }
        switch (cmd.getName().toLowerCase()) {
            case "link" -> {
                if (sender instanceof Player p) running.orchestrator().startLink(p.getUniqueId());
                else sender.sendMessage("Players only");
            }
            case "unlink" -> {
                if (sender instanceof Player p) {
                    running.links().unlinkByMc(p.getUniqueId());
                    sender.sendMessage("Unlinked.");
                }
            }
            default -> sender.sendMessage("DMCL: see logs for details");
        }
        return true;
    }
}
