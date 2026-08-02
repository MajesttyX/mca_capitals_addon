package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;
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
        UUID currentAmbassador =
                getAmbassador(
                        level,
                        capital
                );

        boolean changed = false;

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
                sync(level, currentAmbassador);
                changed = true;
            }
        }

        if (getAmbassador(level, capital) == null
                && capital.getState() == CapitalState.ACTIVE
                && capital.getSovereign() != null
                && CapitalBuildingService
                .hasAmbassadorBuildings(
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
                changed = appoint(
                        level,
                        capital,
                        candidate,
                        residents,
                        false
                ) || changed;
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

    public static boolean appointAmbassador(
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
                || capital.getSovereign() == null
                || !CapitalBuildingService
                .hasAmbassadorBuildings(
                        level,
                        capital
                )
                || !CapitalAmbassadorSelection.isEligible(
                        level,
                        capital,
                        villagerId,
                        residents
                )) {
            return false;
        }

        if (villagerId.equals(
                getAmbassador(
                        level,
                        capital
                )
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
            for (UUID ambassadorId :
                    CapitalDiplomacyDataAccess
                    .getAmbassadorsSnapshot(level)
                    .values()) {
                if (entityId.equals(ambassadorId)) {
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

        AMBASSADOR_CACHE.remove(capitalId);

        if (level != null) {
            CapitalDiplomacyDataAccess.clearAmbassador(
                    level,
                    capitalId
            );
        }
    }

    private static boolean appoint(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId,
            Set<UUID> residents,
            boolean recordReplacement
    ) {
        UUID capitalId = capital.getCapitalId();
        UUID previous = getAmbassador(
                level,
                capital
        );

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
                            + " was relieved of the office of Ambassador of "
                            + capitalName
                            + "."
            );
        }

        CapitalDiplomacyDataAccess.setAmbassador(
                level,
                capitalId,
                villagerId
        );

        AMBASSADOR_CACHE.put(
                capitalId,
                villagerId
        );

        CapitalNameService.refreshCapitalNames(
                level,
                capital,
                residents
        );

        CapitalCourtWatcher.clearFingerprint(
                capitalId
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
                        + " was appointed Ambassador of "
                        + capitalName
                        + "."
        );

        return true;
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
