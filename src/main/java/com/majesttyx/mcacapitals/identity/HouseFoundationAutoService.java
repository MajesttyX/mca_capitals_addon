package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class HouseFoundationAutoService {

    private HouseFoundationAutoService() {
    }

    public static boolean tickCapital(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (level == null || capital == null) {
            return false;
        }

        boolean changed = false;

        for (UUID candidateId : collectCandidates(capital, residents)) {
            Entity candidate = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, candidateId);
            if (candidate == null) {
                continue;
            }

            VillagerIdentityService.ensureAssigned(level, candidate, capital);

            VillagerIdentityData identity = VillagerIdentityService.getIdentity(candidate);
            if (identity.hasFoundedHouse()) {
                continue;
            }

            String title = CapitalTitleResolver.getDisplayTitleForEntity(level, candidateId);

            if (isRoyalChildTitle(title)) {
                if (inheritRoyalHouseFromParent(level, capital, candidate)) {
                    changed = true;
                }
                continue;
            }

            if (!isFoundingTitle(title)) {
                continue;
            }

            HouseFoundationService.HouseFoundationResult result = HouseFoundationService.foundHouse(level, candidate, capital);
            if (result.success()) {
                changed = true;
            }
        }

        return changed;
    }

    private static Set<UUID> collectCandidates(CapitalRecord capital, Set<UUID> residents) {
        LinkedHashSet<UUID> candidates = new LinkedHashSet<>();

        addIfPresent(candidates, capital.getSovereign());
        candidates.addAll(capital.getDukes());
        candidates.addAll(capital.getLords());
        candidates.addAll(capital.getRoyalChildren());
        candidates.addAll(capital.getLegitimizedRoyalChildren());

        if (residents != null) {
            candidates.addAll(residents);
        }

        return candidates;
    }

    private static void addIfPresent(Set<UUID> candidates, UUID id) {
        if (id != null) {
            candidates.add(id);
        }
    }

    private static boolean isFoundingTitle(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }

        return switch (title) {
            case "King", "Queen",
                 "Duke", "Duchess",
                 "Lord", "Lady" -> true;
            default -> false;
        };
    }

    private static boolean isRoyalChildTitle(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }

        return switch (title) {
            case "Crown Prince", "Crown Princess",
                 "Prince", "Princess" -> true;
            default -> false;
        };
    }

    private static boolean inheritRoyalHouseFromParent(ServerLevel level, CapitalRecord capital, Entity child) {
        if (level == null || capital == null || child == null) {
            return false;
        }

        UUID sourceId = findRoyalHouseSourceId(level, capital, child.getUUID());
        if (sourceId == null) {
            return false;
        }

        Entity source = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, sourceId);
        if (source == null) {
            return false;
        }

        VillagerIdentityService.ensureAssigned(level, source, capital);
        VillagerIdentityData sourceIdentity = VillagerIdentityService.getIdentity(source);

        if (!sourceIdentity.hasFoundedHouse()) {
            return false;
        }

        String houseName = sourceIdentity.houseName();
        if (houseName == null || houseName.isBlank()) {
            return false;
        }

        VillagerIdentityService.assignBirthSurname(level, child, houseName, SurnameSource.BIRTH);
        VillagerIdentityService.assignCurrentSurname(level, child, houseName, SurnameSource.BIRTH);
        VillagerIdentityService.foundHouse(
                level,
                child,
                houseName,
                sourceIdentity.houseWords(),
                sourceIdentity.houseWordsPersonality(),
                sourceIdentity.houseFounderId(),
                sourceIdentity.houseFounderName(),
                sourceIdentity.houseFoundedInCapitalId(),
                sourceIdentity.houseFoundedInCapitalName()
        );

        VillagerIdentitySyncService.syncToNearbyPlayers(level, child);
        return true;
    }

    private static UUID findRoyalHouseSourceId(ServerLevel level, CapitalRecord capital, UUID childId) {
        if (level == null || capital == null || childId == null) {
            return null;
        }

        UUID sovereign = capital.getSovereign();
        if (sovereign != null && MCAIntegrationBridge.isChildOf(level, childId, sovereign)) {
            return sovereign;
        }

        UUID consort = capital.getConsort();
        if (consort != null && MCAIntegrationBridge.isChildOf(level, childId, consort)) {
            return consort;
        }

        for (UUID parentId : MCAIntegrationBridge.getParents(level, childId)) {
            if (parentId == null) {
                continue;
            }

            Entity parent = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, parentId);
            if (parent == null) {
                continue;
            }

            VillagerIdentityData parentIdentity = VillagerIdentityService.getIdentity(parent);
            if (parentIdentity.hasFoundedHouse()) {
                return parentId;
            }
        }

        return null;
    }
}