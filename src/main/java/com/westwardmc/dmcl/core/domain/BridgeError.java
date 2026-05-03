package com.westwardmc.dmcl.core.domain;

import java.time.Duration;

public sealed interface BridgeError {
    record NetworkError(String message) implements BridgeError {}
    record RateLimited(Duration retryAfter) implements BridgeError {}
    record NotFound() implements BridgeError {}
    record BadInput(String reason) implements BridgeError {}
    record Unauthorized() implements BridgeError {}
}
