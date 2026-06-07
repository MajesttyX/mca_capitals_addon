package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class CapitalLifecycleHandler {

    public void onServerStarted(MinecraftServer server) {
        FabricServerAccess.setServer(server);
        CapitalManager.clearAll();
        CapitalResidentScanner.clearAllCaches();
        CapitalCourtWatcher.clearAllFingerprints();

        ServerLevel overworld = server.overworld();
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

    public void onServerStopped(MinecraftServer server) {
        CapitalManager.clearAll();
        CapitalResidentScanner.clearAllCaches();
        CapitalCourtWatcher.clearAllFingerprints();
        FabricServerAccess.clearServer(server);
    }
}