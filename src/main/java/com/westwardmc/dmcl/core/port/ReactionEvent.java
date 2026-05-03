package com.westwardmc.dmcl.core.port;

public record ReactionEvent(String bridgedMessageId, long reactorDiscordId, String reactorName, String emoji) {}
