package com.westwardmc.dmcl.adapter.fabric;

import com.westwardmc.dmcl.core.port.LifecycleEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class LifecycleHook {
    private LifecycleHook() {}

    public static void register(FabricMinecraftAdapter adapter) {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> adapter.lifecycleHandler().accept(new LifecycleEvent.Started()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> adapter.lifecycleHandler().accept(new LifecycleEvent.Stopped()));
    }
}
