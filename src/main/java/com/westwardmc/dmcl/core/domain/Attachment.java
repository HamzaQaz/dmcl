package com.westwardmc.dmcl.core.domain;

import java.net.URI;

public record Attachment(Kind kind, URI url, String filename, long sizeBytes) {
    public enum Kind { IMAGE, VIDEO, AUDIO, FILE }
}
