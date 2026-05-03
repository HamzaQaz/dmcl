package com.westwardmc.dmcl.core.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

final class RenderedMcTextTest {
    @Test
    void plainText() {
        var t = RenderedMcText.text("hello");
        assertThat(t.spans()).hasSize(1);
        assertThat(t.spans().get(0)).isInstanceOf(RenderedMcText.Span.Plain.class);
    }

    @Test
    void boldStyling() {
        var t = RenderedMcText.text("hi", RenderedMcText.Style.BOLD);
        var span = (RenderedMcText.Span.Plain) t.spans().get(0);
        assertThat(span.styles()).contains(RenderedMcText.Style.BOLD);
    }

    @Test
    void hyperlinkSpan() {
        var t = RenderedMcText.hyperlink("[image]", "https://x.com/a.png", "filename: a.png");
        var span = (RenderedMcText.Span.Hyperlink) t.spans().get(0);
        assertThat(span.url()).isEqualTo("https://x.com/a.png");
        assertThat(span.hover()).isEqualTo("filename: a.png");
    }

    @Test
    void compose() {
        var a = RenderedMcText.text("hello ");
        var b = RenderedMcText.text("world", RenderedMcText.Style.BOLD);
        var combined = RenderedMcText.concat(a, b);
        assertThat(combined.spans()).hasSize(2);
    }

    @Test
    void copyToClipboardSpan() {
        var t = RenderedMcText.copyToClipboard("ABC123", "Click to copy code");
        var span = (RenderedMcText.Span.CopyToClipboard) t.spans().get(0);
        assertThat(span.value()).isEqualTo("ABC123");
    }

    @Test
    void perRecipientPing() {
        var t = RenderedMcText.ping("@Steve", "minecraft:block.note_block.bell");
        var span = (RenderedMcText.Span.Ping) t.spans().get(0);
        assertThat(span.sound()).isEqualTo("minecraft:block.note_block.bell");
    }
}
