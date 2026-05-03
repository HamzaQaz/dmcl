package com.westwardmc.dmcl.core.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

final class ResultTest {
    @Test
    void okWrapsValueAndIsOk() {
        Result<Integer, BridgeError> r = Result.ok(42);
        assertThat(r.isOk()).isTrue();
        assertThat(r.unwrap()).isEqualTo(42);
    }

    @Test
    void errWrapsErrorAndIsErr() {
        Result<Integer, BridgeError> r = Result.err(new BridgeError.NotFound());
        assertThat(r.isOk()).isFalse();
        assertThat(r.unwrapErr()).isInstanceOf(BridgeError.NotFound.class);
    }

    @Test
    void mapTransformsOkValue() {
        Result<Integer, BridgeError> r = Result.<Integer, BridgeError>ok(5).map(i -> i * 2);
        assertThat(r.unwrap()).isEqualTo(10);
    }

    @Test
    void mapPreservesErr() {
        var err = new BridgeError.NetworkError("boom");
        Result<Integer, BridgeError> r = Result.<Integer, BridgeError>err(err).map(i -> i * 2);
        assertThat(r.unwrapErr()).isSameAs(err);
    }
}
