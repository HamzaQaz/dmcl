package com.westwardmc.dmcl.core.domain;

import java.time.Instant;
import java.util.UUID;

public record LinkedAccount(UUID mcUuid, long discordId, Instant linkedAt) {}
