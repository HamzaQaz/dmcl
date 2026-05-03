package com.westwardmc.dmcl.core.port;

import java.time.Instant;

public interface Clock {
    Instant now();
    static Clock system() { return Instant::now; }
}
