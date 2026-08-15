package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

public final class CapitalMasterOfLawsService {

    private CapitalMasterOfLawsService() {
    }

    public static boolean tickMasterOfLaws(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || residents == null
                || capital.getState() != CapitalState.ACTIVE) {
            return false;
        }

        if (!CapitalBuildingService.hasPrison(level, capital)) {
            return clearMasterOfLaws(
                    level,
                    capital
            );
        }

        UUID current = capital.getMasterOfLaws();

        if (current != null
                && CapitalMasterOfLawsSelection.isEligible(
                level,
                capital,
                residents,
                current
        )) {
            boolean female = MCAIntegrationBridge.isFemale(
                    level,
                    current
            );

            if (capital.isMasterOfLawsFemale() != female) {
                capital.setMasterOfLawsFemale(female);
                CapitalDataAccess.markDirty(level);
                return true;
            }

            return false;
        }

        UUID selected = CapitalMasterOfLawsSelection.select(
                level,
                capital,
                residents
        );

        if (selected == null) {
            return clearMasterOfLaws(
                    level,
                    capital
            );
        }

        capital.setMasterOfLaws(selected);

        capital.setMasterOfLawsFemale(
                MCAIntegrationBridge.isFemale(
                        level,
                        selected
                )
        );

        String name = CapitalNameService.resolveDisplayName(
                level,
                capital,
                selected
        );

        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.MASTER_OF_LAWS_APPOINTED, name, MCAIntegrationBridge.getVillageName(level, capital.getVillageId()));

        sync(level, selected);
        CapitalDataAccess.markDirty(level);

        return true;
    }

    public static boolean isEligibleCandidate(
            ServerLevel level,
            CapitalRecord capital,
            UUID candidateId,
            Set<UUID> residents
    ) {
        return CapitalMasterOfLawsSelection.isEligible(
                level,
                capital,
                residents,
                candidateId
        );
    }

    public static boolean appointMasterOfLaws(
            ServerLevel level,
            CapitalRecord capital,
            UUID candidateId,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || candidateId == null
                || residents == null
                || !CapitalBuildingService.hasPrison(
                level,
                capital
        )
                || !isEligibleCandidate(
                level,
                capital,
                candidateId,
                residents
        )
                || candidateId.equals(
                capital.getMasterOfLaws()
        )) {
            return false;
        }

        UUID previous = capital.getMasterOfLaws();

        String capitalName =
                MCAIntegrationBridge.getVillageName(
                        level,
                        capital.getVillageId()
                );

        if (previous != null && !previous.equals(candidateId)) {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.MASTER_OF_LAWS_RELIEVED,
                    CapitalNameService.resolveDisplayName(level, capital, previous),
                    capitalName
            );
        }

        capital.setMasterOfLaws(candidateId);

        capital.setMasterOfLawsFemale(
                MCAIntegrationBridge.isFemale(
                        level,
                        candidateId
                )
        );

        CapitalNameService.refreshCapitalNames(
                level,
                capital,
                residents
        );

        CapitalCourtWatcher.clearFingerprint(
                capital.getCapitalId()
        );

        if (previous != null
                && !previous.equals(candidateId)) {
            sync(
                    level,
                    previous
            );
        }

        sync(
                level,
                candidateId
        );

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.MASTER_OF_LAWS_APPOINTED,
                CapitalNameService.resolveDisplayName(level, capital, candidateId),
                capitalName
        );

        CapitalDataAccess.markDirty(level);

        return true;
    }

    public static boolean hasUnlockedJustice(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return capital != null
                && capital.getMasterOfLaws() != null
                && CapitalBuildingService.hasPrison(
                level,
                capital
        );
    }

    private static boolean clearMasterOfLaws(
            ServerLevel level,
            CapitalRecord capital
    ) {
        UUID previous = capital.getMasterOfLaws();

        if (previous == null) {
            return false;
        }

        capital.setMasterOfLaws(null);
        capital.setMasterOfLawsFemale(false);

        sync(level, previous);
        CapitalDataAccess.markDirty(level);

        return true;
    }

    private static void sync(
            ServerLevel level,
            UUID entityId
    ) {
        Entity entity =
                MCAIntegrationBridge.findLoadedEntityByUuid(
                        level,
                        entityId
                );

        if (entity != null) {
            VillagerIdentitySyncService.syncToNearbyPlayers(
                    level,
                    entity
            );
        }
    }
}