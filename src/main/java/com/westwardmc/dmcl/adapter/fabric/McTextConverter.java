package com.westwardmc.dmcl.adapter.fabric;

import com.westwardmc.dmcl.core.domain.RenderedMcText;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Color;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Span;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Style;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;

public final class McTextConverter {
    private McTextConverter() {}

    public static MutableText toText(RenderedMcText rendered) {
        MutableText out = Text.empty();
        for (var span : rendered.spans()) {
            out = out.append(spanToText(span));
        }
        return out;
    }

    private static MutableText spanToText(Span s) {
        return switch (s) {
            case Span.Plain p -> applyStyle(Text.literal(p.text()), p.styles(), p.color(), null, null);
            case Span.Hyperlink h -> applyStyle(Text.literal(h.text()),
                Set.of(), h.color(),
                new ClickEvent(ClickEvent.Action.OPEN_URL, h.url()),
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(h.hover())));
            case Span.CopyToClipboard c -> applyStyle(Text.literal(c.text()),
                Set.of(), c.color(),
                new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, c.value()),
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(c.hover())));
            case Span.Ping p -> applyStyle(Text.literal(p.text()),
                Set.of(Style.BOLD), p.color(), null,
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("ping")));
            case Span.Quoted q -> applyStyle(Text.literal(q.text()),
                Set.of(Style.ITALIC), q.color(),
                q.openUrl().isEmpty() ? null : new ClickEvent(ClickEvent.Action.OPEN_URL, q.openUrl()),
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(q.hover())));
        };
    }

    private static MutableText applyStyle(MutableText t, Set<Style> styles, Color color,
                                          ClickEvent click, HoverEvent hover) {
        var s = t.getStyle();
        for (var st : styles) {
            s = switch (st) {
                case BOLD          -> s.withBold(true);
                case ITALIC        -> s.withItalic(true);
                case UNDERLINE     -> s.withUnderline(true);
                case STRIKETHROUGH -> s.withStrikethrough(true);
                case OBFUSCATED    -> s.withObfuscated(true);
            };
        }
        if (color != null) s = s.withColor(toFormatting(color));
        if (click != null) s = s.withClickEvent(click);
        if (hover != null) s = s.withHoverEvent(hover);
        return t.setStyle(s);
    }

    private static Formatting toFormatting(Color c) {
        return switch (c) {
            case BLACK         -> Formatting.BLACK;
            case DARK_BLUE     -> Formatting.DARK_BLUE;
            case DARK_GREEN    -> Formatting.DARK_GREEN;
            case DARK_AQUA     -> Formatting.DARK_AQUA;
            case DARK_RED      -> Formatting.DARK_RED;
            case DARK_PURPLE   -> Formatting.DARK_PURPLE;
            case GOLD          -> Formatting.GOLD;
            case GRAY          -> Formatting.GRAY;
            case DARK_GRAY     -> Formatting.DARK_GRAY;
            case BLUE          -> Formatting.BLUE;
            case GREEN         -> Formatting.GREEN;
            case AQUA          -> Formatting.AQUA;
            case RED           -> Formatting.RED;
            case LIGHT_PURPLE  -> Formatting.LIGHT_PURPLE;
            case YELLOW        -> Formatting.YELLOW;
            case WHITE         -> Formatting.WHITE;
        };
    }
}
