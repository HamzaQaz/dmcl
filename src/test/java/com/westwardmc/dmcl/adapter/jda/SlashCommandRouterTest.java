package com.westwardmc.dmcl.adapter.jda;

import com.westwardmc.dmcl.core.domain.LinkedAccount;
import com.westwardmc.dmcl.core.orchestrator.BridgeOrchestrator;
import com.westwardmc.dmcl.core.port.LinkRepo;
import com.westwardmc.dmcl.core.port.MinecraftPort;
import com.westwardmc.dmcl.core.port.OnlinePlayer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class SlashCommandRouterTest {
    @Test
    void linkInvokesOrchestratorCompleteLink() {
        var orch = mock(BridgeOrchestrator.class);
        var links = mock(LinkRepo.class);
        var mc = mock(MinecraftPort.class);
        var router = new SlashCommandRouter(orch, links, mc);

        var ev = mock(SlashCommandInteractionEvent.class);
        var opt = mock(OptionMapping.class);
        var user = mock(User.class);
        when(ev.getName()).thenReturn("link");
        when(ev.getOption("code")).thenReturn(opt);
        when(opt.getAsString()).thenReturn("ABC123");
        when(ev.getUser()).thenReturn(user);
        when(user.getIdLong()).thenReturn(99L);
        var reply = mock(ReplyCallbackAction.class);
        when(ev.reply(anyString())).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);

        router.onSlashCommandInteraction(ev);
        verify(orch).completeLink(99L, "ABC123");
    }

    @Test
    void playersListsOnlineWithLinkedFlag() {
        var orch = mock(BridgeOrchestrator.class);
        var links = mock(LinkRepo.class);
        var mc = mock(MinecraftPort.class);
        var uuid = UUID.randomUUID();
        when(mc.getOnlinePlayers()).thenReturn(Set.of(new OnlinePlayer(uuid, "Steve")));
        when(links.byMcUuid(uuid)).thenReturn(Optional.of(new LinkedAccount(uuid, 1L, Instant.EPOCH)));

        var router = new SlashCommandRouter(orch, links, mc);
        var ev = mock(SlashCommandInteractionEvent.class);
        when(ev.getName()).thenReturn("players");
        var reply = mock(ReplyCallbackAction.class);
        when(ev.reply(anyString())).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);

        router.onSlashCommandInteraction(ev);
        verify(ev).reply(contains("Steve"));
    }
}
