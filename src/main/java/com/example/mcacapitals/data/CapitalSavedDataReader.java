package com.example.mcacapitals.data;

import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

final class CapitalSavedDataReader {

    private CapitalSavedDataReader() {
    }

    static CapitalSavedData loadCapitals(CompoundTag tag) {
        CapitalSavedData data = new CapitalSavedData();

        ListTag capitalList = tag.getList(CapitalSavedData.KEY_CAPITALS, Tag.TAG_COMPOUND);
        for (Tag base : capitalList) {
            if (!(base instanceof CompoundTag capitalTag)) {
                continue;
            }

            UUID capitalId;
            try {
                capitalId = capitalTag.getUUID(CapitalSavedData.KEY_CAPITAL_ID);
            } catch (Exception ignored) {
                continue;
            }

            Integer villageId = capitalTag.contains(CapitalSavedData.KEY_VILLAGE_ID, Tag.TAG_INT)
                    ? capitalTag.getInt(CapitalSavedData.KEY_VILLAGE_ID)
                    : null;

            UUID sovereign = capitalTag.hasUUID(CapitalSavedData.KEY_SOVEREIGN)
                    ? capitalTag.getUUID(CapitalSavedData.KEY_SOVEREIGN)
                    : null;

            boolean sovereignFemale = capitalTag.getBoolean(CapitalSavedData.KEY_SOVEREIGN_FEMALE);

            CapitalRecord capital = new CapitalRecord(capitalId, villageId, sovereign, sovereignFemale);

            if (capitalTag.contains(CapitalSavedData.KEY_STATE, Tag.TAG_STRING)) {
                try {
                    capital.setState(CapitalState.valueOf(capitalTag.getString(CapitalSavedData.KEY_STATE)));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (capitalTag.hasUUID(CapitalSavedData.KEY_CONSORT)) {
                capital.setConsort(capitalTag.getUUID(CapitalSavedData.KEY_CONSORT));
            }
            capital.setConsortFemale(capitalTag.getBoolean(CapitalSavedData.KEY_CONSORT_FEMALE));

            if (capitalTag.hasUUID(CapitalSavedData.KEY_DOWAGER)) {
                capital.setDowager(capitalTag.getUUID(CapitalSavedData.KEY_DOWAGER));
            }
            capital.setDowagerFemale(capitalTag.getBoolean(CapitalSavedData.KEY_DOWAGER_FEMALE));

            if (capitalTag.hasUUID(CapitalSavedData.KEY_HEIR)) {
                capital.setHeir(capitalTag.getUUID(CapitalSavedData.KEY_HEIR));
            }
            capital.setHeirFemale(capitalTag.getBoolean(CapitalSavedData.KEY_HEIR_FEMALE));

            if (capitalTag.contains(CapitalSavedData.KEY_HEIR_MODE, Tag.TAG_STRING)) {
                try {
                    capital.setHeirMode(CapitalRecord.HeirMode.valueOf(capitalTag.getString(CapitalSavedData.KEY_HEIR_MODE)));
                } catch (IllegalArgumentException ignored) {
                }
            }

            capital.setPlayerSovereign(capitalTag.getBoolean(CapitalSavedData.KEY_PLAYER_SOVEREIGN));
            if (capitalTag.hasUUID(CapitalSavedData.KEY_PLAYER_SOVEREIGN_ID)) {
                capital.setPlayerSovereignId(capitalTag.getUUID(CapitalSavedData.KEY_PLAYER_SOVEREIGN_ID));
            }
            if (capitalTag.contains(CapitalSavedData.KEY_PLAYER_SOVEREIGN_NAME, Tag.TAG_STRING)) {
                capital.setPlayerSovereignName(capitalTag.getString(CapitalSavedData.KEY_PLAYER_SOVEREIGN_NAME));
            }

            capital.setPlayerConsort(capitalTag.getBoolean(CapitalSavedData.KEY_PLAYER_CONSORT));
            if (capitalTag.hasUUID(CapitalSavedData.KEY_PLAYER_CONSORT_ID)) {
                capital.setPlayerConsortId(capitalTag.getUUID(CapitalSavedData.KEY_PLAYER_CONSORT_ID));
            }
            if (capitalTag.contains(CapitalSavedData.KEY_PLAYER_CONSORT_NAME, Tag.TAG_STRING)) {
                capital.setPlayerConsortName(capitalTag.getString(CapitalSavedData.KEY_PLAYER_CONSORT_NAME));
            }

            capital.setMonarchyRejected(capitalTag.getBoolean(CapitalSavedData.KEY_MONARCHY_REJECTED));

            capital.setMourningActive(capitalTag.getBoolean(CapitalSavedData.KEY_MOURNING_ACTIVE));
            capital.setMourningEndDay(capitalTag.getLong(CapitalSavedData.KEY_MOURNING_END_DAY));

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_ROYAL_CHILDREN, Tag.TAG_STRING), capital.getRoyalChildren());
            readUuidBooleanMap(capitalTag.getList(CapitalSavedData.KEY_ROYAL_CHILD_FEMALE, Tag.TAG_COMPOUND), capital.getRoyalChildFemale());
            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_ROYAL_HOUSEHOLD, Tag.TAG_STRING), capital.getRoyalHousehold());

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_DISINHERITED_ROYAL_CHILDREN, Tag.TAG_STRING), capital.getDisinheritedRoyalChildren());
            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_LEGITIMIZED_ROYAL_CHILDREN, Tag.TAG_STRING), capital.getLegitimizedRoyalChildren());
            readUuidBooleanMap(capitalTag.getList(CapitalSavedData.KEY_LEGITIMIZED_ROYAL_CHILD_FEMALE, Tag.TAG_COMPOUND), capital.getLegitimizedRoyalChildFemale());
            readUuidList(capitalTag.getList(CapitalSavedData.KEY_ROYAL_SUCCESSION_ORDER, Tag.TAG_STRING), capital.getRoyalSuccessionOrder());

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_DUKES, Tag.TAG_STRING), capital.getDukes());
            readUuidBooleanMap(capitalTag.getList(CapitalSavedData.KEY_DUKE_FEMALE, Tag.TAG_COMPOUND), capital.getDukeFemale());

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_LORDS, Tag.TAG_STRING), capital.getLords());
            readUuidBooleanMap(capitalTag.getList(CapitalSavedData.KEY_LORD_FEMALE, Tag.TAG_COMPOUND), capital.getLordFemale());

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_KNIGHTS, Tag.TAG_STRING), capital.getKnights());
            readUuidBooleanMap(capitalTag.getList(CapitalSavedData.KEY_KNIGHT_FEMALE, Tag.TAG_COMPOUND), capital.getKnightFemale());

            ListTag chronicleTag = capitalTag.getList(CapitalSavedData.KEY_CHRONICLE_ENTRIES, Tag.TAG_STRING);
            for (Tag entry : chronicleTag) {
                capital.getChronicleEntries().add(entry.getAsString());
            }

            ListTag mourningClothesTag = capitalTag.getList(CapitalSavedData.KEY_MOURNING_ORIGINAL_CLOTHES, Tag.TAG_COMPOUND);
            for (Tag entryBase : mourningClothesTag) {
                if (!(entryBase instanceof CompoundTag entryTag)) {
                    continue;
                }
                try {
                    UUID id = entryTag.getUUID(CapitalSavedData.KEY_ENTITY_ID);
                    String clothes = entryTag.getString(CapitalSavedData.KEY_CLOTHES);
                    capital.getMourningOriginalClothes().put(id, clothes);
                } catch (Exception ignored) {
                }
            }

            if (capitalTag.hasUUID(CapitalSavedData.KEY_COMMANDER)) {
                capital.setCommander(capitalTag.getUUID(CapitalSavedData.KEY_COMMANDER));
            }
            capital.setCommanderFemale(capitalTag.getBoolean(CapitalSavedData.KEY_COMMANDER_FEMALE));
            capital.setLastCommanderRaidBlessingGameTime(capitalTag.getLong(CapitalSavedData.KEY_LAST_COMMANDER_RAID_BLESSING_GAME_TIME));
            capital.setLastCommanderRandomBlessingDay(capitalTag.getLong(CapitalSavedData.KEY_LAST_COMMANDER_RANDOM_BLESSING_DAY));

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARDS, Tag.TAG_STRING), capital.getRoyalGuards());
            readUuidBooleanMap(capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARD_FEMALE, Tag.TAG_COMPOUND), capital.getRoyalGuardFemale());
            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_DISGRACED_ROYAL_GUARDS, Tag.TAG_STRING), capital.getDisgracedRoyalGuards());

            if (capitalTag.hasUUID(CapitalSavedData.KEY_ROYAL_GUARD_LIEGE)) {
                capital.setRoyalGuardLiege(capitalTag.getUUID(CapitalSavedData.KEY_ROYAL_GUARD_LIEGE));
            }

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARD_PATROLLING, Tag.TAG_STRING), capital.getRoyalGuardPatrolling());

            ListTag guardAnchorsTag = capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARD_PATROL_ANCHORS, Tag.TAG_COMPOUND);
            for (Tag entryBase : guardAnchorsTag) {
                if (!(entryBase instanceof CompoundTag entryTag)) {
                    continue;
                }
                try {
                    UUID guardId = entryTag.getUUID(CapitalSavedData.KEY_GUARD_ID);
                    BlockPos pos = new BlockPos(
                            entryTag.getInt(CapitalSavedData.KEY_X),
                            entryTag.getInt(CapitalSavedData.KEY_Y),
                            entryTag.getInt(CapitalSavedData.KEY_Z)
                    );
                    capital.getRoyalGuardPatrolAnchors().put(guardId, pos);
                } catch (Exception ignored) {
                }
            }

            ListTag guardModesTag = capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARD_DUTY_MODES, Tag.TAG_COMPOUND);
            for (Tag entryBase : guardModesTag) {
                if (!(entryBase instanceof CompoundTag entryTag)) {
                    continue;
                }
                try {
                    UUID guardId = entryTag.getUUID(CapitalSavedData.KEY_GUARD_ID);
                    CapitalRecord.GuardDutyMode mode = CapitalRecord.GuardDutyMode.valueOf(entryTag.getString(CapitalSavedData.KEY_MODE));
                    capital.getRoyalGuardDutyModes().put(guardId, mode);
                } catch (Exception ignored) {
                }
            }

            capital.setLastRoyalGuardPromptDay(capitalTag.getLong(CapitalSavedData.KEY_LAST_ROYAL_GUARD_PROMPT_DAY));

            if (capitalTag.hasUUID(CapitalSavedData.KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER)) {
                capital.setPendingPlayerGuardSelectionRequester(capitalTag.getUUID(CapitalSavedData.KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER));
            }

            data.getCapitals().add(capital);
        }

        return data;
    }

    private static void readUuidSet(ListTag listTag, java.util.Set<UUID> target) {
        for (Tag tag : listTag) {
            try {
                target.add(UUID.fromString(tag.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void readUuidList(ListTag listTag, java.util.List<UUID> target) {
        for (Tag tag : listTag) {
            try {
                target.add(UUID.fromString(tag.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void readUuidBooleanMap(ListTag listTag, java.util.Map<UUID, Boolean> target) {
        for (Tag base : listTag) {
            if (!(base instanceof CompoundTag tag)) {
                continue;
            }
            try {
                target.put(tag.getUUID(CapitalSavedData.KEY_ID), tag.getBoolean(CapitalSavedData.KEY_FLAG));
            } catch (Exception ignored) {
            }
        }
    }
}