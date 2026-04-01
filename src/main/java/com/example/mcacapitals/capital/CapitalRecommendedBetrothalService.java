package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.MCARelationshipBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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

                String firstName = buildDisplayName(level, capital, firstId);
                String secondName = buildDisplayName(level, capital, secondId);
                String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

                CapitalChronicleService.addEntry(
                        level,
                        capital,
                        firstName + " and " + secondName + " were married in " + villageName + "."
                );

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

    private static String buildDisplayName(ServerLevel level, CapitalRecord capital, UUID entityId) {
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        String baseName = entity != null ? entity.getName().getString() : entityId.toString();
        String displayTitle = CapitalTitleResolver.getDisplayTitle(level, capital, entityId);

        if (displayTitle == null || displayTitle.isBlank() || "Commoner".equals(displayTitle) || "None".equals(displayTitle)) {
            return baseName;
        }

        if (baseName.startsWith(displayTitle + " ")) {
            return baseName;
        }

        return displayTitle + " " + baseName;
    }
}