package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.domain.RenderedMcText;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Color;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Span;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Style;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class DiscordMd {
    private DiscordMd() {}

    public static RenderedMcText parse(String input) {
        if (input == null || input.isEmpty()) return new RenderedMcText(List.of());
        var p = new Parser(input);
        var spans = p.parseAll(EnumSet.noneOf(Style.class), null);
        return new RenderedMcText(spans);
    }

    private static EnumSet<Style> snapshot(Set<Style> styles) {
        return styles.isEmpty() ? EnumSet.noneOf(Style.class) : EnumSet.copyOf(styles);
    }

    private static final class Parser {
        private final String src;
        private int pos;
        Parser(String s) { this.src = s; }

        List<Span> parseAll(Set<Style> styles, Color color) {
            var out = new ArrayList<Span>();
            var literal = new StringBuilder();
            while (pos < src.length()) {
                if (tryEmit(out, literal, styles, color, "```")) { parseFenced(out); continue; }
                if (tryEmit(out, literal, styles, color, "**"))  { parseWrapped(out, styles, color, Style.BOLD, "**"); continue; }
                if (tryEmit(out, literal, styles, color, "__"))  { parseWrapped(out, styles, color, Style.UNDERLINE, "__"); continue; }
                if (tryEmit(out, literal, styles, color, "~~"))  { parseWrapped(out, styles, color, Style.STRIKETHROUGH, "~~"); continue; }
                if (tryEmit(out, literal, styles, color, "||"))  { parseWrapped(out, styles, color, Style.OBFUSCATED, "||"); continue; }
                char c = src.charAt(pos);
                if (c == '*') { tryEmitChar(out, literal, styles, color); parseWrappedChar(out, styles, color, Style.ITALIC, '*'); continue; }
                if (c == '_') { tryEmitChar(out, literal, styles, color); parseWrappedChar(out, styles, color, Style.ITALIC, '_'); continue; }
                if (c == '`') { tryEmitChar(out, literal, styles, color); parseInlineCode(out); continue; }
                literal.append(c);
                pos++;
            }
            flush(out, literal, styles, color);
            return out;
        }

        private boolean tryEmit(List<Span> out, StringBuilder lit, Set<Style> styles, Color color, String token) {
            if (src.startsWith(token, pos)) {
                flush(out, lit, styles, color);
                pos += token.length();
                return true;
            }
            return false;
        }

        private void tryEmitChar(List<Span> out, StringBuilder lit, Set<Style> styles, Color color) {
            flush(out, lit, styles, color);
            pos++;
        }

        private void flush(List<Span> out, StringBuilder lit, Set<Style> styles, Color color) {
            if (lit.length() == 0) return;
            out.add(new Span.Plain(lit.toString(), snapshot(styles), color));
            lit.setLength(0);
        }

        private void parseWrapped(List<Span> out, Set<Style> outerStyles, Color color, Style add, String token) {
            int end = src.indexOf(token, pos);
            if (end < 0) {
                out.add(new Span.Plain(token + src.substring(pos), snapshot(outerStyles), color));
                pos = src.length();
                return;
            }
            var inner = src.substring(pos, end);
            pos = end + token.length();
            var nested = new Parser(inner);
            var nestedStyles = snapshot(outerStyles);
            nestedStyles.add(add);
            out.addAll(nested.parseAll(nestedStyles, color));
        }

        private void parseWrappedChar(List<Span> out, Set<Style> outerStyles, Color color, Style add, char token) {
            int end = src.indexOf(token, pos);
            if (end < 0) {
                out.add(new Span.Plain(token + src.substring(pos), snapshot(outerStyles), color));
                pos = src.length();
                return;
            }
            var inner = src.substring(pos, end);
            pos = end + 1;
            var nested = new Parser(inner);
            var nestedStyles = snapshot(outerStyles);
            nestedStyles.add(add);
            out.addAll(nested.parseAll(nestedStyles, color));
        }

        private void parseInlineCode(List<Span> out) {
            int end = src.indexOf('`', pos);
            if (end < 0) {
                out.add(new Span.Plain("`" + src.substring(pos), EnumSet.noneOf(Style.class), null));
                pos = src.length();
                return;
            }
            out.add(new Span.Plain(src.substring(pos, end), EnumSet.noneOf(Style.class), Color.GRAY));
            pos = end + 1;
        }

        private void parseFenced(List<Span> out) {
            int end = src.indexOf("```", pos);
            if (end < 0) end = src.length();
            String body = src.substring(pos, end).replaceFirst("^[a-zA-Z0-9]*\\n", "");
            out.add(new Span.Plain(body.strip(), EnumSet.noneOf(Style.class), Color.GRAY));
            pos = end + (end == src.length() ? 0 : 3);
        }
    }
}
