package com.westwardmc.dmcl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

final class SmokeTest {
    @Test
    void junitIsWired() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
