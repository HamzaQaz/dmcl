package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.domain.LinkedAccount;
import com.westwardmc.dmcl.core.port.OnlinePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MentionResolver {
    private static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9_]{1,16})");

    private final Map<String, UUID> nameToUuid = new HashMap<>();
    private final Map<UUID, String> uuidToName = new HashMap<>();
    private final Map<UUID, Long> uuidToDiscord = new HashMap<>();

    public MentionResolver(List<LinkedAccount> linked, Set<OnlinePlayer> online) {
        for (var p : online) {
            nameToUuid.put(p.name().toLowerCase(), p.uuid());
            uuidToName.put(p.uuid(), p.name());
        }
        for (var l : linked) uuidToDiscord.put(l.mcUuid(), l.discordId());
    }

    public String resolveOutbound(String mcText) {
        Matcher m = MENTION.matcher(mcText);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String replacement = resolveOne(name);
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    private String resolveOne(String name) {
        UUID uuid = nameToUuid.get(name.toLowerCase());
        if (uuid == null) return "@" + name;
        Long discordId = uuidToDiscord.get(uuid);
        String real = uuidToName.get(uuid);
        if (discordId != null) return "<@" + discordId + ">";
        return "**" + real + "**";
    }

    public Optional<UUID> uuidFor(long discordId) {
        return uuidToDiscord.entrySet().stream()
            .filter(e -> e.getValue() == discordId)
            .map(Map.Entry::getKey)
            .findFirst();
    }

    public Optional<String> nameFor(UUID uuid) {
        return Optional.ofNullable(uuidToName.get(uuid));
    }
}
