package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.domain.Attachment;
import com.westwardmc.dmcl.core.domain.RenderedMcText;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

final class AttachmentRendererTest {
    @Test
    void imageAttachmentRendersWithCameraEmoji() {
        var a = new Attachment(Attachment.Kind.IMAGE, URI.create("https://x/y.png"), "y.png", 1024);
        var text = AttachmentRenderer.render(List.of(a));
        var span = (RenderedMcText.Span.Hyperlink) text.spans().get(0);
        assertThat(span.text()).contains("📷");
        assertThat(span.url()).isEqualTo("https://x/y.png");
        assertThat(span.hover()).contains("y.png").contains("1.0 KB");
    }

    @Test
    void videoEmoji() {
        var a = new Attachment(Attachment.Kind.VIDEO, URI.create("https://x/v.mp4"), "v.mp4", 0);
        var text = AttachmentRenderer.render(List.of(a));
        assertThat(((RenderedMcText.Span.Hyperlink) text.spans().get(0)).text()).contains("🎬");
    }

    @Test
    void multipleAttachmentsSeparated() {
        var a = new Attachment(Attachment.Kind.IMAGE, URI.create("https://x/y.png"), "y.png", 1);
        var b = new Attachment(Attachment.Kind.FILE, URI.create("https://x/z.zip"), "z.zip", 1);
        var text = AttachmentRenderer.render(List.of(a, b));
        assertThat(text.spans()).hasSize(3);
    }

    @Test
    void emptyListReturnsEmpty() {
        assertThat(AttachmentRenderer.render(List.of()).spans()).isEmpty();
    }
}
