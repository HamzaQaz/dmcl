package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.domain.*;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Color;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordToMc {
    private static final Pattern USER_MENTION = Pattern.compile("<@!?(\\d+)>");
    private static final Pattern ROLE_MENTION = Pattern.compile("<@&(\\d+)>");
    private static final Pattern CHANNEL_MENTION = Pattern.compile("<#(\\d+)>");
    private static final Pattern CUSTOM_EMOJI = Pattern.compile("<a?:([A-Za-z0-9_]+):\\d+>");

    private static final char PING_END = '';
    private static final Pattern PING_TOKEN = Pattern.compile("PING:([^" + PING_END + "]+)" + PING_END);

    private final MentionResolver resolver;
    private final String pingSound;

    public DiscordToMc(MentionResolver resolver, String pingSound) {
        this.resolver = resolver;
        this.pingSound = pingSound;
    }

    public RenderedMcText render(BridgeMessage msg) {
        var pieces = new ArrayList<RenderedMcText>();

        msg.replyTo().ifPresent(r -> {
            String hover = r.originalAuthor() + ": " + r.snippet();
            String openUrl = r.jumpUrl().map(java.net.URI::toString).orElse("");
            pieces.add(RenderedMcText.quoted("┌─ replying to " + r.originalAuthor() + ": " + truncate(r.snippet(), 40), hover, openUrl));
            pieces.add(RenderedMcText.text("\n"));
        });

        pieces.add(RenderedMcText.colored("[#] " + msg.author().displayName() + ": ", Color.GRAY, Style.BOLD));

        String prepared = rewriteMentionsAndEmoji(msg.body());
        var bodyParts = splitOnTokens(prepared);
        for (var part : bodyParts) pieces.add(part);

        if (!msg.attachments().isEmpty()) {
            pieces.add(RenderedMcText.text(" "));
            pieces.add(AttachmentRenderer.render(msg.attachments()));
        }

        if (msg.edited()) {
            pieces.add(RenderedMcText.colored(" (edited)", Color.DARK_GRAY, Style.ITALIC));
        }

        return RenderedMcText.concat(pieces.toArray(new RenderedMcText[0]));
    }

    private String rewriteMentionsAndEmoji(String body) {
        body = USER_MENTION.matcher(body).replaceAll(m -> {
            long id = Long.parseLong(m.group(1));
            var uuid = resolver.uuidFor(id);
            if (uuid.isPresent()) {
                String name = resolver.nameFor(uuid.get()).orElse("?");
                return Matcher.quoteReplacement("PING:" + name + PING_END);
            }
            return Matcher.quoteReplacement("@unknown");
        });
        body = ROLE_MENTION.matcher(body).replaceAll(m -> Matcher.quoteReplacement("@role:" + m.group(1)));
        body = CHANNEL_MENTION.matcher(body).replaceAll(m -> Matcher.quoteReplacement("#channel:" + m.group(1)));
        body = CUSTOM_EMOJI.matcher(body).replaceAll(m -> Matcher.quoteReplacement(":" + m.group(1) + ":"));
        return body;
    }

    private List<RenderedMcText> splitOnTokens(String body) {
        var out = new ArrayList<RenderedMcText>();
        Matcher m = PING_TOKEN.matcher(body);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                String chunk = body.substring(last, m.start());
                out.add(DiscordMd.parse(chunk));
            }
            out.add(RenderedMcText.ping("@" + m.group(1), pingSound));
            last = m.end();
        }
        if (last < body.length()) out.add(DiscordMd.parse(body.substring(last)));
        return out;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
