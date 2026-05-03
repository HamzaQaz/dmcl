package com.westwardmc.dmcl.core.translate;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

final class McToDiscordTest {
    @Test
    void stripsLegacyColorCodes() {
        assertThat(McToDiscord.stripColorCodes("§chello §rworld")).isEqualTo("hello world");
    }

    @Test
    void stripsBothCaretAndAmpersandFormCodes() {
        assertThat(McToDiscord.stripColorCodes("§chello&aworld")).isEqualTo("hello&aworld");
    }

    @Test
    void escapesDiscordMarkdownSpecials() {
        assertThat(McToDiscord.escapeMarkdown("*test* _x_ ~y~ |spoiler| > q `code`"))
            .isEqualTo("\\*test\\* \\_x\\_ \\~y\\~ \\|spoiler\\| \\> q \\`code\\`");
    }

    @Test
    void fullPipelineStripsThenEscapes() {
        assertThat(McToDiscord.translate("§ahello *world*"))
            .isEqualTo("hello \\*world\\*");
    }

    @Test
    void emptyAndNullSafe() {
        assertThat(McToDiscord.translate("")).isEqualTo("");
        assertThat(McToDiscord.translate(null)).isEqualTo("");
    }
}
