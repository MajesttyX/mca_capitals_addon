package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.List;

public final class CapitalBuildingService {

    public static final String BIG_HOUSE = "big_house";
    public static final String PRISON = "prison";
    public static final String INN = "inn";
    public static final String STORAGE = "storage";

    private CapitalBuildingService() {
    }

    public static int countBigHouses(ServerLevel level, CapitalRecord capital) {
        return countBuildings(level, capital, BIG_HOUSE);
    }

    public static boolean hasPrison(ServerLevel level, CapitalRecord capital) {
        return countBuildings(level, capital, PRISON) > 0;
    }

    public static boolean hasInn(ServerLevel level, CapitalRecord capital) {
        return countBuildings(level, capital, INN) > 0;
    }

    public static boolean hasStorage(ServerLevel level, CapitalRecord capital) {
        return countBuildings(level, capital, STORAGE) > 0;
    }

    public static boolean hasAmbassadorBuildings(ServerLevel level, CapitalRecord capital) {
        return hasInn(level, capital) && hasStorage(level, capital);
    }

    public static List<AABB> getPrisonBounds(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getVillageId() == null) {
            return Collections.emptyList();
        }

        return MCAIntegrationBridge.getBuildingBoundsOfType(
                level,
                capital.getVillageId(),
                PRISON
        );
    }

    public static List<BlockPos> getPrisonCenters(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getVillageId() == null) {
            return Collections.emptyList();
        }

        return MCAIntegrationBridge.getBuildingCentersOfType(
                level,
                capital.getVillageId(),
                PRISON
        );
    }

    private static int countBuildings(
            ServerLevel level,
            CapitalRecord capital,
            String buildingType
    ) {
        if (level == null
                || capital == null
                || capital.getVillageId() == null
                || buildingType == null
                || buildingType.isBlank()) {
            return 0;
        }

        return MCAIntegrationBridge.countBuildingsOfType(
                level,
                capital.getVillageId(),
                buildingType
        );
    }
}