package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.UsedHouseWordsSavedData;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.UUID;

public final class HouseFoundationService {

    private HouseFoundationService() {
    }

    public static HouseFoundationResult foundHouse(ServerLevel level, Entity founder) {
        if (level == null || founder == null || !MCAIntegrationBridge.isMCAVillagerEntity(founder)) {
            return HouseFoundationResult.fail("No valid MCA villager founder.");
        }

        CapitalRecord capital = CapitalTitleResolverAccess.findCapital(level, founder.getUUID());
        return foundHouse(level, founder, capital);
    }

    public static HouseFoundationResult foundHouse(ServerLevel level, Entity founder, CapitalRecord capital) {
        if (level == null || founder == null || !MCAIntegrationBridge.isMCAVillagerEntity(founder)) {
            return HouseFoundationResult.fail("No valid MCA villager founder.");
        }

        VillagerIdentityService.ensureAssigned(level, founder);

        VillagerIdentityData existing = VillagerIdentityService.getIdentity(founder);
        if (existing.hasFoundedHouse()) {
            return HouseFoundationResult.fail("This villager already founded or belongs to House " + existing.houseName() + ".");
        }

        String houseName = existing.currentSurname();
        if (houseName == null || houseName.isBlank()) {
            houseName = existing.birthSurname();
        }

        if (houseName == null || houseName.isBlank()) {
            return HouseFoundationResult.fail("Founder has no surname to elevate into a House.");
        }

        CapitalRecord resolvedCapital = capital;
        if (resolvedCapital == null && existing.originCapitalId() != null) {
            resolvedCapital = CapitalManager.getCapital(existing.originCapitalId());
        }

        UUID capitalId = resolvedCapital == null ? existing.originCapitalId() : resolvedCapital.getCapitalId();
        String capitalName = resolvedCapital == null
                ? existing.originCapitalName()
                : MCAIntegrationBridge.getVillageName(level, resolvedCapital.getVillageId());

        String founderPersonality = resolveFounderPersonality(founder);
        HouseWordsPool.Selection selection = HouseWordsPool.select(level, founderPersonality, capitalId);

        String founderName = founder.getName() == null ? "" : founder.getName().getString();

        boolean founded = VillagerIdentityService.foundHouse(
                level,
                founder,
                houseName,
                selection.phrase(),
                selection.bucket(),
                founder.getUUID(),
                founderName,
                capitalId,
                capitalName
        );

        if (!founded) {
            return HouseFoundationResult.fail("Could not found House.");
        }

        if (selection.hasWords()) {
            UsedHouseWordsSavedData.get(level).markUsed(
                    selection.phrase(),
                    selection.bucket(),
                    houseName,
                    founder.getUUID(),
                    founderName,
                    capitalId,
                    capitalName,
                    level.getGameTime()
            );
        }

        applyFoundedHouseToImmediateFamily(
                level,
                founder,
                houseName,
                selection.phrase(),
                selection.bucket(),
                founder.getUUID(),
                founderName,
                capitalId,
                capitalName
        );

        return new HouseFoundationResult(
                true,
                "Founded House " + houseName + (selection.hasWords() ? " — " + selection.phrase() : "."),
                houseName,
                selection.phrase(),
                selection.bucket()
        );
    }

    public static void clearHouse(Entity entity) {
        VillagerIdentityService.clearHouse(entity);
    }

    private static void applyFoundedHouseToImmediateFamily(
            ServerLevel level,
            Entity founder,
            String houseName,
            String houseWords,
            String houseWordsPersonality,
            UUID founderId,
            String founderName,
            UUID capitalId,
            String capitalName
    ) {
        if (level == null || founder == null || founderId == null || houseName == null || houseName.isBlank()) {
            return;
        }

        UUID spouseId = MCAIntegrationBridge.getSpouse(level, founderId);
        applyFoundedHouseToRelative(
                level,
                spouseId,
                houseName,
                houseWords,
                houseWordsPersonality,
                founderId,
                founderName,
                capitalId,
                capitalName
        );

        for (UUID childId : MCAIntegrationBridge.getChildren(level, founderId)) {
            applyFoundedHouseToRelative(
                    level,
                    childId,
                    houseName,
                    houseWords,
                    houseWordsPersonality,
                    founderId,
                    founderName,
                    capitalId,
                    capitalName
            );
        }
    }

    private static void applyFoundedHouseToRelative(
            ServerLevel level,
            UUID relativeId,
            String houseName,
            String houseWords,
            String houseWordsPersonality,
            UUID founderId,
            String founderName,
            UUID capitalId,
            String capitalName
    ) {
        if (level == null || relativeId == null || houseName == null || houseName.isBlank()) {
            return;
        }

        Entity relative = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, relativeId);
        if (relative == null) {
            return;
        }

        VillagerIdentityService.ensureAssigned(level, relative);

        VillagerIdentityData identity = VillagerIdentityService.getIdentity(relative);
        if (identity.hasFoundedHouse()) {
            return;
        }

        VillagerIdentityService.assignCurrentSurname(level, relative, houseName, SurnameSource.ROYAL_HOUSE);
        VillagerIdentityService.foundHouse(
                level,
                relative,
                houseName,
                houseWords,
                houseWordsPersonality,
                founderId,
                founderName,
                capitalId,
                capitalName
        );
    }

    private static String resolveFounderPersonality(Entity founder) {
        if (founder == null) {
            return HouseWordsPool.UNASSIGNED;
        }

        try {
            Object brain = invokeNoArg(founder, "getVillagerBrain");
            if (brain == null) {
                return HouseWordsPool.UNASSIGNED;
            }

            Object personality = invokeNoArg(brain, "getPersonality");
            if (personality == null) {
                return HouseWordsPool.UNASSIGNED;
            }

            if (personality instanceof Enum<?> enumValue) {
                return HouseWordsPool.normalizeBucket(enumValue.name());
            }

            return HouseWordsPool.normalizeBucket(String.valueOf(personality));
        } catch (Throwable ignored) {
            return HouseWordsPool.UNASSIGNED;
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }

        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }

    public record HouseFoundationResult(
            boolean success,
            String message,
            String houseName,
            String houseWords,
            String houseWordsPersonality
    ) {
        static HouseFoundationResult fail(String message) {
            return new HouseFoundationResult(false, message, "", "", "");
        }
    }

    private static final class CapitalTitleResolverAccess {
        private static CapitalRecord findCapital(ServerLevel level, UUID entityId) {
            try {
                return com.majesttyx.mcacapitals.capital.CapitalTitleResolver.findCapitalForEntity(level, entityId);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}