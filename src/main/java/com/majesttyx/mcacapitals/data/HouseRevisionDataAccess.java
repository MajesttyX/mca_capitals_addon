package com.majesttyx.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class HouseRevisionDataAccess {

    private HouseRevisionDataAccess() {
    }

    public static HouseRevisionSavedData get(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return null;
        }

        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                HouseRevisionSavedData::new,
                                HouseRevisionSavedData::load,
                                null
                        ),
                        HouseRevisionSavedData.DATA_NAME
                );
    }
}
