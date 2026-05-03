package com.westwardmc.dmcl;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DmclMod implements DedicatedServerModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("dmcl");

    @Override
    public void onInitializeServer() {
        LOG.info("DMCL initializing");
    }
}
