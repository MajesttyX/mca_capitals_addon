package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

public final class CapitalMaesterService {

    public static final int REQUIRED_POPULATION = 30;

    private CapitalMaesterService() {
    }

    public static boolean tickGrandMaester(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (level == null || capital == null || residents == null) {
            return false;
        }

        boolean changed = false;
        UUID previousGrandMaester = capital.getGrandMaester();

        if (!CapitalMaesterSelection.isValidGrandMaester(level, capital, previousGrandMaester, residents)) {
            if (previousGrandMaester != null) {
                capital.setGrandMaester(null);
                capital.setGrandMaesterFemale(false);
                changed = true;
            }
        }

        if (capital.getGrandMaester() == null
                && !capital.isPlayerSovereign()
                && capital.getSovereign() != null
                && CapitalMaesterSelection.isEligibleForNewGrandMaester(level, capital)) {
            UUID newGrandMaester = CapitalMaesterSelection.findGrandMaesterCandidate(level, capital, residents);
            if (newGrandMaester != null) {
                capital.setGrandMaester(newGrandMaester);
                capital.setGrandMaesterFemale(MCAIntegrationBridge.isFemale(level, newGrandMaester));

                String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
                String name = CapitalChronicleIdentitySnapshot.name(level, capital, newGrandMaester);
                CapitalChronicleService.addEvent(
                        level,
                        capital,
                        CapitalChronicleEventId.GRAND_MAESTER_APPOINTED,
                        name,
                        villageName
                );
                changed = true;
            }
        }

        if (changed) {
            CapitalNameService.refreshCapitalNames(level, capital, residents);
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static boolean isMaester(ServerLevel level, CapitalRecord capital, UUID villagerId, Set<UUID> residents) {
        return CapitalMaesterSelection.isMaester(level, capital, villagerId, residents);
    }

    public static boolean isEligibleGrandMaesterCandidate(ServerLevel level, CapitalRecord capital, UUID villagerId, Set<UUID> residents) {
        return CapitalMaesterSelection.isValidGrandMaester(level, capital, villagerId, residents);
    }

    private static String resolveName(ServerLevel level, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        return entity != null ? entity.getName().getString() : entityId.toString();
    }
}