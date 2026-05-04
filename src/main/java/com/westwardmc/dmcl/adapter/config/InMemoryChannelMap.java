package com.westwardmc.dmcl.adapter.config;

import com.westwardmc.dmcl.core.domain.Scope;
import com.westwardmc.dmcl.core.port.ChannelBinding;
import com.westwardmc.dmcl.core.port.ChannelMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryChannelMap implements ChannelMap {
    private final Map<Scope, ChannelBinding> byScope = new HashMap<>();
    private final Map<Long, ChannelBinding> byChannel = new HashMap<>();
    private final List<ChannelBinding> all;

    public InMemoryChannelMap(List<ChannelBinding> bindings) {
        this.all = List.copyOf(bindings);
        for (var b : bindings) {
            byScope.put(b.scope(), b);
            byChannel.put(b.channelId(), b);
        }
    }

    @Override public Optional<ChannelBinding> forScope(Scope s) { return Optional.ofNullable(byScope.get(s)); }
    @Override public Optional<ChannelBinding> forChannelId(long id) { return Optional.ofNullable(byChannel.get(id)); }
    @Override public List<ChannelBinding> all() { return all; }
}
