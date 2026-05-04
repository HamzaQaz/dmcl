package com.westwardmc.dmcl;

import com.westwardmc.dmcl.adapter.fabric.FabricMinecraftAdapter;
import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DmclMod implements DedicatedServerModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("dmcl");

    private static volatile FabricMinecraftAdapter fabricAdapter;

    public static FabricMinecraftAdapter fabricAdapter() { return fabricAdapter; }
    public static void setFabricAdapter(FabricMinecraftAdapter adapter) { fabricAdapter = adapter; }

    @Override
    public void onInitializeServer() {
        LOG.info("DMCL initializing");
    }
}
