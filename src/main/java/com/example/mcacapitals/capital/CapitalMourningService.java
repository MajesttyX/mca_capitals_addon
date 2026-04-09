package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalMourningService {

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
            UUID villagerId = entry.getKey();
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, villagerId);
            if (!MCAIntegrationBridge.isAliveMCAVillagerEntity(entity)) {
                continue;
            }

            String original = entry.getValue();
            if (original == null || original.isBlank()) {
                MCAIntegrationBridge.randomizeClothes(level, villagerId);
                continue;
            }

            String beforeRestore = MCAIntegrationBridge.getClothes(level, villagerId);
            MCAIntegrationBridge.setClothes(level, villagerId, original);
            String afterRestore = MCAIntegrationBridge.getClothes(level, villagerId);

            if (sameClothingId(original, afterRestore)) {
                continue;
            }

            if (!sameClothingId(beforeRestore, afterRestore) && afterRestore != null && !afterRestore.isBlank()) {
                continue;
            }

            if (MCAIntegrationBridge.clothingExists(original)) {
                MCAIntegrationBridge.setClothes(level, villagerId, original);
                afterRestore = MCAIntegrationBridge.getClothes(level, villagerId);
                if (sameClothingId(original, afterRestore)) {
                    continue;
                }
            }

            MCAIntegrationBridge.randomizeClothes(level, villagerId);
        }

        originals.clear();
        capital.setMourningActive(false);
        capital.setMourningEndDay(0L);

        CapitalChronicleService.addEntry(level, capital,
                "The mourning period in " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + " came to an end.");

        CapitalDataAccess.markDirty(level);
    }

    private static void applyMourning(ServerLevel level, CapitalRecord capital) {
        Set<UUID> targets = new java.util.LinkedHashSet<>(CapitalResidentScanner.scanResidents(level, capital.getCapitalId()));

        if (capital.getSovereign() != null) {
            targets.add(capital.getSovereign());
        }
        if (capital.getConsort() != null) {
            targets.add(capital.getConsort());
        }
        if (capital.getDowager() != null) {
            targets.add(capital.getDowager());
        }
        if (capital.getHeir() != null) {
            targets.add(capital.getHeir());
        }

        targets.addAll(capital.getRoyalChildren());
        targets.addAll(capital.getDukes());
        targets.addAll(capital.getLords());
        targets.addAll(capital.getKnights());

        for (UUID residentId : targets) {
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, residentId);
            if (!MCAIntegrationBridge.isAliveMCAVillagerEntity(entity)) {
                continue;
            }

            boolean explicitRoyalTarget =
                    residentId.equals(capital.getSovereign())
                            || residentId.equals(capital.getConsort())
                            || residentId.equals(capital.getDowager())
                            || residentId.equals(capital.getHeir())
                            || capital.isRoyalChild(residentId);

            if (!explicitRoyalTarget && !MCAIntegrationBridge.isTeenOrAdultVillager(level, residentId)) {
                continue;
            }

            String targetClothes = pickMourningClothes(level, capital, residentId);
            if (targetClothes == null || !MCAIntegrationBridge.clothingExists(targetClothes)) {
                continue;
            }

            capital.getMourningOriginalClothes().putIfAbsent(residentId, MCAIntegrationBridge.getClothes(level, residentId));

            String currentClothes = MCAIntegrationBridge.getClothes(level, residentId);
            if (targetClothes.equals(currentClothes)) {
                continue;
            }

            MCAIntegrationBridge.setClothes(level, residentId, targetClothes);
        }
    }

    private static String pickMourningClothes(ServerLevel level, CapitalRecord capital, UUID villagerId) {
        boolean female = MCAIntegrationBridge.isFemale(level, villagerId);

        if (isRoyalTier(capital, villagerId)) {
            return buildPath(
                    female ? MourningClothingPaths.FEMALE_ROYAL_PREFIX : MourningClothingPaths.MALE_ROYAL_PREFIX,
                    female ? MourningClothingPaths.FEMALE_ROYAL_COUNT : MourningClothingPaths.MALE_ROYAL_COUNT,
                    villagerId
            );
        }

        if (isNobleTier(capital, villagerId)) {
            return buildPath(
                    female ? MourningClothingPaths.FEMALE_NOBLE_PREFIX : MourningClothingPaths.MALE_NOBLE_PREFIX,
                    female ? MourningClothingPaths.FEMALE_NOBLE_COUNT : MourningClothingPaths.MALE_NOBLE_COUNT,
                    villagerId
            );
        }

        return buildPath(
                female ? MourningClothingPaths.FEMALE_COMMONER_PREFIX : MourningClothingPaths.MALE_COMMONER_PREFIX,
                female ? MourningClothingPaths.FEMALE_COMMONER_COUNT : MourningClothingPaths.MALE_COMMONER_COUNT,
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

    private static boolean sameClothingId(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.equals(b)) {
            return true;
        }

        return normalizeClothingId(a).equals(normalizeClothingId(b));
    }

    private static String normalizeClothingId(String value) {
        String normalized = value.trim().replace('\\', '/');
        if (normalized.endsWith(".png")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }
}