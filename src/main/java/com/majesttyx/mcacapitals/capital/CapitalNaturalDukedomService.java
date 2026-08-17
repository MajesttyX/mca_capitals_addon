package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalPublicCrownStatus;
import com.majesttyx.mcacapitals.util.CapitalJusticeText;
import com.majesttyx.mcacapitals.util.MCAExecutionBridge;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CapitalNaturalDukedomService {

    private static final int BIG_HOUSES_PER_DUKE = 2;
    private static final int DAILY_CHANCE = 20;

    private CapitalNaturalDukedomService() {
    }

    public static boolean tick(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || residents == null
                || residents.isEmpty()
                || capital.getState() != CapitalState.ACTIVE) {
            return false;
        }

        long currentDay = Math.max(
                1L,
                level.getDayTime() / 24000L + 1L
        );

        if (capital.getLastNaturalDukedomDay() == currentDay) {
            return false;
        }

        capital.setLastNaturalDukedomDay(currentDay);

        int bigHouses =
                CapitalBuildingService.countBigHouses(level, capital);

        int allowedNaturalDukes =
                bigHouses / BIG_HOUSES_PER_DUKE;

        if (allowedNaturalDukes <= 0) {
            CapitalDataAccess.markDirty(level);
            return true;
        }

        int currentNaturalDukes =
                countNaturalDukes(capital, residents);

        if (currentNaturalDukes >= allowedNaturalDukes) {
            CapitalDataAccess.markDirty(level);
            return true;
        }

        if (level.random.nextInt(100) >= DAILY_CHANCE) {
            CapitalDataAccess.markDirty(level);
            return true;
        }

        UUID candidate =
                selectCandidate(level, capital, residents);

        if (candidate == null) {
            CapitalDataAccess.markDirty(level);
            return true;
        }

        boolean female =
                MCAIntegrationBridge.isFemale(level, candidate);

        capital.addDuke(candidate, female);

        String name =
                CapitalNameService.resolveDisplayName(
                        level,
                        capital,
                        candidate
                );

        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.NATURAL_DUKEDOM,
                name,
                CapitalChronicleIdentitySnapshot.title(level, capital, candidate),
                CapitalChronicleIdentitySnapshot.style(level, capital, candidate)
        );

        CapitalDataAccess.markDirty(level);
        return true;
    }

    private static int countNaturalDukes(
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        int count = 0;

        for (UUID duke : capital.getDukes()) {
            if (duke != null && residents.contains(duke)) {
                count++;
            }
        }

        return count;
    }

    private static UUID selectCandidate(
            ServerLevel level,
            CapitalRecord capital,
            Set<UUID> residents
    ) {
        Set<UUID> blocked = new HashSet<>();

        addIfPresent(blocked, capital.getSovereign());
        addIfPresent(blocked, capital.getConsort());
        addIfPresent(blocked, capital.getDowager());
        addIfPresent(blocked, capital.getHeir());
        addIfPresent(blocked, capital.getHand());
        addIfPresent(blocked, capital.getCommander());
        addIfPresent(blocked, capital.getHerald());
        addIfPresent(blocked, capital.getGrandMaester());
        addIfPresent(blocked, capital.getMasterOfLaws());

        addIfPresent(
                blocked,
                CapitalAmbassadorService.getAmbassador(level, capital)
        );

        blocked.addAll(capital.getRoyalChildren());
        blocked.addAll(capital.getDisinheritedRoyalChildren());
        blocked.addAll(capital.getLegitimizedRoyalChildren());
        blocked.addAll(capital.getDukes());
        blocked.addAll(capital.getRoyalGuards());
        blocked.addAll(capital.getDisgracedRoyalGuards());

        List<UUID> candidates = new ArrayList<>();

        for (UUID resident : residents) {
            if (resident == null
                    || blocked.contains(resident)
                    || CapitalAmbassadorService.isAmbassador(level, resident)) {
                continue;
            }

            int weight = CapitalCrownJusticeService.naturalElevationWeight(
                    level,
                    capital,
                    resident
            );

            if (weight <= 0) {
                continue;
            }

            if (!MCAIntegrationBridge.isTeenOrAdultVillager(level, resident)) {
                continue;
            }

            if (!MCAIntegrationBridge.isAliveMCAVillager(level, resident)) {
                continue;
            }

            for (int index = 0; index < weight; index++) {
                candidates.add(resident);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator.comparing(UUID::toString));

        return candidates.get(
                level.random.nextInt(candidates.size())
        );
    }

    private static boolean isRecognizedFriend(ServerLevel level, CapitalRecord capital, UUID entityId) {
        return level != null
                && capital != null
                && capital.getCapitalId() != null
                && entityId != null
                && CapitalJusticeDataAccess.getPublicStatus(level, capital.getCapitalId(), entityId)
                == CapitalPublicCrownStatus.RECOGNIZED_FRIEND;
    }

    private static boolean isTrustedOfficeEligible(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || capital.getCapitalId() == null || entityId == null) {
            return false;
        }

        UUID capitalId = capital.getCapitalId();

        return CapitalJusticeDataAccess.getPublicStatus(level, capitalId, entityId)
                != CapitalPublicCrownStatus.DISCOVERED_ENEMY
                && !CapitalJusticeDataAccess.hasArrestWarrant(level, capitalId, entityId)
                && !CapitalJusticeDataAccess.isDetainedPrisoner(level, capitalId, entityId)
                && !MCAExecutionBridge.isMarkedForExecution(level, entityId);
    }

    private static UUID getAmbassador(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getCapitalId() == null) {
            return null;
        }

        return CapitalDiplomacyDataAccess.getAmbassador(level, capital.getCapitalId());
    }

    private static boolean isAmbassador(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return false;
        }

        return CapitalDiplomacyDataAccess.getAmbassadorsSnapshot(level)
                .containsValue(entityId);
    }

    private static void addIfPresent(Set<UUID> values, UUID value) {
        if (value != null) {
            values.add(value);
        }
    }
}
