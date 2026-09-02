package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.house.CapitalHouseRecord;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.UUID;

public final class CapitalHouseDataAccess {

    private CapitalHouseDataAccess() {
    }

    public static Collection<CapitalHouseRecord> getHouses(
            ServerLevel level,
            UUID capitalId
    ) {
        return CapitalHouseSavedData.get(level).getHouses(capitalId);
    }

    public static CapitalHouseRecord getHouse(
            ServerLevel level,
            UUID capitalId,
            UUID houseId
    ) {
        return CapitalHouseSavedData.get(level).getHouse(capitalId, houseId);
    }

    public static CapitalHouseRecord findHouseByName(
            ServerLevel level,
            UUID capitalId,
            String houseName
    ) {
        return CapitalHouseSavedData.get(level).findHouseByName(capitalId, houseName);
    }

    public static CapitalHouseRecord findHouseForMember(
            ServerLevel level,
            UUID capitalId,
            UUID memberId
    ) {
        return CapitalHouseSavedData.get(level).findHouseForMember(capitalId, memberId);
    }

    public static CapitalHouseRecord createHouse(
            ServerLevel level,
            UUID capitalId,
            UUID houseId,
            String houseName
    ) {
        return CapitalHouseSavedData.get(level).createHouse(capitalId, houseId, houseName);
    }

    public static void markDirty(ServerLevel level) {
        CapitalHouseSavedData.get(level).markDirty();
    }
}
