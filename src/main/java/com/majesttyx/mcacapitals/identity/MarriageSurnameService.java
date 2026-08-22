package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.house.PlayerHouseInheritanceMode;
import com.majesttyx.mcacapitals.house.PlayerHouseRecord;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class MarriageSurnameService {

    private MarriageSurnameService() {
    }

    public static void onEntityRelationshipMarriage(Object relationshipOwner, Entity spouse) {
        if (relationshipOwner == null || spouse == null) {
            return;
        }

        Entity self = resolveRelationshipEntity(relationshipOwner);

        if (self == null) {
            self = resolveRelationshipWorldEntity(relationshipOwner);
        }

        if (self == null) {
            return;
        }

        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(self) || !MCAIntegrationBridge.isMCAVillagerEntity(spouse)) {
            return;
        }

        applyVillagerVillagerMarriage(level, self, spouse);
    }

    public static void onFamilyTreePartnerUpdate(Object familyTreeNode, Entity spouse, Object relationshipState) {
        if (familyTreeNode == null || spouse == null || !isMarriageState(relationshipState)) {
            return;
        }

        if (!(spouse.level() instanceof ServerLevel level)) {
            return;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(spouse)) {
            return;
        }

        UUID selfId = resolveFamilyTreeNodeId(familyTreeNode);
        if (selfId == null) {
            return;
        }

        Entity self = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, selfId);
        if (self == null || !MCAIntegrationBridge.isMCAVillagerEntity(self)) {
            return;
        }

        applyVillagerVillagerMarriage(level, self, spouse);
    }

    public static void onPlayerMarriage(ServerLevel level, ServerPlayer player, Entity spouse) {
        if (level == null || player == null || spouse == null || !MCAIntegrationBridge.isMCAVillagerEntity(spouse)) {
            return;
        }

        PlayerHouseRecord record = PlayerHouseService.get(level, player.getUUID());
        if (record == null || !record.hasHouseName()) {
            return;
        }

        VillagerIdentityService.ensureAssigned(level, spouse);

        if (record.getInheritanceMode() == PlayerHouseInheritanceMode.PRESERVE_PLAYER_HOUSE) {
            PlayerHouseIdentityService.applyPlayerHouseIdentityToVillager(
                    level,
                    spouse,
                    player.getUUID(),
                    record,
                    SurnameSource.MARRIAGE,
                    false
            );
            VillagerIdentitySyncService.syncToNearbyPlayers(level, spouse);
            return;
        }

        if (shouldSpouseTakePlayerHouse(level, player, spouse)) {
            PlayerHouseIdentityService.applyPlayerHouseIdentityToVillager(
                    level,
                    spouse,
                    player.getUUID(),
                    record,
                    SurnameSource.MARRIAGE,
                    false
            );
        }

        VillagerIdentitySyncService.syncToNearbyPlayers(level, spouse);
    }

    private static void applyVillagerVillagerMarriage(ServerLevel level, Entity first, Entity second) {
        VillagerIdentityService.ensureAssigned(level, first);
        VillagerIdentityService.ensureAssigned(level, second);

        Entity householdSource = resolveVillagerHouseholdSource(level, first, second);
        if (householdSource == null) {
            return;
        }

        Entity other = householdSource.getUUID().equals(first.getUUID()) ? second : first;

        String householdSurname = VillagerIdentityService.getCurrentSurname(householdSource);
        if (householdSurname.isBlank()) {
            return;
        }

        VillagerIdentityService.assignCurrentSurname(level, first, householdSurname, SurnameSource.MARRIAGE);
        VillagerIdentityService.assignCurrentSurname(level, second, householdSurname, SurnameSource.MARRIAGE);

        applyEstablishedHouseAfterMarriage(level, householdSource, other, householdSurname);

        VillagerIdentitySyncService.syncToNearbyPlayers(level, first);
        VillagerIdentitySyncService.syncToNearbyPlayers(level, second);
    }

    private static Entity resolveVillagerHouseholdSource(ServerLevel level, Entity first, Entity second) {
        int firstRank = getVillagerTitleRank(level, first);
        int secondRank = getVillagerTitleRank(level, second);

        if (firstRank < secondRank) {
            return first;
        }

        if (secondRank < firstRank) {
            return second;
        }

        GenderKind firstGender = getGender(first);
        GenderKind secondGender = getGender(second);

        if (firstGender == GenderKind.MALE && secondGender == GenderKind.FEMALE) {
            return first;
        }

        if (secondGender == GenderKind.MALE && firstGender == GenderKind.FEMALE) {
            return second;
        }

        return chooseEqualRankSource(level, first, second);
    }

    private static void applyEstablishedHouseAfterMarriage(
            ServerLevel level,
            Entity source,
            Entity target,
            String householdSurname
    ) {
        if (level == null || source == null || target == null || householdSurname == null || householdSurname.isBlank()) {
            return;
        }

        VillagerIdentityData sourceIdentity = VillagerIdentityService.getIdentity(source);
        if (!sourceIdentity.hasFoundedHouse()) {
            return;
        }

        if (!householdSurname.equals(sourceIdentity.houseName())) {
            return;
        }

        VillagerIdentityService.assignCurrentSurname(level, target, sourceIdentity.houseName(), SurnameSource.MARRIAGE);
        VillagerIdentityService.foundHouse(
                level,
                target,
                sourceIdentity.houseName(),
                sourceIdentity.houseWords(),
                sourceIdentity.houseWordsPersonality(),
                sourceIdentity.houseFounderId(),
                sourceIdentity.houseFounderName(),
                sourceIdentity.houseFoundedInCapitalId(),
                sourceIdentity.houseFoundedInCapitalName()
        );
    }

    private static boolean shouldSpouseTakePlayerHouse(ServerLevel level, ServerPlayer player, Entity spouse) {
        boolean playerFemale = MCAIntegrationBridge.isPlayerFemale(level, player);
        GenderKind spouseGender = getGender(spouse);

        if (!playerFemale && spouseGender == GenderKind.FEMALE) {
            return true;
        }

        if (playerFemale && spouseGender == GenderKind.MALE) {
            return false;
        }

        int playerRank = getPlayerTitleRank(player.getUUID());
        int spouseRank = getVillagerTitleRank(level, spouse);

        return playerRank < spouseRank;
    }

    private static Entity chooseEqualRankSource(ServerLevel level, Entity first, Entity second) {
        String firstSurname = VillagerIdentityService.getCurrentSurname(first);
        String secondSurname = VillagerIdentityService.getCurrentSurname(second);

        if (firstSurname.isBlank()) {
            return second;
        }

        if (secondSurname.isBlank()) {
            return first;
        }

        String seed = first.getUUID() + ":" + second.getUUID() + ":" + level.getGameTime();
        return Math.floorMod(seed.hashCode(), 2) == 0 ? first : second;
    }

    private static int getVillagerTitleRank(ServerLevel level, Entity entity) {
        if (level == null || entity == null) {
            return 900;
        }

        CapitalTitleResolver.ResolvedTitleId titleId = CapitalTitleResolver.getResolvedTitleIdForEntity(level, entity.getUUID());
        return rankValue(titleId);
    }

    private static int getPlayerTitleRank(UUID playerId) {
        if (playerId == null) {
            return 900;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }

            if (playerId.equals(capital.getPlayerSovereignId())) {
                return 30;
            }

            if (playerId.equals(capital.getPlayerConsortId())) {
                return 40;
            }
        }

        return 900;
    }

    private static int rankValue(CapitalTitleResolver.ResolvedTitleId titleId) {
        if (titleId == null) {
            return 900;
        }

        return switch (titleId) {
            case HIGH_SOVEREIGN -> 20;
            case SOVEREIGN -> 30;
            case SOVEREIGN_CONSORT -> 40;
            case SOVEREIGN_DOWAGER -> 50;
            case HEIR_APPARENT -> 60;
            case CROWN_HEIR -> 70;
            case ROYAL_CHILD -> 80;
            case PRINCE_CONSORT -> 90;
            case DOWAGER_PRINCE -> 100;
            case HAND -> 110;
            case GRAND_MAESTER -> 115;
            case DUKE -> 120;
            case DOWAGER_DUKE -> 130;
            case MAESTER -> 140;
            case LORD_COMMANDER -> 150;
            case LORD -> 170;
            case ROYAL_GUARD, KNIGHT -> 180;
            default -> 900;
        };
    }

    private static GenderKind getGender(Entity entity) {
        if (entity == null) {
            return GenderKind.UNASSIGNED;
        }

        Optional<Object> relationship = resolveEntityRelationship(entity);
        if (relationship.isPresent()) {
            Object gender = invokeNoArg(relationship.get(), "getGender");
            return normalizeGender(gender);
        }

        if (entity.level() instanceof ServerLevel level) {
            if (MCAIntegrationBridge.isFemale(level, entity.getUUID())) {
                return GenderKind.FEMALE;
            }
        }

        return GenderKind.UNASSIGNED;
    }

    private static Optional<Object> resolveEntityRelationship(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        String[] classNames = new String[] {
                "forge.net.conczin.mca.entity.ai.relationship.EntityRelationship"
        };

        for (String className : classNames) {
            try {
                Class<?> relationshipClass = Class.forName(className);
                Method ofMethod = relationshipClass.getMethod("of", Entity.class);
                Object value = ofMethod.invoke(null, entity);

                if (value instanceof Optional<?> optional) {
                    return optional.map(object -> object);
                }

                if (value != null) {
                    return Optional.of(value);
                }
            } catch (Throwable ignored) {
            }
        }

        return Optional.empty();
    }

    private static GenderKind normalizeGender(Object gender) {
        if (gender == null) {
            return GenderKind.UNASSIGNED;
        }

        String value;
        if (gender instanceof Enum<?> enumValue) {
            value = enumValue.name();
        } else {
            value = String.valueOf(gender);
        }

        if (value == null || value.isBlank()) {
            return GenderKind.UNASSIGNED;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.endsWith(".MALE")) {
            return GenderKind.MALE;
        }

        if (normalized.endsWith(".FEMALE")) {
            return GenderKind.FEMALE;
        }

        return switch (normalized) {
            case "MALE" -> GenderKind.MALE;
            case "FEMALE" -> GenderKind.FEMALE;
            default -> GenderKind.UNASSIGNED;
        };
    }

    private static boolean isMarriageState(Object relationshipState) {
        if (relationshipState == null) {
            return false;
        }

        String value;
        if (relationshipState instanceof Enum<?> enumValue) {
            value = enumValue.name();
        } else {
            value = String.valueOf(relationshipState);
        }

        if (value == null) {
            return false;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.endsWith("MARRIED_TO_VILLAGER") || normalized.endsWith("MARRIED_TO_PLAYER");
    }

    private static UUID resolveFamilyTreeNodeId(Object familyTreeNode) {
        Object id = invokeNoArg(familyTreeNode, "id");
        return id instanceof UUID uuid ? uuid : null;
    }

    private static Entity resolveRelationshipEntity(Object relationshipOwner) {
        if (relationshipOwner instanceof Entity entity) {
            return entity;
        }

        Entity fromField = resolveEntityField(relationshipOwner);
        if (fromField != null) {
            return fromField;
        }

        Object owner = invokeNoArg(relationshipOwner, "getEntity");
        if (owner instanceof Entity entity) {
            return entity;
        }

        owner = invokeNoArg(relationshipOwner, "entity");
        if (owner instanceof Entity entity) {
            return entity;
        }

        owner = invokeNoArg(relationshipOwner, "asEntity");
        if (owner instanceof Entity entity) {
            return entity;
        }

        return null;
    }

    private static Entity resolveRelationshipWorldEntity(Object relationshipOwner) {
        if (relationshipOwner == null) {
            return null;
        }

        Object world = invokeNoArg(relationshipOwner, "getWorld");
        Object uuid = invokeNoArg(relationshipOwner, "getUUID");

        if (world == null || !(uuid instanceof UUID entityId)) {
            return null;
        }

        Object entity = invokeMethod(world, "getEntity", UUID.class, entityId);
        return entity instanceof Entity resolved ? resolved : null;
    }

    private static Entity resolveEntityField(Object relationshipOwner) {
        if (relationshipOwner == null) {
            return null;
        }

        Class<?> current = relationshipOwner.getClass();

        while (current != null) {
            try {
                Field field = current.getDeclaredField("entity");
                field.setAccessible(true);
                Object value = field.get(relationshipOwner);
                return value instanceof Entity entity ? entity : null;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
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

    private static Object invokeMethod(Object target, String methodName, Class<?> parameterType, Object argument) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }

        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterType);
                method.setAccessible(true);
                return method.invoke(target, argument);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }

    private enum GenderKind {
        MALE,
        FEMALE,
        UNASSIGNED
    }
}