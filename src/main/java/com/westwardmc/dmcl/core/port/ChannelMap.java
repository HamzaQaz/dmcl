package com.westwardmc.dmcl.core.port;

import com.westwardmc.dmcl.core.domain.Scope;

import java.util.List;
import java.util.Optional;

public interface ChannelMap {
    Optional<ChannelBinding> forScope(Scope scope);
    Optional<ChannelBinding> forChannelId(long channelId);
    List<ChannelBinding> all();
}
