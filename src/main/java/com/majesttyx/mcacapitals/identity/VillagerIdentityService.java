package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

public final class VillagerIdentityService {

    private static final String IDENTITY_TAG = "McaCapitalsIdentity";

    private static final String ORIGIN_VILLAGE_ID = "OriginVillageId";
    private static final String ORIGIN_VILLAGE_NAME = "OriginVillageName";
    private static final String ORIGIN_CAPITAL_ID = "OriginCapitalId";
    private static final String ORIGIN_CAPITAL_NAME = "OriginCapitalName";
    private static final String ORIGIN_SOURCE = "OriginSource";
    private static final String ORIGIN_SET_AT_GAME_TIME = "OriginSetAtGameTime";
    private static final String ORIGIN_DIMENSION = "OriginDimension";
    private static final String ORIGIN_BLOCK_X = "OriginBlockX";
    private static final String ORIGIN_BLOCK_Y = "OriginBlockY";
    private static final String ORIGIN_BLOCK_Z = "OriginBlockZ";

    private static final String BIRTH_SURNAME = "BirthSurname";
    private static final String CURRENT_SURNAME = "CurrentSurname";
    private static final String SURNAME_SOURCE = "SurnameSource";
    private static final String SURNAME_SET_AT_GAME_TIME = "SurnameSetAtGameTime";

    private static final String HOUSE_FOUNDED = "HouseFounded";
    private static final String HOUSE_NAME = "HouseName";
    private static final String HOUSE_WORDS = "HouseWords";
    private static final String HOUSE_WORDS_PERSONALITY = "HouseWordsPersonality";
    private static final String HOUSE_FOUNDER_ID = "HouseFounderId";
    private static final String HOUSE_FOUNDER_NAME = "HouseFounderName";
    private static final String HOUSE_FOUNDED_AT_GAME_TIME = "HouseFoundedAtGameTime";
    private static final String HOUSE_FOUNDED_IN_CAPITAL_ID = "HouseFoundedInCapitalId";
    private static final String HOUSE_FOUNDED_IN_CAPITAL_NAME = "HouseFoundedInCapitalName";

    private VillagerIdentityService() {
    }

    public static boolean ensureAssigned(ServerLevel level, Entity entity) {
        if (!canStoreIdentity(level, entity)) {
            return false;
        }

        boolean changed = false;
        changed |= ensureSurname(level, entity, SurnameSource.GENERATED);
        changed |= ensureOriginFromCurrentVillage(level, entity, null, OriginSource.DISCOVERED);
        return changed;
    }

    public static boolean ensureAssigned(ServerLevel level, Entity entity, CapitalRecord capital) {
        if (!canStoreIdentity(level, entity)) {
            return false;
        }

        boolean changed = false;
        changed |= ensureSurname(level, entity, SurnameSource.GENERATED);
        changed |= ensureOriginFromCurrentVillage(level, entity, capital, OriginSource.DISCOVERED);
        return changed;
    }

    public static boolean ensureAssigned(ServerLevel level, UUID villagerId) {
        Entity entity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, villagerId);
        return ensureAssigned(level, entity);
    }

    public static int ensureResidents(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (level == null || capital == null || residents == null || residents.isEmpty()) {
            return 0;
        }

        int changed = 0;
        for (UUID residentId : residents) {
            Entity entity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, residentId);
            if (ensureAssigned(level, entity, capital)) {
                changed++;
            }
        }
        return changed;
    }

    public static boolean ensureSurname(ServerLevel level, Entity entity, SurnameSource source) {
        if (!canStoreIdentity(level, entity)) {
            return false;
        }

        CompoundTag identity = getIdentityTag(entity);
        if (hasNonBlankString(identity, CURRENT_SURNAME)) {
            return false;
        }

        String surname = SurnamePool.generate(level, entity);
        return assignSurname(level, entity, surname, source == null ? SurnameSource.GENERATED : source, true);
    }

    public static boolean ensureOriginFromCurrentVillage(ServerLevel level, Entity entity, CapitalRecord knownCapital, OriginSource source) {
        if (!canStoreIdentity(level, entity)) {
            return false;
        }

        CompoundTag identity = getIdentityTag(entity);
        if (hasOrigin(identity)) {
            return false;
        }

        Integer villageId = knownCapital != null ? knownCapital.getVillageId() : null;
        if (villageId == null) {
            villageId = MCAIntegrationBridge.getVillageIdForResident(level, entity.getUUID());
        }
        if (villageId == null || !MCAIntegrationBridge.hasVillage(level, villageId)) {
            return false;
        }

        CapitalRecord capital = knownCapital != null ? knownCapital : CapitalManager.getCapitalForVillage(villageId);
        String villageName = MCAIntegrationBridge.getVillageName(level, villageId);
        String capitalName = capital == null ? "" : MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        identity.putInt(ORIGIN_VILLAGE_ID, villageId);
        identity.putString(ORIGIN_VILLAGE_NAME, villageName);
        if (capital != null && capital.getCapitalId() != null) {
            identity.putUUID(ORIGIN_CAPITAL_ID, capital.getCapitalId());
            identity.putString(ORIGIN_CAPITAL_NAME, capitalName);
        } else {
            identity.remove(ORIGIN_CAPITAL_ID);
            identity.remove(ORIGIN_CAPITAL_NAME);
        }
        identity.putString(ORIGIN_SOURCE, (source == null ? OriginSource.DISCOVERED : source).name());
        identity.putLong(ORIGIN_SET_AT_GAME_TIME, level.getGameTime());
        identity.putString(ORIGIN_DIMENSION, level.dimension().location().toString());

        BlockPos pos = entity.blockPosition();
        identity.putInt(ORIGIN_BLOCK_X, pos.getX());
        identity.putInt(ORIGIN_BLOCK_Y, pos.getY());
        identity.putInt(ORIGIN_BLOCK_Z, pos.getZ());

        saveIdentityTag(entity, identity);
        return true;
    }

    public static boolean assignOriginFromCurrentVillage(ServerLevel level, Entity entity) {
        if (!canStoreIdentity(level, entity)) {
            return false;
        }

        clearOrigin(entity);
        return ensureOriginFromCurrentVillage(level, entity, null, OriginSource.DEBUG);
    }

    public static boolean assignSurname(ServerLevel level, Entity entity, String surname, SurnameSource source) {
        return assignSurname(level, entity, surname, source, false);
    }

    public static boolean assignBirthSurname(ServerLevel level, Entity entity, String surname, SurnameSource source) {
        if (!canStoreIdentity(level, entity)) {
            return false;
        }

        surname = normalizeSurname(surname);
        if (surname.isBlank()) {
            return false;
        }

        CompoundTag identity = getIdentityTag(entity);
        identity.putString(BIRTH_SURNAME, surname);
        identity.putString(SURNAME_SOURCE, (source == null ? SurnameSource.DEBUG : source).name());
        identity.putLong(SURNAME_SET_AT_GAME_TIME, level == null ? 0L : level.getGameTime());
        saveIdentityTag(entity, identity);
        return true;
    }

    public static boolean assignCurrentSurname(ServerLevel level, Entity entity, String surname, SurnameSource source) {
        if (!canStoreIdentity(level, entity)) {
            return false;
        }

        surname = normalizeSurname(surname);
        if (surname.isBlank()) {
            return false;
        }

        CompoundTag identity = getIdentityTag(entity);
        identity.putString(CURRENT_SURNAME, surname);
        identity.putString(SURNAME_SOURCE, (source == null ? SurnameSource.DEBUG : source).name());
        identity.putLong(SURNAME_SET_AT_GAME_TIME, level == null ? 0L : level.getGameTime());
        saveIdentityTag(entity, identity);
        return true;
    }

    public static boolean foundHouse(
            ServerLevel level,
            Entity entity,
            String houseName,
            String houseWords,
            String houseWordsPersonality,
            UUID founderId,
            String founderName,
            UUID capitalId,
            String capitalName
    ) {
        if (!canStoreIdentity(level, entity)) {
            return false;
        }

        houseName = normalizeSurname(houseName);
        if (houseName.isBlank()) {
            return false;
        }

        CompoundTag identity = getIdentityTag(entity);
        identity.putBoolean(HOUSE_FOUNDED, true);
        identity.putString(HOUSE_NAME, houseName);
        identity.putString(HOUSE_WORDS, houseWords == null ? "" : houseWords.trim().replaceAll("\\s+", " "));
        identity.putString(HOUSE_WORDS_PERSONALITY, houseWordsPersonality == null ? "" : houseWordsPersonality);
        if (founderId != null) {
            identity.putUUID(HOUSE_FOUNDER_ID, founderId);
        }
        identity.putString(HOUSE_FOUNDER_NAME, founderName == null ? "" : founderName);
        identity.putLong(HOUSE_FOUNDED_AT_GAME_TIME, level.getGameTime());
        if (capitalId != null) {
            identity.putUUID(HOUSE_FOUNDED_IN_CAPITAL_ID, capitalId);
        }
        identity.putString(HOUSE_FOUNDED_IN_CAPITAL_NAME, capitalName == null ? "" : capitalName);
        saveIdentityTag(entity, identity);
        return true;
    }

    private static boolean assignSurname(ServerLevel level, Entity entity, String surname, SurnameSource source, boolean onlyIfMissingBirthSurname) {
        if (!canStoreIdentity(level, entity)) {
            return false;
        }

        surname = normalizeSurname(surname);
        if (surname.isBlank()) {
            return false;
        }

        CompoundTag identity = getIdentityTag(entity);
        if (!onlyIfMissingBirthSurname || !hasNonBlankString(identity, BIRTH_SURNAME)) {
            identity.putString(BIRTH_SURNAME, surname);
        }
        identity.putString(CURRENT_SURNAME, surname);
        identity.putString(SURNAME_SOURCE, (source == null ? SurnameSource.DEBUG : source).name());
        identity.putLong(SURNAME_SET_AT_GAME_TIME, level == null ? 0L : level.getGameTime());
        saveIdentityTag(entity, identity);
        return true;
    }

    public static void clearOrigin(Entity entity) {
        if (entity == null) {
            return;
        }

        CompoundTag identity = getIdentityTag(entity);
        identity.remove(ORIGIN_VILLAGE_ID);
        identity.remove(ORIGIN_VILLAGE_NAME);
        identity.remove(ORIGIN_CAPITAL_ID);
        identity.remove(ORIGIN_CAPITAL_NAME);
        identity.remove(ORIGIN_SOURCE);
        identity.remove(ORIGIN_SET_AT_GAME_TIME);
        identity.remove(ORIGIN_DIMENSION);
        identity.remove(ORIGIN_BLOCK_X);
        identity.remove(ORIGIN_BLOCK_Y);
        identity.remove(ORIGIN_BLOCK_Z);
        saveIdentityTag(entity, identity);
    }

    public static void clearSurname(Entity entity) {
        if (entity == null) {
            return;
        }

        CompoundTag identity = getIdentityTag(entity);
        identity.remove(BIRTH_SURNAME);
        identity.remove(CURRENT_SURNAME);
        identity.remove(SURNAME_SOURCE);
        identity.remove(SURNAME_SET_AT_GAME_TIME);
        saveIdentityTag(entity, identity);
    }

    public static void clearHouse(Entity entity) {
        if (entity == null) {
            return;
        }

        CompoundTag identity = getIdentityTag(entity);
        identity.remove(HOUSE_FOUNDED);
        identity.remove(HOUSE_NAME);
        identity.remove(HOUSE_WORDS);
        identity.remove(HOUSE_WORDS_PERSONALITY);
        identity.remove(HOUSE_FOUNDER_ID);
        identity.remove(HOUSE_FOUNDER_NAME);
        identity.remove(HOUSE_FOUNDED_AT_GAME_TIME);
        identity.remove(HOUSE_FOUNDED_IN_CAPITAL_ID);
        identity.remove(HOUSE_FOUNDED_IN_CAPITAL_NAME);
        saveIdentityTag(entity, identity);
    }

    public static VillagerIdentityData getIdentity(Entity entity) {
        if (entity == null) {
            return empty();
        }

        CompoundTag identity = getIdentityTag(entity);
        UUID originCapitalId = identity.hasUUID(ORIGIN_CAPITAL_ID) ? identity.getUUID(ORIGIN_CAPITAL_ID) : null;
        Integer originVillageId = identity.contains(ORIGIN_VILLAGE_ID) ? identity.getInt(ORIGIN_VILLAGE_ID) : null;

        return new VillagerIdentityData(
                null,
                originVillageId,
                getStringOrEmpty(identity, ORIGIN_VILLAGE_NAME),
                originCapitalId,
                getStringOrEmpty(identity, ORIGIN_CAPITAL_NAME),
                getStringOrEmpty(identity, ORIGIN_SOURCE),
                identity.contains(ORIGIN_SET_AT_GAME_TIME) ? identity.getLong(ORIGIN_SET_AT_GAME_TIME) : 0L,
                getStringOrEmpty(identity, ORIGIN_DIMENSION),
                identity.contains(ORIGIN_BLOCK_X) ? identity.getInt(ORIGIN_BLOCK_X) : 0,
                identity.contains(ORIGIN_BLOCK_Y) ? identity.getInt(ORIGIN_BLOCK_Y) : 0,
                identity.contains(ORIGIN_BLOCK_Z) ? identity.getInt(ORIGIN_BLOCK_Z) : 0,
                getStringOrEmpty(identity, BIRTH_SURNAME),
                getStringOrEmpty(identity, CURRENT_SURNAME),
                getStringOrEmpty(identity, SURNAME_SOURCE),
                identity.contains(SURNAME_SET_AT_GAME_TIME) ? identity.getLong(SURNAME_SET_AT_GAME_TIME) : 0L,
                identity.getBoolean(HOUSE_FOUNDED),
                getStringOrEmpty(identity, HOUSE_NAME),
                getStringOrEmpty(identity, HOUSE_WORDS),
                getStringOrEmpty(identity, HOUSE_WORDS_PERSONALITY),
                identity.hasUUID(HOUSE_FOUNDER_ID) ? identity.getUUID(HOUSE_FOUNDER_ID) : null,
                getStringOrEmpty(identity, HOUSE_FOUNDER_NAME),
                identity.contains(HOUSE_FOUNDED_AT_GAME_TIME) ? identity.getLong(HOUSE_FOUNDED_AT_GAME_TIME) : 0L,
                identity.hasUUID(HOUSE_FOUNDED_IN_CAPITAL_ID) ? identity.getUUID(HOUSE_FOUNDED_IN_CAPITAL_ID) : null,
                getStringOrEmpty(identity, HOUSE_FOUNDED_IN_CAPITAL_NAME)
        );
    }

    public static String getCurrentSurname(Entity entity) {
        if (entity == null) {
            return "";
        }
        return getStringOrEmpty(getIdentityTag(entity), CURRENT_SURNAME);
    }

    public static String getBirthSurname(Entity entity) {
        if (entity == null) {
            return "";
        }
        return getStringOrEmpty(getIdentityTag(entity), BIRTH_SURNAME);
    }

    public static String getOriginVillageName(Entity entity) {
        if (entity == null) {
            return "";
        }
        return getStringOrEmpty(getIdentityTag(entity), ORIGIN_VILLAGE_NAME);
    }

    public static String describe(Entity entity) {
        if (entity == null) {
            return "Identity: no loaded villager target";
        }

        VillagerIdentityData data = getIdentity(entity);
        return "Name=" + entity.getName().getString()
                + ", UUID=" + entity.getUUID()
                + ", Origin=" + blankAsUnset(data.originVillageName())
                + ", OriginVillageId=" + (data.originVillageId() == null ? "unset" : data.originVillageId())
                + ", OriginCapitalId=" + (data.originCapitalId() == null ? "unset" : data.originCapitalId())
                + ", OriginSource=" + blankAsUnset(data.originSource())
                + ", BirthSurname=" + blankAsUnset(data.birthSurname())
                + ", CurrentSurname=" + blankAsUnset(data.currentSurname())
                + ", SurnameSource=" + blankAsUnset(data.surnameSource())
                + ", HouseFounded=" + data.houseFounded()
                + ", HouseName=" + blankAsUnset(data.houseName())
                + ", HouseWords=" + blankAsUnset(data.houseWords())
                + ", HouseWordsPersonality=" + blankAsUnset(data.houseWordsPersonality())
                + ", HouseFounder=" + (data.houseFounderId() == null ? "unset" : data.houseFounderId())
                + ", HouseFoundedCapital=" + (data.houseFoundedInCapitalId() == null ? "unset" : data.houseFoundedInCapitalId());
    }

    public static boolean isValidDebugSurname(String surname) {
        surname = normalizeSurname(surname);
        return !surname.isBlank() && !surname.contains("§") && surname.length() <= 40;
    }

    private static boolean canStoreIdentity(ServerLevel level, Entity entity) {
        return level != null
                && entity != null
                && entity.isAlive()
                && !entity.isRemoved()
                && MCAIntegrationBridge.isMCAVillagerEntity(entity);
    }

    private static boolean hasOrigin(CompoundTag identity) {
        return identity.contains(ORIGIN_VILLAGE_ID) && hasNonBlankString(identity, ORIGIN_VILLAGE_NAME);
    }

    private static CompoundTag getIdentityTag(Entity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(IDENTITY_TAG)) {
            CompoundTag identity = new CompoundTag();
            persistent.put(IDENTITY_TAG, identity);
            return identity;
        }
        return persistent.getCompound(IDENTITY_TAG);
    }

    private static void saveIdentityTag(Entity entity, CompoundTag identity) {
        entity.getPersistentData().put(IDENTITY_TAG, identity);
    }

    private static boolean hasNonBlankString(CompoundTag tag, String key) {
        return tag.contains(key) && !tag.getString(key).isBlank();
    }

    private static String getStringOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getString(key) : "";
    }

    private static String normalizeSurname(String surname) {
        return surname == null ? "" : surname.trim().replaceAll("\\s+", " ");
    }

    private static String blankAsUnset(String value) {
        return value == null || value.isBlank() ? "unset" : value;
    }

    private static VillagerIdentityData empty() {
        return new VillagerIdentityData(
                null,
                null,
                "",
                null,
                "",
                "",
                0L,
                "",
                0,
                0,
                0,
                "",
                "",
                "",
                0L,
                false,
                "",
                "",
                "",
                null,
                "",
                0L,
                null,
                ""
        );
    }
}