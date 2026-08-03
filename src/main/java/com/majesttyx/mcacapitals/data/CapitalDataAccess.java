package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class CapitalDataAccess {

    private CapitalDataAccess() {
    }

    public static CapitalSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                CapitalSavedData::new,
                                CapitalSavedData::load,
                                null
                        ),
                        CapitalSavedData.DATA_NAME
                );
    }

    public static void markDirty(ServerLevel level) {
        CapitalSavedData data = get(level);
        data.getCapitals().clear();

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null) {
                data.getCapitals().add(capital);
            }
        }

        data.setDirty();
    }
}