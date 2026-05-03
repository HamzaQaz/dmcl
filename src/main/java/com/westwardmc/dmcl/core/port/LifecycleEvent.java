package com.westwardmc.dmcl.core.port;

public sealed interface LifecycleEvent {
    record Started() implements LifecycleEvent {}
    record Stopped() implements LifecycleEvent {}
    record Crashed(int exitCode, String reason) implements LifecycleEvent {}
}
