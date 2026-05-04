package com.westwardmc.dmcl.adapter.config;

import com.westwardmc.dmcl.core.domain.Scope;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class ConfigValidator {
    private ConfigValidator() {}

    public static List<String> validate(DmclConfig c) {
        var errors = new ArrayList<String>();
        if (c.discordToken() == null || c.discordToken().isBlank()) errors.add("discord.token must not be empty");
        if (c.guildId() <= 0) errors.add("discord.guild_id must be a positive snowflake");
        if (c.channels().isEmpty()) errors.add("at least one [[channels]] entry required");

        var seen = EnumSet.noneOf(Scope.class);
        for (var ch : c.channels()) {
            if (!seen.add(ch.scope())) errors.add("duplicate scope in channels: " + ch.scope());
            if (ch.channelId() <= 0) errors.add("channel_id must be a positive snowflake for scope " + ch.scope());
        }
        return errors;
    }
}
