package com.n1ghtlydev.dmcl.adapter.mc;

import com.n1ghtlydev.dmcl.core.domain.RenderedMcText;
import com.n1ghtlydev.dmcl.core.domain.RenderedMcText.Color;
import com.n1ghtlydev.dmcl.core.domain.RenderedMcText.Span;
import com.n1ghtlydev.dmcl.core.domain.RenderedMcText.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.Set;

public final class McTextConverter {
    private McTextConverter() {}

    public static MutableComponent toComponent(RenderedMcText rendered) {
        MutableComponent out = Component.empty();
        for (var span : rendered.spans()) {
            out = out.append(spanToText(span));
        }
        return out;
    }

    private static MutableComponent spanToText(Span s) {
        return switch (s) {
            case Span.Plain p -> applyStyle(Component.literal(p.text()), p.styles(), p.color(), null, null);
            case Span.Hyperlink h -> applyStyle(Component.literal(h.text()),
                Set.of(), h.color(),
                McCompat.openUrl(h.url()),
                McCompat.showText(Component.literal(h.hover())));
            case Span.CopyToClipboard c -> applyStyle(Component.literal(c.text()),
                Set.of(), c.color(),
                McCompat.copy(c.value()),
                McCompat.showText(Component.literal(c.hover())));
            case Span.Ping p -> applyStyle(Component.literal(p.text()),
                Set.of(Style.BOLD), p.color(), null,
                McCompat.showText(Component.literal("ping")));
            case Span.Quoted q -> applyStyle(Component.literal(q.text()),
                Set.of(Style.ITALIC), q.color(),
                q.openUrl().isEmpty() ? null : McCompat.openUrl(q.openUrl()),
                McCompat.showText(Component.literal(q.hover())));
        };
    }

    private static MutableComponent applyStyle(MutableComponent t, Set<Style> styles, Color color,
                                          ClickEvent click, HoverEvent hover) {
        var s = t.getStyle();
        for (var st : styles) {
            s = switch (st) {
                case BOLD          -> s.withBold(true);
                case ITALIC        -> s.withItalic(true);
                case UNDERLINE     -> s.withUnderlined(true);
                case STRIKETHROUGH -> s.withStrikethrough(true);
                case OBFUSCATED    -> s.withObfuscated(true);
            };
        }
        if (color != null) s = s.withColor(toFormatting(color));
        if (click != null) s = s.withClickEvent(click);
        if (hover != null) s = s.withHoverEvent(hover);
        return t.setStyle(s);
    }

    private static ChatFormatting toFormatting(Color c) {
        return switch (c) {
            case BLACK         -> ChatFormatting.BLACK;
            case DARK_BLUE     -> ChatFormatting.DARK_BLUE;
            case DARK_GREEN    -> ChatFormatting.DARK_GREEN;
            case DARK_AQUA     -> ChatFormatting.DARK_AQUA;
            case DARK_RED      -> ChatFormatting.DARK_RED;
            case DARK_PURPLE   -> ChatFormatting.DARK_PURPLE;
            case GOLD          -> ChatFormatting.GOLD;
            case GRAY          -> ChatFormatting.GRAY;
            case DARK_GRAY     -> ChatFormatting.DARK_GRAY;
            case BLUE          -> ChatFormatting.BLUE;
            case GREEN         -> ChatFormatting.GREEN;
            case AQUA          -> ChatFormatting.AQUA;
            case RED           -> ChatFormatting.RED;
            case LIGHT_PURPLE  -> ChatFormatting.LIGHT_PURPLE;
            case YELLOW        -> ChatFormatting.YELLOW;
            case WHITE         -> ChatFormatting.WHITE;
        };
    }
}
