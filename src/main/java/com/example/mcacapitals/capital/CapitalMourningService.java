package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.mca.entity.VillagerEntityMCA;
import net.mca.resources.ClothingList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalMourningService {

    private static final String FEMALE_ROYAL_PREFIX = "examplemod:skins/clothing/normal/female/mourning_royal/";
    private static final String MALE_ROYAL_PREFIX = "examplemod:skins/clothing/normal/male/mourning_royal/";
    private static final String FEMALE_NOBLE_PREFIX = "examplemod:skins/clothing/normal/female/mourning_noble/";
    private static final String MALE_NOBLE_PREFIX = "examplemod:skins/clothing/normal/male/mourning_noble/";
    private static final String FEMALE_COMMONER_PREFIX = "examplemod:skins/clothing/normal/female/mourning_commoner/";
    private static final String MALE_COMMONER_PREFIX = "examplemod:skins/clothing/normal/male/mourning_commoner/";

    private static final int FEMALE_ROYAL_COUNT = 4;
    private static final int MALE_ROYAL_COUNT = 3;
    private static final int FEMALE_NOBLE_COUNT = 3;
    private static final int MALE_NOBLE_COUNT = 2;
    private static final int FEMALE_COMMONER_COUNT = 2;
    private static final int MALE_COMMONER_COUNT = 2;

    private CapitalMourningService() {
    }

    public static void startMourning(ServerLevel level, CapitalRecord capital, String reason) {
        if (level == null || capital == null) {
            return;
        }

        long currentDay = level.getDayTime() / 24000L;
        long endDay = currentDay + 2L;
        boolean wasActive = capital.isMourningActive();

        capital.setMourningActive(true);
        capital.setMourningEndDay(Math.max(capital.getMourningEndDay(), endDay));

        if (!wasActive) {
            CapitalChronicleService.addEntry(level, capital,
                    "Mourning was declared in " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
                            + " for two days. " + reason);
        }

        applyMourning(level, capital);
        CapitalDataAccess.markDirty(level);
    }

    public static void tickMourning(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || !capital.isMourningActive()) {
            return;
        }

        long currentDay = level.getDayTime() / 24000L;
        if (currentDay >= capital.getMourningEndDay()) {
            endMourning(level, capital);
            return;
        }

        applyMourning(level, capital);
    }

    public static void endMourning(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null) {
            return;
        }

        Map<UUID, String> originals = capital.getMourningOriginalClothes();
        for (Map.Entry<UUID, String> entry : originals.entrySet()) {
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entry.getKey());
            if (!(entity instanceof VillagerEntityMCA villager)) {
                continue;
            }

            String original = entry.getValue();
            if (original == null || original.isBlank()) {
                villager.randomizeClothes();
            } else if (ClothingList.getInstance().clothing.containsKey(original)) {
                villager.setClothes(original);
            } else {
                villager.randomizeClothes();
            }
        }

        originals.clear();
        capital.setMourningActive(false);
        capital.setMourningEndDay(0L);

        CapitalChronicleService.addEntry(level, capital,
                "The mourning period in " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + " came to an end.");

        CapitalDataAccess.markDirty(level);
    }

    private static void applyMourning(ServerLevel level, CapitalRecord capital) {
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        for (UUID residentId : residents) {
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, residentId);
            if (!(entity instanceof VillagerEntityMCA villager)) {
                continue;
            }

            if (!MCAIntegrationBridge.isTeenOrAdultVillager(level, residentId)) {
                continue;
            }

            String targetClothes = pickMourningClothes(level, capital, residentId);
            if (targetClothes == null) {
                continue;
            }

            if (!ClothingList.getInstance().clothing.containsKey(targetClothes)) {
                continue;
            }

            capital.getMourningOriginalClothes().putIfAbsent(residentId, villager.getClothes());

            if (targetClothes.equals(villager.getClothes())) {
                continue;
            }

            villager.setClothes(targetClothes);
        }
    }

    private static String pickMourningClothes(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        boolean female = MCAIntegrationBridge.isFemale(level, villagerId);

        if (isRoyalTier(capital, villagerId)) {
            return buildPath(
                    female ? FEMALE_ROYAL_PREFIX : MALE_ROYAL_PREFIX,
                    female ? FEMALE_ROYAL_COUNT : MALE_ROYAL_COUNT,
                    villagerId
            );
        }

        if (isNobleTier(capital, villagerId)) {
            return buildPath(
                    female ? FEMALE_NOBLE_PREFIX : MALE_NOBLE_PREFIX,
                    female ? FEMALE_NOBLE_COUNT : MALE_NOBLE_COUNT,
                    villagerId
            );
        }

        return buildPath(
                female ? FEMALE_COMMONER_PREFIX : MALE_COMMONER_PREFIX,
                female ? FEMALE_COMMONER_COUNT : MALE_COMMONER_COUNT,
                villagerId
        );
    }

    private static boolean isRoyalTier(CapitalRecord capital, UUID villagerId) {
        return villagerId != null && (
                villagerId.equals(capital.getSovereign())
                        || villagerId.equals(capital.getConsort())
                        || villagerId.equals(capital.getDowager())
                        || villagerId.equals(capital.getHeir())
                        || capital.isRoyalChild(villagerId)
        );
    }

    private static boolean isNobleTier(CapitalRecord capital, UUID villagerId) {
        return villagerId != null && (
                capital.isDuke(villagerId)
                        || capital.isLord(villagerId)
        );
    }

    private static String buildPath(String prefix, int count, UUID villagerId) {
        int variant = Math.floorMod(villagerId.hashCode(), count);
        return prefix + variant + ".png";
    }
}