package com.minewatch;

import com.minewatch.server.HeroManager;
import com.minewatch.server.PulseBombs;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

final class ServerLifecycleCleanup {
    static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { HeroManager.clear(); PulseBombs.clear(); });
    }
    private ServerLifecycleCleanup() {}
}
