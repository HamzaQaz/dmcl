package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.domain.RenderedMcText;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Span.Plain;
import com.westwardmc.dmcl.core.domain.RenderedMcText.Style;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

final class DiscordMdTest {
    private Plain plainAt(RenderedMcText t, int idx) { return (Plain) t.spans().get(idx); }

    @Test
    void plainText() {
        var t = DiscordMd.parse("hello world");
        assertThat(t.spans()).hasSize(1);
        assertThat(plainAt(t, 0).text()).isEqualTo("hello world");
        assertThat(plainAt(t, 0).styles()).isEmpty();
    }

    @Test
    void boldText() {
        var t = DiscordMd.parse("a **b** c");
        assertThat(plainAt(t, 0).text()).isEqualTo("a ");
        assertThat(plainAt(t, 1).text()).isEqualTo("b");
        assertThat(plainAt(t, 1).styles()).contains(Style.BOLD);
        assertThat(plainAt(t, 2).text()).isEqualTo(" c");
    }

    @Test
    void italicWithUnderscore() {
        var t = DiscordMd.parse("_x_");
        assertThat(plainAt(t, 0).styles()).contains(Style.ITALIC);
    }

    @Test
    void italicWithSingleAsterisk() {
        var t = DiscordMd.parse("*x*");
        assertThat(plainAt(t, 0).styles()).contains(Style.ITALIC);
    }

    @Test
    void underline() {
        var t = DiscordMd.parse("__x__");
        assertThat(plainAt(t, 0).styles()).contains(Style.UNDERLINE);
    }

    @Test
    void strikethrough() {
        var t = DiscordMd.parse("~~x~~");
        assertThat(plainAt(t, 0).styles()).contains(Style.STRIKETHROUGH);
    }

    @Test
    void inlineCodeRendersGray() {
        var t = DiscordMd.parse("`x`");
        assertThat(plainAt(t, 0).text()).isEqualTo("x");
        assertThat(plainAt(t, 0).color()).isEqualTo(RenderedMcText.Color.GRAY);
    }

    @Test
    void codeBlockRendersGrayBlock() {
        var t = DiscordMd.parse("```\nfoo\nbar\n```");
        assertThat(plainAt(t, 0).text()).contains("foo").contains("bar");
        assertThat(plainAt(t, 0).color()).isEqualTo(RenderedMcText.Color.GRAY);
    }

    @Test
    void spoilerBecomesObfuscatedWithHover() {
        var t = DiscordMd.parse("||secret||");
        var span = plainAt(t, 0);
        assertThat(span.styles()).contains(Style.OBFUSCATED);
    }

    @Test
    void unmatchedDelimiterTreatedAsLiteral() {
        var t = DiscordMd.parse("**unclosed");
        assertThat(plainAt(t, 0).text()).isEqualTo("**unclosed");
    }

    @Test
    void empty() {
        assertThat(DiscordMd.parse("").spans()).isEmpty();
    }
}
