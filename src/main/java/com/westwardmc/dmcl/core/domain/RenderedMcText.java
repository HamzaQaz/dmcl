package com.westwardmc.dmcl.core.domain;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record RenderedMcText(List<Span> spans) {

    public RenderedMcText { spans = List.copyOf(spans); }

    public enum Style { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, OBFUSCATED }

    public enum Color {
        BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD,
        GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE
    }

    public sealed interface Span {
        record Plain(String text, Set<Style> styles, Color color) implements Span {
            public Plain {
                styles = styles.isEmpty() ? EnumSet.noneOf(Style.class) : EnumSet.copyOf(styles);
            }
        }
        record Hyperlink(String text, String url, String hover, Color color) implements Span {}
        record CopyToClipboard(String text, String value, String hover, Color color) implements Span {}
        record Ping(String text, String sound, Color color) implements Span {}
        record Quoted(String text, String hover, String openUrl, Color color) implements Span {}
    }

    private static EnumSet<Style> styleSet(Style[] styles) {
        var set = EnumSet.noneOf(Style.class);
        for (var s : styles) set.add(s);
        return set;
    }

    public static RenderedMcText text(String s, Style... styles) {
        return new RenderedMcText(List.of(new Span.Plain(s, styleSet(styles), null)));
    }

    public static RenderedMcText colored(String s, Color color, Style... styles) {
        return new RenderedMcText(List.of(new Span.Plain(s, styleSet(styles), color)));
    }

    public static RenderedMcText hyperlink(String text, String url, String hover) {
        return new RenderedMcText(List.of(new Span.Hyperlink(text, url, hover, Color.AQUA)));
    }

    public static RenderedMcText copyToClipboard(String text, String hover) {
        return new RenderedMcText(List.of(new Span.CopyToClipboard(text, text, hover, Color.YELLOW)));
    }

    public static RenderedMcText ping(String displayText, String sound) {
        return new RenderedMcText(List.of(new Span.Ping(displayText, sound, Color.AQUA)));
    }

    public static RenderedMcText quoted(String text, String hover, String openUrl) {
        return new RenderedMcText(List.of(new Span.Quoted(text, hover, openUrl, Color.DARK_GRAY)));
    }

    public static RenderedMcText concat(RenderedMcText... parts) {
        var all = new ArrayList<Span>();
        for (var p : parts) all.addAll(p.spans);
        return new RenderedMcText(all);
    }
}
