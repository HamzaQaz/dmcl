package com.n1ghtlydev.dmcl.core.port;

public record PostedRef(long channelId, long messageId, String webhookId, String webhookToken) {}
