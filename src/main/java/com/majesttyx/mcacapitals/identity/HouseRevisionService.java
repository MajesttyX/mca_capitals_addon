package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.data.HouseRevisionDataAccess;
import com.majesttyx.mcacapitals.data.HouseRevisionSavedData;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class HouseRevisionService {

    private HouseRevisionService() {
    }

    public static void recordRevision(
            ServerLevel level,
            VillagerIdentityData identity,
            String houseWords
    ) {
        String key = houseKey(identity);
        if (key.isBlank()) {
            return;
        }

        HouseRevisionSavedData data = HouseRevisionDataAccess.get(level);
        if (data != null) {
            data.setHouseWords(key, normalizeWords(houseWords));
        }
    }

    public static boolean applyLatestRevision(ServerLevel level, Entity entity) {
        if (level == null
                || entity == null
                || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return false;
        }

        VillagerIdentityData identity = VillagerIdentityService.getIdentity(entity);
        if (identity == null || !identity.hasFoundedHouse()) {
            return false;
        }

        String key = houseKey(identity);
        if (key.isBlank()) {
            return false;
        }

        HouseRevisionSavedData data = HouseRevisionDataAccess.get(level);
        if (data == null) {
            return false;
        }

        String latestWords = data.getHouseWords(key);
        if (latestWords == null) {
            return false;
        }

        String currentWords = normalizeWords(identity.houseWords());
        latestWords = normalizeWords(latestWords);
        if (currentWords.equals(latestWords)) {
            return false;
        }

        VillagerIdentityService.clearHouse(entity);
        boolean changed = VillagerIdentityService.foundHouse(
                level,
                entity,
                identity.houseName(),
                latestWords,
                identity.houseWordsPersonality(),
                identity.houseFounderId(),
                identity.houseFounderName(),
                identity.houseFoundedInCapitalId(),
                identity.houseFoundedInCapitalName()
        );

        if (changed) {
            VillagerIdentitySyncService.syncToNearbyPlayers(level, entity);
        }

        return changed;
    }

    public static int applyToResidents(ServerLevel level, Set<UUID> residents) {
        if (level == null || residents == null || residents.isEmpty()) {
            return 0;
        }

        int changed = 0;
        for (UUID residentId : residents) {
            Entity entity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, residentId);
            if (applyLatestRevision(level, entity)) {
                changed++;
            }
        }
        return changed;
    }

    private static String houseKey(VillagerIdentityData identity) {
        if (identity == null || !identity.hasFoundedHouse()) {
            return "";
        }

        String houseName = identity.houseName();
        if (houseName == null || houseName.isBlank()) {
            houseName = identity.currentSurname();
        }

        if (houseName == null || houseName.isBlank()) {
            return "";
        }

        return houseName
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static String normalizeWords(String words) {
        return words == null
                ? ""
                : words.trim().replaceAll("\\s+", " ");
    }
}
