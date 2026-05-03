package com.westwardmc.dmcl.core.domain;

import java.net.URI;
import java.util.Optional;

public record ReplyContext(String snippet, String originalAuthor, Optional<URI> jumpUrl) {}
