package com.n1ghtlydev.dmcl.core.port;

import com.n1ghtlydev.dmcl.core.domain.Scope;

import java.util.Optional;

public record ChannelBinding(
    Scope scope,
    long channelId,
    String mcFormat,
    String discordFormat,
    Optional<String> mcPermission,
    Optional<String> mcCommand,
    boolean mcSend
) {}
