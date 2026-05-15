package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CapitalLifecycleHandler {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CapitalManager.clearAll();
        CapitalResidentScanner.clearAllCaches();
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

        for (CapitalRecord capital : data.getCapitals()) {
            if (capital == null || capital.getCapitalId() == null) {
                continue;
            }
            CapitalCourtWatcher.seedCurrentState(overworld, capital);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        CapitalManager.clearAll();
        CapitalResidentScanner.clearAllCaches();
        CapitalCourtWatcher.clearAllFingerprints();
    }
}