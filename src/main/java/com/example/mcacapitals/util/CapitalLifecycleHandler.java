package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalManager;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CapitalLifecycleHandler {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CapitalManager.clearAll();
        CapitalCourtWatcher.clearAllFingerprints();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        CapitalManager.clearAll();
        CapitalCourtWatcher.clearAllFingerprints();
    }
}