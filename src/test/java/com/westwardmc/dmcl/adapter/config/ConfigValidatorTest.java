package com.westwardmc.dmcl.adapter.config;

import com.westwardmc.dmcl.core.domain.Scope;
import com.westwardmc.dmcl.core.port.ChannelBinding;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

final class ConfigValidatorTest {
    private DmclConfig sample(String token, List<ChannelBinding> channels) {
        return new DmclConfig(token, 1, "", new DmclConfig.Storage("world"),
            new DmclConfig.Avatars("mc-heads", 64),
            new DmclConfig.Behavior(true, true, false, "aqua", "minecraft:block.note_block.bell", true),
            channels, new DmclConfig.Mentions(false, false, List.of()));
    }

    @Test
    void validConfigPasses() {
        var ch = new ChannelBinding(Scope.GLOBAL, 100L, "<{name}> {message}", "{message}",
            Optional.empty(), Optional.empty(), true);
        assertThat(ConfigValidator.validate(sample("tok", List.of(ch)))).isEmpty();
    }

    @Test
    void emptyTokenIsError() {
        var errors = ConfigValidator.validate(sample("", List.of()));
        assertThat(errors).anyMatch(e -> e.contains("token"));
    }

    @Test
    void noChannelsIsError() {
        var errors = ConfigValidator.validate(sample("tok", List.of()));
        assertThat(errors).anyMatch(e -> e.contains("channels"));
    }

    @Test
    void duplicateScopeIsError() {
        var ch = new ChannelBinding(Scope.GLOBAL, 100L, "x", "y", Optional.empty(), Optional.empty(), true);
        var errors = ConfigValidator.validate(sample("tok", List.of(ch, ch)));
        assertThat(errors).anyMatch(e -> e.contains("duplicate"));
    }
}
