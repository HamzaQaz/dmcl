package com.westwardmc.dmcl.adapter.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

final class SecretResolverTest {
    @Test
    void plainStringPassesThroughUnchanged() {
        var r = new SecretResolver(Map.of(), null, null);
        assertThat(r.resolve("hello")).isEqualTo("hello");
    }

    @Test
    void envPrefixResolvesFromMapFirst() {
        var r = new SecretResolver(Map.of("FOO", "from-env"), null, null);
        assertThat(r.resolve("env:FOO")).isEqualTo("from-env");
    }

    @Test
    void envPrefixFallsBackToEnvFile(@TempDir Path tmp) throws IOException {
        var envFile = tmp.resolve("dmcl.env");
        Files.writeString(envFile, "FOO=from-file\nBAR=other\n");
        var r = new SecretResolver(Map.of(), envFile, null);
        assertThat(r.resolve("env:FOO")).isEqualTo("from-file");
    }

    @Test
    void envPrefixFallsBackToSecretsToml(@TempDir Path tmp) throws IOException {
        var sec = tmp.resolve("dmcl.secrets.toml");
        Files.writeString(sec, "[discord]\ntoken = \"from-toml\"\n");
        var r = new SecretResolver(Map.of(), null, sec);
        assertThat(r.resolveSecret("discord.token")).isEqualTo("from-toml");
    }

    @Test
    void missingEnvThrows() {
        var r = new SecretResolver(Map.of(), null, null);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> r.resolve("env:MISSING_VAR_XYZ"));
    }
}
