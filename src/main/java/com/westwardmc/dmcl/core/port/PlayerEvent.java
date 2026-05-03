package com.westwardmc.dmcl.core.port;

import java.util.UUID;

public sealed interface PlayerEvent {
    record Joined(UUID uuid, String name) implements PlayerEvent {}
    record Left(UUID uuid, String name) implements PlayerEvent {}
    record Died(UUID uuid, String name, String deathMessage) implements PlayerEvent {}
    record Advanced(UUID uuid, String name, String advancementTitle) implements PlayerEvent {}
}
