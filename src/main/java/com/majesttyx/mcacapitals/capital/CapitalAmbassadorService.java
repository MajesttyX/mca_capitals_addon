package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

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

                CapitalChronicleService.addEntry(
                        level,
                        capital,
                        name
                                + " was appointed Ambassador of "
                                + capitalName
                                + "."
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
}