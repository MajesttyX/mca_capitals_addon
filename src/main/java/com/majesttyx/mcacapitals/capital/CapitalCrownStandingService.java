package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.identity.VillagerIdentityData;
import com.majesttyx.mcacapitals.identity.VillagerIdentityService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CapitalCrownStandingService {

    private CapitalCrownStandingService() {
    }

    public static boolean tick(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (level == null || capital == null || residents == null) {
            return false;
        }

        UUID sovereign = capital.getSovereign();
        boolean sovereignChanged = !sameUuid(sovereign, capital.getLastCrownStandingSovereign());
        boolean changed = false;

        if (sovereignChanged) {
            capital.clearCrownStandings();
            capital.setLastCrownStandingSovereign(sovereign);
            changed = true;
        }

        Set<UUID> candidates = new HashSet<>(residents);
        addIfPresent(candidates, capital.getSovereign());
        addIfPresent(candidates, capital.getConsort());
        addIfPresent(candidates, capital.getDowager());
        addIfPresent(candidates, capital.getHeir());
        candidates.addAll(capital.getRoyalChildren());
        candidates.addAll(capital.getDisinheritedRoyalChildren());
        candidates.addAll(capital.getLegitimizedRoyalChildren());
        candidates.addAll(capital.getDukes());
        candidates.addAll(capital.getLords());
        candidates.addAll(capital.getKnights());
        candidates.addAll(capital.getRoyalGuards());
        candidates.addAll(capital.getDisgracedRoyalGuards());
        addIfPresent(candidates, capital.getCommander());
        addIfPresent(candidates, capital.getHand());
        addIfPresent(candidates, capital.getHerald());
        addIfPresent(candidates, capital.getGrandMaester());
        addIfPresent(candidates, capital.getMasterOfLaws());

        for (UUID candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            CrownStanding desired = resolveStanding(level, capital, candidate, sovereignChanged);
            if (desired != capital.getCrownStanding(candidate)) {
                capital.setCrownStanding(candidate, desired);
                changed = true;
            }
        }

        Map<UUID, CrownStanding> standings = capital.getCrownStandings();
        for (UUID known : new HashSet<>(standings.keySet())) {
            if (known == null) {
                standings.remove(null);
                changed = true;
            }
        }

        if (changed) {
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static CrownStanding getStanding(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        if (capital == null || villagerId == null) {
            return CrownStanding.FRIEND_OF_CROWN;
        }

        if (villagerId.equals(capital.getSovereign())) {
            return CrownStanding.FRIEND_OF_CROWN;
        }

        CrownStanding standing = capital.getCrownStanding(villagerId);
        if (standing != null) {
            return standing;
        }

        return resolveStanding(level, capital, villagerId, false);
    }

    public static boolean isEnemy(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        return getStanding(level, capital, villagerId) == CrownStanding.ENEMY_OF_CROWN;
    }

    public static boolean isFriend(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        return getStanding(level, capital, villagerId) == CrownStanding.FRIEND_OF_CROWN;
    }

    public static boolean isWillingToDeclareLoyalty(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId
    ) {
        if (!isFriend(level, capital, villagerId)
                || capital == null
                || capital.getCapitalId() == null
                || villagerId == null) {
            return false;
        }

        String seed = capital.getCapitalId()
                + ":"
                + String.valueOf(capital.getSovereign())
                + ":public-loyalty:"
                + villagerId;
        return Math.floorMod(seed.hashCode(), 100) < 25;
    }

    private static CrownStanding resolveStanding(ServerLevel level, CapitalRecord capital, UUID villagerId, boolean sovereignChanged) {
        if (capital == null || villagerId == null) {
            return CrownStanding.FRIEND_OF_CROWN;
        }

        if (villagerId.equals(capital.getSovereign())) {
            return CrownStanding.FRIEND_OF_CROWN;
        }

        if (!sovereignChanged) {
            CrownStanding current = capital.getCrownStanding(villagerId);
            if (current != null) {
                return current;
            }
        }

        int enemyChance = enemyChance(level, capital, villagerId);
        int roll = Math.floorMod((capital.getCapitalId().toString() + ":" + villagerId + ":" + capital.getSovereign()).hashCode(), 100);
        return roll < enemyChance ? CrownStanding.ENEMY_OF_CROWN : CrownStanding.FRIEND_OF_CROWN;
    }

    private static int enemyChance(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        int chance = 18;

        if (capital.isDisinheritedRoyalChild(villagerId)) {
            chance += 48;
        }
        if (capital.isDisgracedRoyalGuard(villagerId)) {
            chance += 42;
        }
        if (capital.isRoyalChild(villagerId) && !villagerId.equals(capital.getHeir())) {
            chance += 16;
        }
        if (villagerId.equals(capital.getDowager()) || capital.isDowagerPrince(villagerId) || capital.isDowagerDuke(villagerId)) {
            chance += 18;
        }
        if (capital.isDuke(villagerId) || capital.isLord(villagerId)) {
            chance += 6;
        }
        if (capital.isRoyalGuard(villagerId)) {
            chance -= 18;
        }
        if (villagerId.equals(capital.getHand()) || villagerId.equals(capital.getCommander()) || villagerId.equals(capital.getGrandMaester()) || villagerId.equals(capital.getHerald()) || villagerId.equals(capital.getMasterOfLaws())) {
            chance -= 18;
        }
        if (villagerId.equals(capital.getConsort()) || villagerId.equals(capital.getHeir())) {
            chance -= 22;
        }

        chance += originWeight(level, capital, villagerId);
        chance += houseWeight(level, capital, villagerId);

        return Math.max(5, Math.min(85, chance));
    }

    private static int originWeight(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        Entity entity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, villagerId);
        if (entity == null) {
            return 0;
        }

        VillagerIdentityData identity = VillagerIdentityService.getIdentity(entity);
        if (identity == null || identity.originCapitalId() == null || capital.getCapitalId() == null) {
            return 0;
        }

        return identity.originCapitalId().equals(capital.getCapitalId()) ? -8 : 14;
    }

    private static int houseWeight(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        UUID sovereign = capital.getSovereign();
        if (sovereign == null) {
            return 0;
        }

        Entity villager = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, villagerId);
        Entity sovereignEntity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, sovereign);
        if (villager == null || sovereignEntity == null) {
            return 0;
        }

        String villagerSurname = normalize(VillagerIdentityService.getCurrentSurname(villager));
        String sovereignSurname = normalize(VillagerIdentityService.getCurrentSurname(sovereignEntity));
        if (villagerSurname.isBlank() || sovereignSurname.isBlank()) {
            return 0;
        }

        return villagerSurname.equals(sovereignSurname) ? -14 : 5;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static void addIfPresent(Set<UUID> values, UUID value) {
        if (value != null) {
            values.add(value);
        }
    }

    private static boolean sameUuid(UUID first, UUID second) {
        if (first == null) {
            return second == null;
        }
        return first.equals(second);
    }
}