package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.port.LifecycleEvent;
import com.westwardmc.dmcl.core.port.PlayerEvent;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

final class SystemEventRendererTest {
    @Test
    void joinHasGreenBorder() {
        var card = SystemEventRenderer.render(new PlayerEvent.Joined(UUID.randomUUID(), "Steve"));
        assertThat(card.title()).contains("Steve").contains("joined");
        assertThat(card.color()).isEqualTo(0x2ECC71);
    }

    @Test
    void deathRedBorderIncludesDeathMessage() {
        var card = SystemEventRenderer.render(
            new PlayerEvent.Died(UUID.randomUUID(), "Steve", "Steve was blown up by Creeper"));
        assertThat(card.title()).contains("blown up by Creeper");
        assertThat(card.color()).isEqualTo(0xE74C3C);
    }

    @Test
    void lifecycleStarted() {
        var card = SystemEventRenderer.render(new LifecycleEvent.Started());
        assertThat(card.title()).contains("started");
        assertThat(card.color()).isEqualTo(0x2ECC71);
    }

    @Test
    void lifecycleCrashedShowsExitCode() {
        var card = SystemEventRenderer.render(new LifecycleEvent.Crashed(137, "OOM"));
        assertThat(card.title()).contains("crashed").contains("137");
    }
}
