package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.port.LifecycleEvent;
import com.westwardmc.dmcl.core.port.PlayerEvent;

public final class SystemEventRenderer {
    private SystemEventRenderer() {}

    public record Card(String title, String description, int color, java.util.Optional<java.util.UUID> headUuid) {}

    public static Card render(PlayerEvent ev) {
        return switch (ev) {
            case PlayerEvent.Joined j     -> new Card(j.name() + " joined the game", "", 0x2ECC71, java.util.Optional.of(j.uuid()));
            case PlayerEvent.Left l       -> new Card(l.name() + " left the game", "", 0x95A5A6, java.util.Optional.of(l.uuid()));
            case PlayerEvent.Died d       -> new Card(d.deathMessage(), "", 0xE74C3C, java.util.Optional.of(d.uuid()));
            case PlayerEvent.Advanced a   -> new Card(a.name() + " got the advancement [" + a.advancementTitle() + "]", "", 0x9B59B6, java.util.Optional.of(a.uuid()));
        };
    }

    public static Card render(LifecycleEvent ev) {
        return switch (ev) {
            case LifecycleEvent.Started s  -> new Card("🟢 Server started", "", 0x2ECC71, java.util.Optional.empty());
            case LifecycleEvent.Stopped s  -> new Card("🔴 Server stopped", "", 0x7F8C8D, java.util.Optional.empty());
            case LifecycleEvent.Crashed c  -> new Card("💥 Server crashed (exit code " + c.exitCode() + ")", c.reason(), 0xC0392B, java.util.Optional.empty());
        };
    }
}
