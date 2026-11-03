package com.n1ghtlydev.dmcl.paper;

import com.n1ghtlydev.dmcl.core.domain.RenderedMcText;
import com.n1ghtlydev.dmcl.core.domain.RenderedMcText.Color;
import com.n1ghtlydev.dmcl.core.domain.RenderedMcText.Span;
import com.n1ghtlydev.dmcl.core.domain.RenderedMcText.Style;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Set;

final class PaperTextConverter {
    private PaperTextConverter() {}

    static Component toComponent(RenderedMcText rendered) {
        var out = Component.text();
        for (var span : rendered.spans()) out.append(spanToComponent(span));
        return out.build();
    }

    private static Component spanToComponent(Span s) {
        return switch (s) {
            case Span.Plain p -> style(p.text(), p.styles(), p.color(), null, null);
            case Span.Hyperlink h -> style(h.text(), Set.of(), h.color(),
                ClickEvent.openUrl(h.url()), HoverEvent.showText(Component.text(h.hover())));
            case Span.CopyToClipboard c -> style(c.text(), Set.of(), c.color(),
                ClickEvent.copyToClipboard(c.value()), HoverEvent.showText(Component.text(c.hover())));
            case Span.Ping p -> style(p.text(), Set.of(Style.BOLD), p.color(),
                null, HoverEvent.showText(Component.text("ping")));
            case Span.Quoted q -> style(q.text(), Set.of(Style.ITALIC), q.color(),
                q.openUrl().isEmpty() ? null : ClickEvent.openUrl(q.openUrl()),
                HoverEvent.showText(Component.text(q.hover())));
        };
    }

    private static Component style(String text, Set<Style> styles, Color color,
                                   ClickEvent click, HoverEvent<Component> hover) {
        var c = Component.text(text);
        for (var st : styles) {
            c = c.decoration(switch (st) {
                case BOLD -> TextDecoration.BOLD;
                case ITALIC -> TextDecoration.ITALIC;
                case UNDERLINE -> TextDecoration.UNDERLINED;
                case STRIKETHROUGH -> TextDecoration.STRIKETHROUGH;
                case OBFUSCATED -> TextDecoration.OBFUSCATED;
            }, true);
        }
        if (color != null) c = c.color(toNamed(color));
        if (click != null) c = c.clickEvent(click);
        if (hover != null) c = c.hoverEvent(hover);
        return c;
    }

    private static NamedTextColor toNamed(Color c) {
        return switch (c) {
            case BLACK -> NamedTextColor.BLACK;
            case DARK_BLUE -> NamedTextColor.DARK_BLUE;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case DARK_RED -> NamedTextColor.DARK_RED;
            case DARK_PURPLE -> NamedTextColor.DARK_PURPLE;
            case GOLD -> NamedTextColor.GOLD;
            case GRAY -> NamedTextColor.GRAY;
            case DARK_GRAY -> NamedTextColor.DARK_GRAY;
            case BLUE -> NamedTextColor.BLUE;
            case GREEN -> NamedTextColor.GREEN;
            case AQUA -> NamedTextColor.AQUA;
            case RED -> NamedTextColor.RED;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case YELLOW -> NamedTextColor.YELLOW;
            case WHITE -> NamedTextColor.WHITE;
        };
    }
}
