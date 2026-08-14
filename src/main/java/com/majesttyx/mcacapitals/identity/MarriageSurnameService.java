package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.house.PlayerHouseInheritanceMode;
import com.majesttyx.mcacapitals.house.PlayerHouseRecord;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.ai.Relationship;
import net.conczin.mca.entity.ai.relationship.EntityRelationship;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class MarriageSurnameService {

    private MarriageSurnameService() {
    }

    public static void onEntityRelationshipMarriage(Object relationshipOwner, Entity spouse) {
        if (relationshipOwner == null || spouse == null) {
            return;
        }

        Entity self = resolveRelationshipEntity(relationshipOwner);
        if (self == null && relationshipOwner instanceof EntityRelationship relationship) {
            self = relationship.getWorld().getEntity(relationship.getUUID());
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

        Gender firstGender = getGender(first);
        Gender secondGender = getGender(second);

        if (isMale(firstGender) && isFemale(secondGender)) {
            return first;
        }

        if (isMale(secondGender) && isFemale(firstGender)) {
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
        boolean playerFemale = resolvePlayerFemale(level, player);
        Gender spouseGender = getGender(spouse);

        if (!playerFemale && isFemale(spouseGender)) {
            return true;
        }

        if (playerFemale && isMale(spouseGender)) {
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

    private static Gender getGender(Entity entity) {
        if (entity == null) {
            return Gender.UNASSIGNED;
        }

        try {
            return EntityRelationship.of(entity)
                    .map(EntityRelationship::getGender)
                    .orElse(Gender.UNASSIGNED);
        } catch (Throwable ignored) {
            return Gender.UNASSIGNED;
        }
    }

    private static boolean isMale(Gender gender) {
        return gender == Gender.MALE;
    }

    private static boolean isFemale(Gender gender) {
        return gender == Gender.FEMALE;
    }

    private static Entity resolveRelationshipEntity(Object relationshipOwner) {
        if (relationshipOwner instanceof Relationship<?> relationship) {
            return resolveEntityField(relationship);
        }

        return resolveEntityField(relationshipOwner);
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

    private static boolean resolvePlayerFemale(ServerLevel level, ServerPlayer player) {
        try {
            Class<?> bridge = Class.forName("com.majesttyx.mcacapitals.util.MCAPlayerBridge");
            Method method = bridge.getDeclaredMethod("isPlayerFemale", ServerLevel.class, ServerPlayer.class);
            method.setAccessible(true);
            Object result = method.invoke(null, level, player);
            return result instanceof Boolean value && value;
        } catch (Throwable ignored) {
            return false;
        }
    }
}