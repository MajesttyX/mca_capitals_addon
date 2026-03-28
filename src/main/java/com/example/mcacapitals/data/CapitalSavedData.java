package com.example.mcacapitals.data;

import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CapitalSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_data";

    private static final String KEY_CAPITALS = "Capitals";

    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_VILLAGE_ID = "VillageId";
    private static final String KEY_STATE = "State";

    private static final String KEY_SOVEREIGN = "Sovereign";
    private static final String KEY_SOVEREIGN_FEMALE = "SovereignFemale";

    private static final String KEY_CONSORT = "Consort";
    private static final String KEY_CONSORT_FEMALE = "ConsortFemale";

    private static final String KEY_DOWAGER = "Dowager";
    private static final String KEY_DOWAGER_FEMALE = "DowagerFemale";

    private static final String KEY_HEIR = "Heir";
    private static final String KEY_HEIR_FEMALE = "HeirFemale";
    private static final String KEY_HEIR_MODE = "HeirMode";

    private static final String KEY_PLAYER_SOVEREIGN = "PlayerSovereign";
    private static final String KEY_PLAYER_SOVEREIGN_ID = "PlayerSovereignId";
    private static final String KEY_PLAYER_SOVEREIGN_NAME = "PlayerSovereignName";

    private static final String KEY_PLAYER_CONSORT = "PlayerConsort";
    private static final String KEY_PLAYER_CONSORT_ID = "PlayerConsortId";
    private static final String KEY_PLAYER_CONSORT_NAME = "PlayerConsortName";

    private static final String KEY_MONARCHY_REJECTED = "MonarchyRejected";

    private static final String KEY_MOURNING_ACTIVE = "MourningActive";
    private static final String KEY_MOURNING_END_DAY = "MourningEndDay";
    private static final String KEY_MOURNING_ORIGINAL_CLOTHES = "MourningOriginalClothes";

    private static final String KEY_ROYAL_CHILDREN = "RoyalChildren";
    private static final String KEY_ROYAL_CHILD_FEMALE = "RoyalChildFemale";
    private static final String KEY_DISINHERITED_ROYAL_CHILDREN = "DisinheritedRoyalChildren";
    private static final String KEY_LEGITIMIZED_ROYAL_CHILDREN = "LegitimizedRoyalChildren";
    private static final String KEY_LEGITIMIZED_ROYAL_CHILD_FEMALE = "LegitimizedRoyalChildFemale";
    private static final String KEY_ROYAL_SUCCESSION_ORDER = "RoyalSuccessionOrder";

    private static final String KEY_DUKES = "Dukes";
    private static final String KEY_DUKE_FEMALE = "DukeFemale";

    private static final String KEY_LORDS = "Lords";
    private static final String KEY_LORD_FEMALE = "LordFemale";

    private static final String KEY_KNIGHTS = "Knights";
    private static final String KEY_KNIGHT_FEMALE = "KnightFemale";

    private static final String KEY_CHRONICLE_ENTRIES = "ChronicleEntries";

    private static final String KEY_COMMANDER = "Commander";
    private static final String KEY_COMMANDER_FEMALE = "CommanderFemale";
    private static final String KEY_LAST_COMMANDER_RAID_BLESSING_GAME_TIME = "LastCommanderRaidBlessingGameTime";
    private static final String KEY_LAST_COMMANDER_RANDOM_BLESSING_DAY = "LastCommanderRandomBlessingDay";

    private static final String KEY_ROYAL_GUARDS = "RoyalGuards";
    private static final String KEY_ROYAL_GUARD_FEMALE = "RoyalGuardFemale";
    private static final String KEY_DISGRACED_ROYAL_GUARDS = "DisgracedRoyalGuards";
    private static final String KEY_ROYAL_GUARD_LIEGE = "RoyalGuardLiege";
    private static final String KEY_ROYAL_GUARD_PATROLLING = "RoyalGuardPatrolling";
    private static final String KEY_ROYAL_GUARD_PATROL_ANCHORS = "RoyalGuardPatrolAnchors";
    private static final String KEY_ROYAL_GUARD_DUTY_MODES = "RoyalGuardDutyModes";
    private static final String KEY_LAST_ROYAL_GUARD_PROMPT_DAY = "LastRoyalGuardPromptDay";
    private static final String KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER = "PendingPlayerGuardSelectionRequester";

    private static final String KEY_ENTITY_ID = "EntityId";
    private static final String KEY_CLOTHES = "Clothes";

    private static final String KEY_GUARD_ID = "GuardId";
    private static final String KEY_X = "X";
    private static final String KEY_Y = "Y";
    private static final String KEY_Z = "Z";
    private static final String KEY_MODE = "Mode";

    private static final String KEY_ID = "Id";
    private static final String KEY_FLAG = "Flag";

    private final List<CapitalRecord> capitals = new ArrayList<>();

    public List<CapitalRecord> getCapitals() {
        return capitals;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag capitalList = new ListTag();

        for (CapitalRecord capital : capitals) {
            CompoundTag capitalTag = new CompoundTag();

            capitalTag.putUUID(KEY_CAPITAL_ID, capital.getCapitalId());

            if (capital.getVillageId() != null) {
                capitalTag.putInt(KEY_VILLAGE_ID, capital.getVillageId());
            }

            capitalTag.putString(KEY_STATE, capital.getState().name());

            if (capital.getSovereign() != null) {
                capitalTag.putUUID(KEY_SOVEREIGN, capital.getSovereign());
            }
            capitalTag.putBoolean(KEY_SOVEREIGN_FEMALE, capital.isSovereignFemale());

            if (capital.getConsort() != null) {
                capitalTag.putUUID(KEY_CONSORT, capital.getConsort());
            }
            capitalTag.putBoolean(KEY_CONSORT_FEMALE, capital.isConsortFemale());

            if (capital.getDowager() != null) {
                capitalTag.putUUID(KEY_DOWAGER, capital.getDowager());
            }
            capitalTag.putBoolean(KEY_DOWAGER_FEMALE, capital.isDowagerFemale());

            if (capital.getHeir() != null) {
                capitalTag.putUUID(KEY_HEIR, capital.getHeir());
            }
            capitalTag.putBoolean(KEY_HEIR_FEMALE, capital.isHeirFemale());
            capitalTag.putString(KEY_HEIR_MODE, capital.getHeirMode().name());

            capitalTag.putBoolean(KEY_PLAYER_SOVEREIGN, capital.isPlayerSovereign());
            if (capital.getPlayerSovereignId() != null) {
                capitalTag.putUUID(KEY_PLAYER_SOVEREIGN_ID, capital.getPlayerSovereignId());
            }
            if (capital.getPlayerSovereignName() != null) {
                capitalTag.putString(KEY_PLAYER_SOVEREIGN_NAME, capital.getPlayerSovereignName());
            }

            capitalTag.putBoolean(KEY_PLAYER_CONSORT, capital.isPlayerConsort());
            if (capital.getPlayerConsortId() != null) {
                capitalTag.putUUID(KEY_PLAYER_CONSORT_ID, capital.getPlayerConsortId());
            }
            if (capital.getPlayerConsortName() != null) {
                capitalTag.putString(KEY_PLAYER_CONSORT_NAME, capital.getPlayerConsortName());
            }

            capitalTag.putBoolean(KEY_MONARCHY_REJECTED, capital.isMonarchyRejected());

            capitalTag.putBoolean(KEY_MOURNING_ACTIVE, capital.isMourningActive());
            capitalTag.putLong(KEY_MOURNING_END_DAY, capital.getMourningEndDay());

            writeUuidSet(capitalTag, KEY_ROYAL_CHILDREN, capital.getRoyalChildren());
            writeUuidBooleanMap(capitalTag, KEY_ROYAL_CHILD_FEMALE, capital.getRoyalChildFemale());

            writeUuidSet(capitalTag, KEY_DISINHERITED_ROYAL_CHILDREN, capital.getDisinheritedRoyalChildren());
            writeUuidSet(capitalTag, KEY_LEGITIMIZED_ROYAL_CHILDREN, capital.getLegitimizedRoyalChildren());
            writeUuidBooleanMap(capitalTag, KEY_LEGITIMIZED_ROYAL_CHILD_FEMALE, capital.getLegitimizedRoyalChildFemale());
            writeUuidList(capitalTag, KEY_ROYAL_SUCCESSION_ORDER, capital.getRoyalSuccessionOrder());

            writeUuidSet(capitalTag, KEY_DUKES, capital.getDukes());
            writeUuidBooleanMap(capitalTag, KEY_DUKE_FEMALE, capital.getDukeFemale());

            writeUuidSet(capitalTag, KEY_LORDS, capital.getLords());
            writeUuidBooleanMap(capitalTag, KEY_LORD_FEMALE, capital.getLordFemale());

            writeUuidSet(capitalTag, KEY_KNIGHTS, capital.getKnights());
            writeUuidBooleanMap(capitalTag, KEY_KNIGHT_FEMALE, capital.getKnightFemale());

            ListTag chronicleTag = new ListTag();
            for (String entry : capital.getChronicleEntries()) {
                chronicleTag.add(StringTag.valueOf(entry));
            }
            capitalTag.put(KEY_CHRONICLE_ENTRIES, chronicleTag);

            ListTag mourningClothesTag = new ListTag();
            capital.getMourningOriginalClothes().forEach((uuid, clothes) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID(KEY_ENTITY_ID, uuid);
                entryTag.putString(KEY_CLOTHES, clothes == null ? "" : clothes);
                mourningClothesTag.add(entryTag);
            });
            capitalTag.put(KEY_MOURNING_ORIGINAL_CLOTHES, mourningClothesTag);

            if (capital.getCommander() != null) {
                capitalTag.putUUID(KEY_COMMANDER, capital.getCommander());
            }
            capitalTag.putBoolean(KEY_COMMANDER_FEMALE, capital.isCommanderFemale());
            capitalTag.putLong(KEY_LAST_COMMANDER_RAID_BLESSING_GAME_TIME, capital.getLastCommanderRaidBlessingGameTime());
            capitalTag.putLong(KEY_LAST_COMMANDER_RANDOM_BLESSING_DAY, capital.getLastCommanderRandomBlessingDay());

            writeUuidSet(capitalTag, KEY_ROYAL_GUARDS, capital.getRoyalGuards());
            writeUuidBooleanMap(capitalTag, KEY_ROYAL_GUARD_FEMALE, capital.getRoyalGuardFemale());
            writeUuidSet(capitalTag, KEY_DISGRACED_ROYAL_GUARDS, capital.getDisgracedRoyalGuards());

            if (capital.getRoyalGuardLiege() != null) {
                capitalTag.putUUID(KEY_ROYAL_GUARD_LIEGE, capital.getRoyalGuardLiege());
            }

            writeUuidSet(capitalTag, KEY_ROYAL_GUARD_PATROLLING, capital.getRoyalGuardPatrolling());

            ListTag guardAnchorsTag = new ListTag();
            capital.getRoyalGuardPatrolAnchors().forEach((uuid, anchor) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID(KEY_GUARD_ID, uuid);
                entryTag.putInt(KEY_X, anchor.getX());
                entryTag.putInt(KEY_Y, anchor.getY());
                entryTag.putInt(KEY_Z, anchor.getZ());
                guardAnchorsTag.add(entryTag);
            });
            capitalTag.put(KEY_ROYAL_GUARD_PATROL_ANCHORS, guardAnchorsTag);

            ListTag guardModesTag = new ListTag();
            capital.getRoyalGuardDutyModes().forEach((uuid, mode) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID(KEY_GUARD_ID, uuid);
                entryTag.putString(KEY_MODE, mode.name());
                guardModesTag.add(entryTag);
            });
            capitalTag.put(KEY_ROYAL_GUARD_DUTY_MODES, guardModesTag);

            capitalTag.putLong(KEY_LAST_ROYAL_GUARD_PROMPT_DAY, capital.getLastRoyalGuardPromptDay());

            if (capital.getPendingPlayerGuardSelectionRequester() != null) {
                capitalTag.putUUID(KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER, capital.getPendingPlayerGuardSelectionRequester());
            }

            capitalList.add(capitalTag);
        }

        tag.put(KEY_CAPITALS, capitalList);
        return tag;
    }

    public static CapitalSavedData load(CompoundTag tag) {
        CapitalSavedData data = new CapitalSavedData();
        ListTag capitalList = tag.getList(KEY_CAPITALS, Tag.TAG_COMPOUND);

        for (Tag baseTag : capitalList) {
            CompoundTag capitalTag = (CompoundTag) baseTag;

            UUID capitalId = capitalTag.getUUID(KEY_CAPITAL_ID);
            Integer villageId = capitalTag.contains(KEY_VILLAGE_ID) ? capitalTag.getInt(KEY_VILLAGE_ID) : null;

            CapitalRecord capital = new CapitalRecord(capitalId, villageId);

            if (capitalTag.contains(KEY_STATE)) {
                try {
                    capital.setState(CapitalState.valueOf(capitalTag.getString(KEY_STATE)));
                } catch (IllegalArgumentException ignored) {
                    capital.setState(CapitalState.ACTIVE);
                }
            }

            if (capitalTag.contains(KEY_SOVEREIGN)) {
                capital.setSovereign(capitalTag.getUUID(KEY_SOVEREIGN));
            }
            capital.setSovereignFemale(capitalTag.getBoolean(KEY_SOVEREIGN_FEMALE));

            if (capitalTag.contains(KEY_CONSORT)) {
                capital.setConsort(capitalTag.getUUID(KEY_CONSORT));
            }
            capital.setConsortFemale(capitalTag.getBoolean(KEY_CONSORT_FEMALE));

            if (capitalTag.contains(KEY_DOWAGER)) {
                capital.setDowager(capitalTag.getUUID(KEY_DOWAGER));
            }
            capital.setDowagerFemale(capitalTag.getBoolean(KEY_DOWAGER_FEMALE));

            if (capitalTag.contains(KEY_HEIR)) {
                capital.setHeir(capitalTag.getUUID(KEY_HEIR));
            }
            capital.setHeirFemale(capitalTag.getBoolean(KEY_HEIR_FEMALE));
            if (capitalTag.contains(KEY_HEIR_MODE)) {
                try {
                    capital.setHeirMode(CapitalRecord.HeirMode.valueOf(capitalTag.getString(KEY_HEIR_MODE)));
                } catch (IllegalArgumentException ignored) {
                    capital.setHeirMode(CapitalRecord.HeirMode.DYNASTIC);
                }
            }

            capital.setPlayerSovereign(capitalTag.getBoolean(KEY_PLAYER_SOVEREIGN));
            if (capitalTag.contains(KEY_PLAYER_SOVEREIGN_ID)) {
                capital.setPlayerSovereignId(capitalTag.getUUID(KEY_PLAYER_SOVEREIGN_ID));
            }
            if (capitalTag.contains(KEY_PLAYER_SOVEREIGN_NAME)) {
                capital.setPlayerSovereignName(capitalTag.getString(KEY_PLAYER_SOVEREIGN_NAME));
            }

            capital.setPlayerConsort(capitalTag.getBoolean(KEY_PLAYER_CONSORT));
            if (capitalTag.contains(KEY_PLAYER_CONSORT_ID)) {
                capital.setPlayerConsortId(capitalTag.getUUID(KEY_PLAYER_CONSORT_ID));
            }
            if (capitalTag.contains(KEY_PLAYER_CONSORT_NAME)) {
                capital.setPlayerConsortName(capitalTag.getString(KEY_PLAYER_CONSORT_NAME));
            }

            capital.setMonarchyRejected(capitalTag.getBoolean(KEY_MONARCHY_REJECTED));

            capital.setMourningActive(capitalTag.getBoolean(KEY_MOURNING_ACTIVE));
            capital.setMourningEndDay(capitalTag.getLong(KEY_MOURNING_END_DAY));

            readUuidSet(capitalTag.getList(KEY_ROYAL_CHILDREN, Tag.TAG_STRING), capital.getRoyalChildren());
            readUuidBooleanMap(capitalTag.getList(KEY_ROYAL_CHILD_FEMALE, Tag.TAG_COMPOUND), capital.getRoyalChildFemale());

            readUuidSet(capitalTag.getList(KEY_DISINHERITED_ROYAL_CHILDREN, Tag.TAG_STRING), capital.getDisinheritedRoyalChildren());
            readUuidSet(capitalTag.getList(KEY_LEGITIMIZED_ROYAL_CHILDREN, Tag.TAG_STRING), capital.getLegitimizedRoyalChildren());
            readUuidBooleanMap(capitalTag.getList(KEY_LEGITIMIZED_ROYAL_CHILD_FEMALE, Tag.TAG_COMPOUND), capital.getLegitimizedRoyalChildFemale());
            readUuidList(capitalTag.getList(KEY_ROYAL_SUCCESSION_ORDER, Tag.TAG_STRING), capital.getRoyalSuccessionOrder());

            readUuidSet(capitalTag.getList(KEY_DUKES, Tag.TAG_STRING), capital.getDukes());
            readUuidBooleanMap(capitalTag.getList(KEY_DUKE_FEMALE, Tag.TAG_COMPOUND), capital.getDukeFemale());

            readUuidSet(capitalTag.getList(KEY_LORDS, Tag.TAG_STRING), capital.getLords());
            readUuidBooleanMap(capitalTag.getList(KEY_LORD_FEMALE, Tag.TAG_COMPOUND), capital.getLordFemale());

            readUuidSet(capitalTag.getList(KEY_KNIGHTS, Tag.TAG_STRING), capital.getKnights());
            readUuidBooleanMap(capitalTag.getList(KEY_KNIGHT_FEMALE, Tag.TAG_COMPOUND), capital.getKnightFemale());

            ListTag chronicleTag = capitalTag.getList(KEY_CHRONICLE_ENTRIES, Tag.TAG_STRING);
            for (Tag entryTag : chronicleTag) {
                capital.getChronicleEntries().add(entryTag.getAsString());
            }

            ListTag mourningClothesTag = capitalTag.getList(KEY_MOURNING_ORIGINAL_CLOTHES, Tag.TAG_COMPOUND);
            for (Tag entryBase : mourningClothesTag) {
                CompoundTag entryTag = (CompoundTag) entryBase;
                UUID entityId = entryTag.getUUID(KEY_ENTITY_ID);
                String clothes = entryTag.getString(KEY_CLOTHES);
                capital.getMourningOriginalClothes().put(entityId, clothes);
            }

            if (capitalTag.contains(KEY_COMMANDER)) {
                capital.setCommander(capitalTag.getUUID(KEY_COMMANDER));
            }
            capital.setCommanderFemale(capitalTag.getBoolean(KEY_COMMANDER_FEMALE));
            capital.setLastCommanderRaidBlessingGameTime(capitalTag.getLong(KEY_LAST_COMMANDER_RAID_BLESSING_GAME_TIME));
            capital.setLastCommanderRandomBlessingDay(capitalTag.getLong(KEY_LAST_COMMANDER_RANDOM_BLESSING_DAY));

            readUuidSet(capitalTag.getList(KEY_ROYAL_GUARDS, Tag.TAG_STRING), capital.getRoyalGuards());
            readUuidBooleanMap(capitalTag.getList(KEY_ROYAL_GUARD_FEMALE, Tag.TAG_COMPOUND), capital.getRoyalGuardFemale());
            readUuidSet(capitalTag.getList(KEY_DISGRACED_ROYAL_GUARDS, Tag.TAG_STRING), capital.getDisgracedRoyalGuards());

            if (capitalTag.contains(KEY_ROYAL_GUARD_LIEGE)) {
                capital.setRoyalGuardLiege(capitalTag.getUUID(KEY_ROYAL_GUARD_LIEGE));
            }

            readUuidSet(capitalTag.getList(KEY_ROYAL_GUARD_PATROLLING, Tag.TAG_STRING), capital.getRoyalGuardPatrolling());

            ListTag guardAnchorsTag = capitalTag.getList(KEY_ROYAL_GUARD_PATROL_ANCHORS, Tag.TAG_COMPOUND);
            for (Tag entryBase : guardAnchorsTag) {
                CompoundTag entryTag = (CompoundTag) entryBase;
                UUID guardId = entryTag.getUUID(KEY_GUARD_ID);
                capital.setRoyalGuardPatrolAnchor(
                        guardId,
                        new BlockPos(entryTag.getInt(KEY_X), entryTag.getInt(KEY_Y), entryTag.getInt(KEY_Z))
                );
            }

            ListTag guardModesTag = capitalTag.getList(KEY_ROYAL_GUARD_DUTY_MODES, Tag.TAG_COMPOUND);
            for (Tag entryBase : guardModesTag) {
                CompoundTag entryTag = (CompoundTag) entryBase;
                UUID guardId = entryTag.getUUID(KEY_GUARD_ID);
                try {
                    capital.setRoyalGuardDutyMode(guardId, CapitalRecord.GuardDutyMode.valueOf(entryTag.getString(KEY_MODE)));
                } catch (IllegalArgumentException ignored) {
                    capital.setRoyalGuardDutyMode(guardId, CapitalRecord.GuardDutyMode.FOLLOW_SOVEREIGN);
                }
            }

            capital.setLastRoyalGuardPromptDay(capitalTag.getLong(KEY_LAST_ROYAL_GUARD_PROMPT_DAY));

            if (capitalTag.contains(KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER)) {
                capital.setPendingPlayerGuardSelectionRequester(capitalTag.getUUID(KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER));
            }

            data.getCapitals().add(capital);
        }

        return data;
    }

    private static void writeUuidSet(CompoundTag parent, String key, Iterable<UUID> values) {
        ListTag list = new ListTag();
        for (UUID id : values) {
            list.add(StringTag.valueOf(id.toString()));
        }
        parent.put(key, list);
    }

    private static void writeUuidList(CompoundTag parent, String key, List<UUID> values) {
        ListTag list = new ListTag();
        for (UUID id : values) {
            list.add(StringTag.valueOf(id.toString()));
        }
        parent.put(key, list);
    }

    private static void writeUuidBooleanMap(CompoundTag parent, String key, Map<UUID, Boolean> values) {
        ListTag list = new ListTag();
        values.forEach((id, flag) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(KEY_ID, id);
            tag.putBoolean(KEY_FLAG, flag != null && flag);
            list.add(tag);
        });
        parent.put(key, list);
    }

    private static void readUuidSet(ListTag listTag, java.util.Set<UUID> target) {
        for (Tag tag : listTag) {
            try {
                target.add(UUID.fromString(tag.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void readUuidList(ListTag listTag, List<UUID> target) {
        for (Tag tag : listTag) {
            try {
                target.add(UUID.fromString(tag.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void readUuidBooleanMap(ListTag listTag, Map<UUID, Boolean> target) {
        for (Tag base : listTag) {
            if (!(base instanceof CompoundTag tag)) {
                continue;
            }
            try {
                target.put(tag.getUUID(KEY_ID), tag.getBoolean(KEY_FLAG));
            } catch (Exception ignored) {
            }
        }
    }
}