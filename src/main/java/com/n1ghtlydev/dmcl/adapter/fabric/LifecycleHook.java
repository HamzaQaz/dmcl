package com.n1ghtlydev.dmcl.adapter.fabric;

import com.n1ghtlydev.dmcl.adapter.mc.VanillaMinecraftAdapter;
import com.n1ghtlydev.dmcl.core.port.LifecycleEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class LifecycleHook {
    private LifecycleHook() {}

    public static void register(VanillaMinecraftAdapter adapter) {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> adapter.lifecycleHandler().accept(new LifecycleEvent.Started()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> adapter.lifecycleHandler().accept(new LifecycleEvent.Stopped()));
    }
}
