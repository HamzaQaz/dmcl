package com.n1ghtlydev.dmcl.adapter.avatar;

import com.n1ghtlydev.dmcl.core.port.AvatarService;

import java.util.UUID;

public final class McHeadsAvatarService implements AvatarService {
    private final int size;
    public McHeadsAvatarService(int size) { this.size = size; }
    @Override public String headUrlFor(UUID uuid) {
        return "https://mc-heads.net/avatar/" + uuid + "/" + size;
    }
}
