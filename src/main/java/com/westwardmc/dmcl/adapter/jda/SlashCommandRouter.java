package com.westwardmc.dmcl.adapter.jda;

import com.westwardmc.dmcl.core.orchestrator.BridgeOrchestrator;
import com.westwardmc.dmcl.core.port.LinkRepo;
import com.westwardmc.dmcl.core.port.MinecraftPort;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public final class SlashCommandRouter extends ListenerAdapter {
    private final BridgeOrchestrator orch;
    private final LinkRepo links;
    private final MinecraftPort minecraft;

    public SlashCommandRouter(BridgeOrchestrator orch, LinkRepo links, MinecraftPort mc) {
        this.orch = orch;
        this.links = links;
        this.minecraft = mc;
    }

    public void register(JDA jda) {
        jda.updateCommands().addCommands(
            Commands.slash("link", "Link your Minecraft account to Discord")
                .addOption(OptionType.STRING, "code", "6-char code from MC /link", true),
            Commands.slash("unlink", "Unlink your Minecraft account"),
            Commands.slash("players", "List online MC players")
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent ev) {
        switch (ev.getName()) {
            case "link" -> handleLink(ev);
            case "unlink" -> handleUnlink(ev);
            case "players" -> handlePlayers(ev);
            default -> {}
        }
    }

    private void handleLink(SlashCommandInteractionEvent ev) {
        String code = ev.getOption("code").getAsString().toUpperCase();
        orch.completeLink(ev.getUser().getIdLong(), code);
        ev.reply("Attempting link with code " + code).setEphemeral(true).queue();
    }

    private void handleUnlink(SlashCommandInteractionEvent ev) {
        links.unlinkByDiscord(ev.getUser().getIdLong());
        ev.reply("Unlinked.").setEphemeral(true).queue();
    }

    private void handlePlayers(SlashCommandInteractionEvent ev) {
        var sb = new StringBuilder("Online players:\n");
        for (var p : minecraft.getOnlinePlayers()) {
            boolean linked = links.byMcUuid(p.uuid()).isPresent();
            sb.append("- ").append(p.name()).append(linked ? " (linked)" : "").append("\n");
        }
        ev.reply(sb.toString()).setEphemeral(true).queue();
    }
}
