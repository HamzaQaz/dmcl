package com.westwardmc.dmcl.adapter.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.westwardmc.dmcl.core.domain.Scope;
import com.westwardmc.dmcl.core.port.ChannelBinding;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TomlConfigLoader {
    private final SecretResolver secrets;

    public TomlConfigLoader(SecretResolver secrets) { this.secrets = secrets; }

    public DmclConfig load(Path tomlPath) {
        try (var cfg = FileConfig.of(tomlPath)) {
            cfg.load();
            String token = secrets.resolve((String) cfg.get("discord.token"));
            long guild = ((Number) cfg.get("discord.guild_id")).longValue();
            String status = cfg.getOrElse("discord.status", "");

            var storage = new DmclConfig.Storage(cfg.getOrElse("storage.db_path", "world"));
            var avatars = new DmclConfig.Avatars(
                cfg.getOrElse("avatars.provider", "mc-heads"),
                cfg.getIntOrElse("avatars.size", 64));
            var behavior = new DmclConfig.Behavior(
                cfg.getOrElse("behavior.show_edits", true),
                cfg.getOrElse("behavior.show_deletes", true),
                cfg.getOrElse("behavior.show_reactions", false),
                cfg.getOrElse("behavior.mc_mention_color", "aqua"),
                cfg.getOrElse("behavior.ping_sound", "minecraft:block.note_block.bell"),
                cfg.getOrElse("behavior.loop_guard_strict", true));
            var mentions = new DmclConfig.Mentions(
                cfg.getOrElse("mentions.allow_everyone", false),
                cfg.getOrElse("mentions.allow_here", false),
                cfg.getOrElse("mentions.allow_role", List.of()));

            List<ChannelBinding> channels = new ArrayList<>();
            List<Config> arr = cfg.getOrElse("channels", List.of());
            for (Config c : arr) {
                var scope = Scope.valueOf((String) c.get("scope"));
                long cid = ((Number) c.get("channel_id")).longValue();
                channels.add(new ChannelBinding(
                    scope, cid,
                    c.getOrElse("mc_format", "<{name}> {message}"),
                    c.getOrElse("discord_format", "{message}"),
                    Optional.ofNullable((String) c.get("mc_permission")),
                    Optional.ofNullable((String) c.get("mc_command")),
                    c.getOrElse("mc_send", true)));
            }

            return new DmclConfig(token, guild, status, storage, avatars, behavior, channels, mentions);
        }
    }
}
