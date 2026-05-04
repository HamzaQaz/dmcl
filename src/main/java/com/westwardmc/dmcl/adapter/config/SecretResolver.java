package com.westwardmc.dmcl.adapter.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SecretResolver {
    private final Map<String, String> env;
    private final Path envFile;
    private final Path secretsToml;
    private final Map<String, String> envFileCache;

    public SecretResolver(Map<String, String> env, Path envFile, Path secretsToml) {
        this.env = env;
        this.envFile = envFile;
        this.secretsToml = secretsToml;
        this.envFileCache = new HashMap<>();
        if (envFile != null && Files.exists(envFile)) loadEnvFile(envFile);
    }

    private void loadEnvFile(Path p) {
        try {
            for (String line : Files.readAllLines(p)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;
                envFileCache.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + p, e);
        }
    }

    public String resolve(String value) {
        if (value == null) return null;
        if (value.startsWith("env:")) {
            String key = value.substring(4);
            String v = env.get(key);
            if (v != null) return v;
            v = envFileCache.get(key);
            if (v != null) return v;
            throw new IllegalStateException("Missing env value: " + key
                + " (looked in OS env, " + envFile + ")");
        }
        return value;
    }

    public String resolveSecret(String dottedPath) {
        if (secretsToml == null || !Files.exists(secretsToml))
            throw new IllegalStateException("No secrets.toml configured for " + dottedPath);
        try (var cfg = FileConfig.of(secretsToml, TomlFormat.instance())) {
            cfg.load();
            Object o = cfg.get(dottedPath);
            if (o == null) throw new IllegalStateException("Missing secret: " + dottedPath);
            return o.toString();
        }
    }

    public Optional<String> tryResolve(String value) {
        try { return Optional.ofNullable(resolve(value)); }
        catch (IllegalStateException e) { return Optional.empty(); }
    }
}
