package com.westwardmc.dmcl.adapter.config;

import com.westwardmc.dmcl.core.port.ChannelBinding;

import java.util.List;

public record DmclConfig(
    String discordToken,
    long guildId,
    String status,
    Storage storage,
    Avatars avatars,
    Behavior behavior,
    List<ChannelBinding> channels,
    Mentions mentions
) {
    public record Storage(String dbPath) {}
    public record Avatars(String provider, int size) {}
    public record Behavior(boolean showEdits, boolean showDeletes, boolean showReactions,
                           String mcMentionColor, String pingSound, boolean loopGuardStrict) {}
    public record Mentions(boolean allowEveryone, boolean allowHere, List<String> allowRole) {}
}
