package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CapitalAmbassadorService {
    private static final Map<UUID, UUID> AMBASSADOR_CACHE =
            new LinkedHashMap<>();

    private CapitalAmbassadorService() {
    }

    public static boolean tickAmbassador(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || residents == null) {
            return false;
        }

        UUID capitalId = capital.getCapitalId();
        boolean changed = reconcileCapitalAssignments(
                level,
                capital,
                residents
        );

        UUID currentAmbassador =
                getAmbassador(
                        level,
                        capital
                );

        if (!CapitalAmbassadorSelection.isValid(
                level,
                capital,
                currentAmbassador,
                residents
        )) {
            if (currentAmbassador != null) {
                CapitalDiplomacyDataAccess.clearAmbassador(
                        level,
                        capitalId
                );
                AMBASSADOR_CACHE.remove(capitalId);
                sync(
                        level,
                        currentAmbassador
                );
                changed = true;
            }
        }

        if (getAmbassador(level, capital) == null
                && capital.getState() == CapitalState.ACTIVE
                && capital.getSovereign() != null
                && CapitalBuildingService.hasAmbassadorBuildings(
                level,
                capital
        )) {
            UUID candidate =
                    CapitalAmbassadorSelection.findCandidate(
                            level,
                            capital,
                            residents
                    );

            if (candidate != null) {
                CapitalDiplomacyDataAccess.setAmbassador(
                        level,
                        capitalId,
                        candidate
                );

                AMBASSADOR_CACHE.put(
                        capitalId,
                        candidate
                );

                sync(
                        level,
                        candidate
                );

                String name =
                        CapitalNameService.resolveDisplayName(
                                level,
                                capital,
                                candidate
                        );

                String capitalName =
                        MCAIntegrationBridge.getVillageName(
                                level,
                                capital.getVillageId()
                        );

                CapitalChronicleService.addEvent(
                        level,
                        capital,
                        CapitalChronicleEventId.AMBASSADOR_APPOINTED,
                        name,
                        capitalName
                );

                changed = true;
            }
        }

        if (changed) {
            CapitalNameService.refreshCapitalNames(
                    level,
                    capital,
                    residents
            );

            CapitalCourtWatcher.clearFingerprint(
                    capitalId
            );
        }

        return changed;
    }

    public static boolean isEligibleCandidate(
            ServerLevel level,
            CapitalRecord capital,
            UUID candidateId,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || residents == null) {
            return false;
        }

        reconcileCapitalAssignments(
                level,
                capital,
                residents
        );

        return CapitalAmbassadorSelection.isEligible(
                level,
                capital,
                candidateId,
                residents
        );
    }

    public static boolean appointAmbassador(
            ServerLevel level,
            CapitalRecord capital,
            UUID candidateId,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || candidateId == null
                || residents == null
                || !CapitalBuildingService.hasAmbassadorBuildings(
                level,
                capital
        )) {
            return false;
        }

        reconcileCapitalAssignments(
                level,
                capital,
                residents
        );

        if (!isEligibleCandidate(
                level,
                capital,
                candidateId,
                residents
        )
                || candidateId.equals(
                getAmbassador(
                        level,
                        capital
                )
        )) {
            return false;
        }

        UUID previous =
                getAmbassador(
                        level,
                        capital
                );

        String capitalName =
                MCAIntegrationBridge.getVillageName(
                        level,
                        capital.getVillageId()
                );

        if (previous != null
                && !previous.equals(candidateId)) {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.AMBASSADOR_RELIEVED,
                    CapitalNameService.resolveDisplayName(level, capital, previous),
                    capitalName
            );
        }

        CapitalDiplomacyDataAccess.setAmbassador(
                level,
                capital.getCapitalId(),
                candidateId
        );

        AMBASSADOR_CACHE.put(
                capital.getCapitalId(),
                candidateId
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

        CapitalNameService.refreshCapitalNames(
                level,
                capital,
                residents
        );

        CapitalCourtWatcher.clearFingerprint(
                capital.getCapitalId()
        );

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.AMBASSADOR_APPOINTED,
                CapitalNameService.resolveDisplayName(level, capital, candidateId),
                capitalName
        );

        return true;
    }

    public static UUID getAmbassador(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null) {
            return null;
        }

        UUID ambassador =
                CapitalDiplomacyDataAccess.getAmbassador(
                        level,
                        capital.getCapitalId()
                );

        if (ambassador == null) {
            AMBASSADOR_CACHE.remove(
                    capital.getCapitalId()
            );
        } else {
            AMBASSADOR_CACHE.put(
                    capital.getCapitalId(),
                    ambassador
            );
        }

        return ambassador;
    }

    public static UUID getCachedAmbassador(
            CapitalRecord capital
    ) {
        if (capital == null
                || capital.getCapitalId() == null) {
            return null;
        }

        return AMBASSADOR_CACHE.get(
                capital.getCapitalId()
        );
    }

    public static boolean isAmbassador(
            ServerLevel level,
            UUID entityId
    ) {
        if (entityId == null) {
            return false;
        }

        if (level != null) {
            for (Map.Entry<UUID, UUID> entry :
                    CapitalDiplomacyDataAccess
                            .getAmbassadorsSnapshot(level)
                            .entrySet()) {
                if (!entityId.equals(entry.getValue())) {
                    continue;
                }

                CapitalRecord capital =
                        CapitalManager.getCapital(
                                entry.getKey()
                        );

                if (capital != null) {
                    return true;
                }
            }

            return false;
        }

        return AMBASSADOR_CACHE.containsValue(
                entityId
        );
    }

    public static boolean isAmbassador(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId
    ) {
        if (entityId == null) {
            return false;
        }

        UUID ambassador =
                level == null
                        ? getCachedAmbassador(capital)
                        : getAmbassador(
                        level,
                        capital
                );

        return entityId.equals(ambassador);
    }

    public static void clearCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        if (capitalId == null) {
            return;
        }

        UUID previous =
                level == null
                        ? AMBASSADOR_CACHE.get(capitalId)
                        : CapitalDiplomacyDataAccess.getAmbassador(
                        level,
                        capitalId
                );

        AMBASSADOR_CACHE.remove(capitalId);

        if (level != null) {
            CapitalDiplomacyDataAccess.clearAmbassador(
                    level,
                    capitalId
            );

            sync(
                    level,
                    previous
            );
        }
    }

    private static boolean reconcileCapitalAssignments(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null
                || residents == null) {
            return false;
        }

        UUID capitalId = capital.getCapitalId();
        Integer villageId = capital.getVillageId();
        boolean changed = false;

        Map<UUID, UUID> ambassadors =
                CapitalDiplomacyDataAccess
                        .getAmbassadorsSnapshot(level);

        for (Map.Entry<UUID, UUID> entry :
                ambassadors.entrySet()) {
            UUID assignedCapitalId = entry.getKey();
            UUID ambassadorId = entry.getValue();

            if (assignedCapitalId == null
                    || ambassadorId == null
                    || capitalId.equals(assignedCapitalId)) {
                continue;
            }

            CapitalRecord assignedCapital =
                    CapitalManager.getCapital(
                            assignedCapitalId
                    );

            boolean sameCapitalVillage =
                    assignedCapital != null
                            && Objects.equals(
                            villageId,
                            assignedCapital.getVillageId()
                    );

            boolean holderIsCurrentResident =
                    residents.contains(
                            ambassadorId
                    );

            if (!sameCapitalVillage
                    && !holderIsCurrentResident) {
                continue;
            }

            if (CapitalDiplomacyDataAccess.clearAmbassador(
                    level,
                    assignedCapitalId
            )) {
                changed = true;
            }

            AMBASSADOR_CACHE.remove(
                    assignedCapitalId
            );

            sync(
                    level,
                    ambassadorId
            );
        }

        return changed;
    }

    private static void sync(
            ServerLevel level,
            UUID entityId
    ) {
        if (level == null
                || entityId == null) {
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

    public static void clearRuntimeCache() {
        AMBASSADOR_CACHE.clear();
    }

}
