package com.westwardmc.dmcl.adapter.config;

import com.westwardmc.dmcl.core.domain.Scope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

final class TomlConfigLoaderTest {
    @Test
    void loadsTokenViaSecretResolver(@TempDir Path tmp) throws IOException {
        var cfg = tmp.resolve("dmcl.toml");
        Files.writeString(cfg, """
            [discord]
            token = "env:T"
            guild_id = 1

            [storage]
            db_path = "world"

            [avatars]
            provider = "mc-heads"
            size = 64

            [behavior]
            show_edits = true
            show_deletes = true
            show_reactions = false
            mc_mention_color = "aqua"
            ping_sound = "minecraft:block.note_block.bell"
            loop_guard_strict = true

            [[channels]]
            scope = "GLOBAL"
            channel_id = 100
            mc_format = "<{name}> {message}"
            discord_format = "{message}"

            [mentions]
            allow_everyone = false
            allow_here = false
            allow_role = ["MOD"]
            """);
        var loader = new TomlConfigLoader(new SecretResolver(Map.of("T", "tok"), null, null));
        var c = loader.load(cfg);
        assertThat(c.discordToken()).isEqualTo("tok");
        assertThat(c.guildId()).isEqualTo(1L);
        assertThat(c.channels()).hasSize(1);
        assertThat(c.channels().get(0).scope()).isEqualTo(Scope.GLOBAL);
        assertThat(c.behavior().showEdits()).isTrue();
        assertThat(c.behavior().showReactions()).isFalse();
    }
}
