package com.westwardmc.dmcl.core.port;

public record PostedRef(long channelId, long messageId, String webhookId, String webhookToken) {}
