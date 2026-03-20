package com.example.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class CapitalDataAccess {

    private CapitalDataAccess() {
    }

    public static CapitalSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(CapitalSavedData::load, CapitalSavedData::new, CapitalSavedData.DATA_NAME);
    }

    public static void markDirty(ServerLevel level) {
        get(level).setDirty();
    }
}