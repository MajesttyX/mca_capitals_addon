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

        boolean changed = false;
        UUID current = capital.getMasterOfLaws();

        if (current != null) {
            if (isExistingHolderValid(
                    level,
                    capital,
                    residents,
                    current
            )) {
                Entity loaded =
                        MCAIntegrationBridge.findLoadedEntityByUuid(
                                level,
                                current
                        );

                if (loaded != null) {
                    boolean female =
                            MCAIntegrationBridge.isFemale(
                                    level,
                                    current
                            );

                    if (capital.isMasterOfLawsFemale() != female) {
                        capital.setMasterOfLawsFemale(female);
                        CapitalDataAccess.markDirty(level);
                        return true;
                    }
                }

                return false;
            }

            changed = clearMasterOfLaws(
                    level,
                    capital
            );
        }

        if (!CapitalBuildingService.hasPrison(
                level,
                capital
        )) {
            return changed;
        }

        UUID selected =
                CapitalMasterOfLawsSelection.select(
                        level,
                        capital,
                        residents
                );

        if (selected == null) {
            return changed;
        }

        return appoint(
                level,
                capital,
                selected,
                residents,
                false
        ) || changed;
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

    public static boolean appointMasterOfLaws(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || villagerId == null
                || residents == null
                || capital.getState() != CapitalState.ACTIVE
                || !CapitalBuildingService.hasPrison(
                        level,
                        capital
                )
                || !CapitalMasterOfLawsSelection.isEligible(
                        level,
                        capital,
                        residents,
                        villagerId
                )) {
            return false;
        }

        if (villagerId.equals(
                capital.getMasterOfLaws()
        )) {
            return false;
        }

        return appoint(
                level,
                capital,
                villagerId,
                residents,
                true
        );
    }

    private static boolean isExistingHolderValid(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents,
            UUID holderId
    ) {
        if (holderId == null
                || !CapitalRoleValidation
                .isExistingRoleStillResolvable(
                        level,
                        holderId,
                        residents
                )
                || !CapitalCrownJusticeService
                .isTrustedOfficeEligible(
                        level,
                        capital,
                        holderId
                )
                || hasConflictingOffice(
                        level,
                        capital,
                        holderId
                )
                || CapitalRoleValidation
                .isLoadedDeadOrRemoved(
                        level,
                        holderId
                )
                || MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        holderId
                )) {
            return false;
        }

        Entity loaded =
                MCAIntegrationBridge.findLoadedEntityByUuid(
                        level,
                        holderId
                );

        if (loaded != null
                && !MCAIntegrationBridge
                .isAliveMCAVillagerEntity(
                        loaded
                )) {
            return false;
        }

        Integer holderVillageId =
                MCAIntegrationBridge
                .getVillageIdForResident(
                        level,
                        holderId
                );

        return holderVillageId == null
                || capital.getVillageId() == null
                || holderVillageId.equals(
                        capital.getVillageId()
                );
    }

    private static boolean hasConflictingOffice(
            ServerLevel level,
            CapitalRecord capital,
            UUID holderId
    ) {
        return holderId.equals(capital.getSovereign())
                || holderId.equals(capital.getConsort())
                || holderId.equals(capital.getDowager())
                || holderId.equals(capital.getHeir())
                || holderId.equals(capital.getHand())
                || holderId.equals(capital.getCommander())
                || holderId.equals(capital.getHerald())
                || holderId.equals(capital.getGrandMaester())
                || capital.isRoyalGuard(holderId)
                || CapitalAmbassadorService.isAmbassador(
                        level,
                        holderId
                );
    }

    private static boolean appoint(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId,
            Set<UUID> residents,
            boolean recordReplacement
    ) {
        UUID previous = capital.getMasterOfLaws();

        if (villagerId.equals(previous)) {
            return false;
        }

        String capitalName =
                MCAIntegrationBridge.getVillageName(
                        level,
                        capital.getVillageId()
                );

        if (recordReplacement
                && previous != null) {
            String previousName =
                    CapitalNameService.resolveDisplayName(
                            level,
                            capital,
                            previous
                    );

            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    previousName
                            + " was relieved of the office of Master of Laws of "
                            + capitalName
                            + "."
            );
        }

        capital.setMasterOfLaws(villagerId);
        capital.setMasterOfLawsFemale(
                MCAIntegrationBridge.isFemale(
                        level,
                        villagerId
                )
        );

        refreshPresentation(
                level,
                capital,
                residents
        );

        sync(level, previous);
        sync(level, villagerId);

        CapitalDataAccess.markDirty(level);

        String name =
                CapitalNameService.resolveDisplayName(
                        level,
                        capital,
                        villagerId
                );

        CapitalChronicleService.addEntry(
                level,
                capital,
                name
                        + " was appointed Master of Laws of "
                        + capitalName
                        + "."
        );

        return true;
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

        CapitalCourtWatcher.clearFingerprint(
                capital.getCapitalId()
        );

        CapitalDataAccess.markDirty(level);
        return true;
    }

    private static void refreshPresentation(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        CapitalHeraldService
                .refreshHeraldAfterStatusChange(
                        level,
                        capital,
                        residents
                );

        CapitalNameService.refreshCapitalNames(
                level,
                capital,
                residents
        );

        CapitalCourtWatcher.clearFingerprint(
                capital.getCapitalId()
        );
    }

    private static void sync(
            ServerLevel level,
            UUID entityId
    ) {
        if (entityId == null) {
            return;
        }

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
