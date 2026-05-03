package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.domain.LinkedAccount;
import com.westwardmc.dmcl.core.port.OnlinePlayer;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

final class MentionResolverTest {
    private static final UUID STEVE = UUID.randomUUID();
    private static final UUID ALEX = UUID.randomUUID();

    private MentionResolver resolver(List<LinkedAccount> linked, Set<OnlinePlayer> online) {
        return new MentionResolver(linked, online);
    }

    @Test
    void linkedOnlineNameBecomesDiscordMention() {
        var linked = List.of(new LinkedAccount(STEVE, 1234L, Instant.EPOCH));
        var online = Set.of(new OnlinePlayer(STEVE, "Steve"));
        assertThat(resolver(linked, online).resolveOutbound("hi @Steve"))
            .isEqualTo("hi <@1234>");
    }

    @Test
    void unlinkedOnlineNameBecomesBoldName() {
        var online = Set.of(new OnlinePlayer(ALEX, "Alex"));
        assertThat(resolver(List.of(), online).resolveOutbound("hi @Alex"))
            .isEqualTo("hi **Alex**");
    }

    @Test
    void unknownNameLeftAsIs() {
        assertThat(resolver(List.of(), Set.of()).resolveOutbound("hi @Ghost"))
            .isEqualTo("hi @Ghost");
    }

    @Test
    void caseInsensitiveMatch() {
        var online = Set.of(new OnlinePlayer(ALEX, "Alex"));
        assertThat(resolver(List.of(), online).resolveOutbound("hi @aLeX"))
            .isEqualTo("hi **Alex**");
    }

    @Test
    void multipleMentionsResolveIndependently() {
        var linked = List.of(new LinkedAccount(STEVE, 1234L, Instant.EPOCH));
        var online = Set.of(new OnlinePlayer(STEVE, "Steve"), new OnlinePlayer(ALEX, "Alex"));
        assertThat(resolver(linked, online).resolveOutbound("@Steve and @Alex"))
            .isEqualTo("<@1234> and **Alex**");
    }
}
