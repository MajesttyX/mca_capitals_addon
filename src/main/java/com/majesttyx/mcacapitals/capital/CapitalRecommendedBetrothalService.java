package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.MCARelationshipBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CapitalRecommendedBetrothalService {

    private CapitalRecommendedBetrothalService() {
    }

    public static boolean tick(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getVillageId() == null) {
            return false;
        }

        boolean changed = false;
        List<UUID> residents = new ArrayList<>(CapitalResidentScanner.scanResidents(level, capital.getCapitalId()));
        residents.sort(Comparator.comparing(UUID::toString));

        for (int i = 0; i < residents.size(); i++) {
            UUID firstId = residents.get(i);

            if (!isLoadedAdultMcaVillager(level, firstId)) {
                continue;
            }

            for (int j = i + 1; j < residents.size(); j++) {
                UUID secondId = residents.get(j);

                if (!isLoadedAdultMcaVillager(level, secondId)) {
                    continue;
                }

                Entity firstVillager = MCAIntegrationBridge.getEntityByUuid(level, firstId);
                Entity secondVillager = MCAIntegrationBridge.getEntityByUuid(level, secondId);

                if (!MCARelationshipBridge.areVillagersBetrothedToEachOther(firstVillager, secondVillager)) {
                    continue;
                }

                MCARelationshipBridge.BetrothalResult result =
                        MCARelationshipBridge.marryVillagerToVillager(firstVillager, secondVillager);

                if (!result.success()) {
                    continue;
                }

                String firstName = CapitalChronicleIdentitySnapshot.name(level, capital, firstId);
                String secondName = CapitalChronicleIdentitySnapshot.name(level, capital, secondId);
                String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

                CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.CAPITAL_MARRIAGE, firstName, secondName, villageName);

                CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
                CapitalDataAccess.markDirty(level);
                changed = true;
            }
        }

        return changed;
    }

    private static boolean isLoadedAdultMcaVillager(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return false;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return false;
        }

        if (!entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        String ageState = MCAIntegrationBridge.getAgeState(level, entityId);
        return "ADULT".equalsIgnoreCase(ageState);
    }

}
