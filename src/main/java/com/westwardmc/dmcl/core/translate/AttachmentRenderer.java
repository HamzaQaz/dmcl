package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.domain.Attachment;
import com.westwardmc.dmcl.core.domain.RenderedMcText;

import java.util.ArrayList;
import java.util.List;

public final class AttachmentRenderer {
    private AttachmentRenderer() {}

    public static RenderedMcText render(List<Attachment> attachments) {
        if (attachments.isEmpty()) return new RenderedMcText(List.of());
        var parts = new ArrayList<RenderedMcText>();
        for (int i = 0; i < attachments.size(); i++) {
            if (i > 0) parts.add(RenderedMcText.text(" "));
            parts.add(renderOne(attachments.get(i)));
        }
        return RenderedMcText.concat(parts.toArray(new RenderedMcText[0]));
    }

    private static RenderedMcText renderOne(Attachment a) {
        String emoji = switch (a.kind()) {
            case IMAGE -> "📷";
            case VIDEO -> "🎬";
            case AUDIO -> "🔊";
            case FILE  -> "📎";
        };
        String label = switch (a.kind()) {
            case IMAGE -> emoji + " image";
            case VIDEO, AUDIO, FILE -> emoji + " " + a.filename();
        };
        String hover = a.filename() + " (" + humanSize(a.sizeBytes()) + ")";
        return RenderedMcText.hyperlink("[" + label + "]", a.url().toString(), hover);
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        return String.format("%.1f MB", kb / 1024.0);
    }
}
