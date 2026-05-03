package com.westwardmc.dmcl.core.port;

public sealed interface SystemEvent {
    record Lifecycle(LifecycleEvent ev) implements SystemEvent {}
    record Player(PlayerEvent ev) implements SystemEvent {}
}
