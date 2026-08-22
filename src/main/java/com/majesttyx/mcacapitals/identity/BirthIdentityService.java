package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.EntityPersistentData;
import fabric.net.conczin.mca.entity.ai.relationship.EntityRelationship;
import fabric.net.conczin.mca.entity.ai.relationship.Gender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class BirthIdentityService {

    private static final String PENDING_BIRTH_IDENTITY_TAG = "McaCapitalsPendingBirthIdentity";

    private static final String KEY_HOUSEHOLD_SURNAME = "HouseholdSurname";
    private static final String KEY_HAS_HOUSE = "HasHouse";
    private static final String KEY_HOUSE_NAME = "HouseName";
    private static final String KEY_HOUSE_WORDS = "HouseWords";
    private static final String KEY_HOUSE_WORDS_PERSONALITY = "HouseWordsPersonality";
    private static final String KEY_HOUSE_FOUNDER_ID = "HouseFounderId";
    private static final String KEY_HOUSE_FOUNDER_NAME = "HouseFounderName";
    private static final String KEY_HOUSE_CAPITAL_ID = "HouseCapitalId";
    private static final String KEY_HOUSE_CAPITAL_NAME = "HouseCapitalName";

    private BirthIdentityService() {
    }

    public static boolean applyBirthIdentity(ServerLevel level, Entity child, Entity firstParent, Entity secondParent) {
        if (level == null || child == null || firstParent == null || secondParent == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(child)
                || !MCAIntegrationBridge.isMCAVillagerEntity(firstParent)
                || !MCAIntegrationBridge.isMCAVillagerEntity(secondParent)) {
            return false;
        }

        VillagerIdentityService.ensureAssigned(level, firstParent);
        VillagerIdentityService.ensureAssigned(level, secondParent);

        InheritedBirthIdentity inherited = resolveInheritedBirthIdentity(level, firstParent, secondParent);
        if (!inherited.isValid()) {
            return false;
        }

        writePendingBirthIdentity(child, inherited);

        boolean applied = applyInheritedIdentity(level, child, inherited);
        if (applied) {
            clearPendingBirthIdentity(child);
            VillagerIdentitySyncService.syncToNearbyPlayers(level, child);
        }

        return applied;
    }

    public static boolean repairFromParentsIfNeeded(ServerLevel level, Entity child) {
        if (level == null || child == null || !MCAIntegrationBridge.isMCAVillagerEntity(child)) {
            return false;
        }

        VillagerIdentityData existingIdentity = VillagerIdentityService.getIdentity(child);

        if (existingIdentity != null && "LEGAL_RENAME".equals(existingIdentity.surnameSource())) {
            return false;
        }

        InheritedBirthIdentity pending = readPendingBirthIdentity(child);
        if (pending.isValid()) {
            boolean applied = applyInheritedIdentity(level, child, pending);
            if (applied) {
                clearPendingBirthIdentity(child);
                VillagerIdentitySyncService.syncToNearbyPlayers(level, child);
            }
            return applied;
        }

        Set<UUID> parentIds = MCAIntegrationBridge.getParents(level, child.getUUID());
        if (parentIds == null || parentIds.size() < 2) {
            return false;
        }

        List<Entity> parents = new ArrayList<>();
        for (UUID parentId : parentIds) {
            Entity parent = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, parentId);
            if (parent != null && MCAIntegrationBridge.isMCAVillagerEntity(parent)) {
                parents.add(parent);
            }
        }

        if (parents.size() < 2) {
            return false;
        }

        VillagerIdentityService.ensureAssigned(level, parents.get(0));
        VillagerIdentityService.ensureAssigned(level, parents.get(1));

        InheritedBirthIdentity inherited = resolveInheritedBirthIdentity(level, parents.get(0), parents.get(1));
        if (!inherited.isValid()) {
            return false;
        }

        VillagerIdentityData childIdentity = VillagerIdentityService.getIdentity(child);

        if (childIdentity != null && "LEGAL_RENAME".equals(childIdentity.surnameSource())) {
            return false;
        }

        boolean missingSurname = !childIdentity.hasSurname();
        boolean generatedSurname = "GENERATED".equals(childIdentity.surnameSource());
        boolean currentSurnameWrong = !inherited.householdSurname().equals(childIdentity.currentSurname());
        boolean birthSurnameWrong = childIdentity.birthSurname() == null
                || childIdentity.birthSurname().isBlank()
                || "GENERATED".equals(childIdentity.surnameSource())
                || (isYoungVillager(level, child) && !inherited.householdSurname().equals(childIdentity.birthSurname()));

        boolean houseMissingOrWrong = inherited.hasHouse()
                && (!childIdentity.hasFoundedHouse()
                || !inherited.houseName().equals(childIdentity.houseName())
                || !inherited.houseWords().equals(childIdentity.houseWords()));

        if (!missingSurname
                && !generatedSurname
                && !birthSurnameWrong
                && !currentSurnameWrong
                && !houseMissingOrWrong) {
            return false;
        }

        boolean applied = applyInheritedIdentity(level, child, inherited);
        if (applied) {
            VillagerIdentitySyncService.syncToNearbyPlayers(level, child);
        }

        return applied;
    }

    private static InheritedBirthIdentity resolveInheritedBirthIdentity(ServerLevel level, Entity firstParent, Entity secondParent) {
        Entity householdSource = resolveHouseholdSource(level, firstParent, secondParent);
        if (householdSource == null) {
            return InheritedBirthIdentity.empty();
        }

        String householdSurname = VillagerIdentityService.getCurrentSurname(householdSource);
        if (householdSurname == null || householdSurname.isBlank()) {
            return InheritedBirthIdentity.empty();
        }

        VillagerIdentityData sourceIdentity = VillagerIdentityService.getIdentity(householdSource);

        if (sourceIdentity.hasFoundedHouse() && householdSurname.equals(sourceIdentity.houseName())) {
            return new InheritedBirthIdentity(
                    householdSurname,
                    true,
                    sourceIdentity.houseName(),
                    sourceIdentity.houseWords(),
                    sourceIdentity.houseWordsPersonality(),
                    sourceIdentity.houseFounderId(),
                    sourceIdentity.houseFounderName(),
                    sourceIdentity.houseFoundedInCapitalId(),
                    sourceIdentity.houseFoundedInCapitalName()
            );
        }

        return new InheritedBirthIdentity(
                householdSurname,
                false,
                "",
                "",
                "",
                null,
                "",
                null,
                ""
        );
    }

    private static boolean applyInheritedIdentity(ServerLevel level, Entity child, InheritedBirthIdentity inherited) {
        if (level == null || child == null || !inherited.isValid()) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(child)) {
            return false;
        }

        VillagerIdentityService.ensureOriginFromCurrentVillage(level, child, null, OriginSource.BIRTH);

        boolean birthAssigned = VillagerIdentityService.assignBirthSurname(
                level,
                child,
                inherited.householdSurname(),
                SurnameSource.BIRTH
        );

        boolean currentAssigned = VillagerIdentityService.assignCurrentSurname(
                level,
                child,
                inherited.householdSurname(),
                SurnameSource.BIRTH
        );

        boolean houseAssigned = true;
        if (inherited.hasHouse()) {
            houseAssigned = VillagerIdentityService.foundHouse(
                    level,
                    child,
                    inherited.houseName(),
                    inherited.houseWords(),
                    inherited.houseWordsPersonality(),
                    inherited.houseFounderId(),
                    inherited.houseFounderName(),
                    inherited.houseFoundedInCapitalId(),
                    inherited.houseFoundedInCapitalName()
            );
        }

        return birthAssigned || currentAssigned || houseAssigned;
    }

    private static Entity resolveHouseholdSource(ServerLevel level, Entity firstParent, Entity secondParent) {
        String firstSurname = VillagerIdentityService.getCurrentSurname(firstParent);
        String secondSurname = VillagerIdentityService.getCurrentSurname(secondParent);

        if (!firstSurname.isBlank() && firstSurname.equals(secondSurname)) {
            VillagerIdentityData firstIdentity = VillagerIdentityService.getIdentity(firstParent);
            if (firstIdentity.hasFoundedHouse() && firstSurname.equals(firstIdentity.houseName())) {
                return firstParent;
            }

            VillagerIdentityData secondIdentity = VillagerIdentityService.getIdentity(secondParent);
            if (secondIdentity.hasFoundedHouse() && secondSurname.equals(secondIdentity.houseName())) {
                return secondParent;
            }

            return firstParent;
        }

        int firstRank = getVillagerTitleRank(level, firstParent);
        int secondRank = getVillagerTitleRank(level, secondParent);

        if (firstRank < secondRank) {
            return firstParent;
        }

        if (secondRank < firstRank) {
            return secondParent;
        }

        Gender firstGender = getGender(firstParent);
        Gender secondGender = getGender(secondParent);

        if (firstGender == Gender.MALE && secondGender == Gender.FEMALE) {
            return firstParent;
        }

        if (secondGender == Gender.MALE && firstGender == Gender.FEMALE) {
            return secondParent;
        }

        if (!firstSurname.isBlank()) {
            return firstParent;
        }

        return secondParent;
    }

    private static boolean isYoungVillager(ServerLevel level, Entity child) {
        if (level == null || child == null) {
            return false;
        }

        String ageState = MCAIntegrationBridge.getAgeState(level, child.getUUID());
        if (ageState == null) {
            return false;
        }

        String normalized = ageState.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("BABY")
                || normalized.equals("TODDLER")
                || normalized.equals("CHILD")
                || normalized.equals("TEEN");
    }

    private static int getVillagerTitleRank(ServerLevel level, Entity entity) {
        if (level == null || entity == null) {
            return 900;
        }

        CapitalTitleResolver.ResolvedTitleId titleId = CapitalTitleResolver.getResolvedTitleIdForEntity(level, entity.getUUID());
        return rankValue(titleId);
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

    private static void writePendingBirthIdentity(Entity child, InheritedBirthIdentity inherited) {
        if (child == null || !inherited.isValid()) {
            return;
        }

        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_HOUSEHOLD_SURNAME, inherited.householdSurname());
        tag.putBoolean(KEY_HAS_HOUSE, inherited.hasHouse());
        tag.putString(KEY_HOUSE_NAME, inherited.houseName());
        tag.putString(KEY_HOUSE_WORDS, inherited.houseWords());
        tag.putString(KEY_HOUSE_WORDS_PERSONALITY, inherited.houseWordsPersonality());
        tag.putString(KEY_HOUSE_FOUNDER_NAME, inherited.houseFounderName());
        tag.putString(KEY_HOUSE_CAPITAL_NAME, inherited.houseFoundedInCapitalName());

        if (inherited.houseFounderId() != null) {
            tag.putUUID(KEY_HOUSE_FOUNDER_ID, inherited.houseFounderId());
        }

        if (inherited.houseFoundedInCapitalId() != null) {
            tag.putUUID(KEY_HOUSE_CAPITAL_ID, inherited.houseFoundedInCapitalId());
        }

        EntityPersistentData.get(child).put(PENDING_BIRTH_IDENTITY_TAG, tag);
    }

    private static InheritedBirthIdentity readPendingBirthIdentity(Entity child) {
        if (child == null) {
            return InheritedBirthIdentity.empty();
        }

        CompoundTag persistent = EntityPersistentData.get(child);
        if (!persistent.contains(PENDING_BIRTH_IDENTITY_TAG)) {
            return InheritedBirthIdentity.empty();
        }

        CompoundTag tag = persistent.getCompound(PENDING_BIRTH_IDENTITY_TAG);
        String householdSurname = tag.getString(KEY_HOUSEHOLD_SURNAME);
        if (householdSurname == null || householdSurname.isBlank()) {
            return InheritedBirthIdentity.empty();
        }

        return new InheritedBirthIdentity(
                householdSurname,
                tag.getBoolean(KEY_HAS_HOUSE),
                tag.getString(KEY_HOUSE_NAME),
                tag.getString(KEY_HOUSE_WORDS),
                tag.getString(KEY_HOUSE_WORDS_PERSONALITY),
                tag.hasUUID(KEY_HOUSE_FOUNDER_ID) ? tag.getUUID(KEY_HOUSE_FOUNDER_ID) : null,
                tag.getString(KEY_HOUSE_FOUNDER_NAME),
                tag.hasUUID(KEY_HOUSE_CAPITAL_ID) ? tag.getUUID(KEY_HOUSE_CAPITAL_ID) : null,
                tag.getString(KEY_HOUSE_CAPITAL_NAME)
        );
    }

    private static void clearPendingBirthIdentity(Entity child) {
        if (child == null) {
            return;
        }

        EntityPersistentData.get(child).remove(PENDING_BIRTH_IDENTITY_TAG);
    }

    private record InheritedBirthIdentity(
            String householdSurname,
            boolean hasHouse,
            String houseName,
            String houseWords,
            String houseWordsPersonality,
            UUID houseFounderId,
            String houseFounderName,
            UUID houseFoundedInCapitalId,
            String houseFoundedInCapitalName
    ) {
        static InheritedBirthIdentity empty() {
            return new InheritedBirthIdentity("", false, "", "", "", null, "", null, "");
        }

        boolean isValid() {
            return householdSurname != null && !householdSurname.isBlank();
        }
    }
}