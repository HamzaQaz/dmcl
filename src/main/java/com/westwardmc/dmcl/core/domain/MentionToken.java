package com.westwardmc.dmcl.core.domain;

public sealed interface MentionToken {
    record User(long discordId) implements MentionToken {}
    record Role(long roleId, String name, int color) implements MentionToken {}
    record Channel(long channelId, String name) implements MentionToken {}
    record Everyone() implements MentionToken {}
    record Here() implements MentionToken {}
}
