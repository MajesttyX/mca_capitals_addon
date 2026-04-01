package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.data.CapitalSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CapitalLifecycleHandler {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CapitalManager.clearAll();
        CapitalCourtWatcher.clearAllFingerprints();

        ServerLevel overworld = event.getServer().overworld();
        if (overworld == null) {
            return;
        }

        CapitalSavedData data = CapitalDataAccess.get(overworld);
        for (CapitalRecord capital : data.getCapitals()) {
            if (capital == null || capital.getCapitalId() == null) {
                continue;
            }
            CapitalManager.putCapital(capital);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        CapitalManager.clearAll();
        CapitalCourtWatcher.clearAllFingerprints();
    }
}