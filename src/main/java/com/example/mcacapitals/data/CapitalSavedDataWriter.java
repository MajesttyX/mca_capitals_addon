package com.example.mcacapitals.data;

import com.example.mcacapitals.capital.CapitalRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CapitalSavedDataWriter {

    private CapitalSavedDataWriter() {
    }

    static CompoundTag saveCapitals(CompoundTag tag, List<CapitalRecord> capitals) {
        ListTag capitalList = new ListTag();

        for (CapitalRecord capital : capitals) {
            CompoundTag capitalTag = new CompoundTag();

            capitalTag.putUUID(CapitalSavedData.KEY_CAPITAL_ID, capital.getCapitalId());

            if (capital.getVillageId() != null) {
                capitalTag.putInt(CapitalSavedData.KEY_VILLAGE_ID, capital.getVillageId());
            }

            capitalTag.putString(CapitalSavedData.KEY_STATE, capital.getState().name());

            if (capital.getSovereign() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_SOVEREIGN, capital.getSovereign());
            }
            capitalTag.putBoolean(CapitalSavedData.KEY_SOVEREIGN_FEMALE, capital.isSovereignFemale());

            if (capital.getConsort() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_CONSORT, capital.getConsort());
            }
            capitalTag.putBoolean(CapitalSavedData.KEY_CONSORT_FEMALE, capital.isConsortFemale());

            if (capital.getDowager() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_DOWAGER, capital.getDowager());
            }
            capitalTag.putBoolean(CapitalSavedData.KEY_DOWAGER_FEMALE, capital.isDowagerFemale());

            if (capital.getHeir() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_HEIR, capital.getHeir());
            }
            capitalTag.putBoolean(CapitalSavedData.KEY_HEIR_FEMALE, capital.isHeirFemale());
            capitalTag.putString(CapitalSavedData.KEY_HEIR_MODE, capital.getHeirMode().name());

            capitalTag.putBoolean(CapitalSavedData.KEY_PLAYER_SOVEREIGN, capital.isPlayerSovereign());
            if (capital.getPlayerSovereignId() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_PLAYER_SOVEREIGN_ID, capital.getPlayerSovereignId());
            }
            if (capital.getPlayerSovereignName() != null) {
                capitalTag.putString(CapitalSavedData.KEY_PLAYER_SOVEREIGN_NAME, capital.getPlayerSovereignName());
            }

            capitalTag.putBoolean(CapitalSavedData.KEY_PLAYER_CONSORT, capital.isPlayerConsort());
            if (capital.getPlayerConsortId() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_PLAYER_CONSORT_ID, capital.getPlayerConsortId());
            }
            if (capital.getPlayerConsortName() != null) {
                capitalTag.putString(CapitalSavedData.KEY_PLAYER_CONSORT_NAME, capital.getPlayerConsortName());
            }

            capitalTag.putBoolean(CapitalSavedData.KEY_MONARCHY_REJECTED, capital.isMonarchyRejected());

            capitalTag.putBoolean(CapitalSavedData.KEY_MOURNING_ACTIVE, capital.isMourningActive());
            capitalTag.putLong(CapitalSavedData.KEY_MOURNING_END_DAY, capital.getMourningEndDay());

            writeUuidSet(capitalTag, CapitalSavedData.KEY_ROYAL_CHILDREN, capital.getRoyalChildren());
            writeUuidBooleanMap(capitalTag, CapitalSavedData.KEY_ROYAL_CHILD_FEMALE, capital.getRoyalChildFemale());
            writeUuidSet(capitalTag, CapitalSavedData.KEY_ROYAL_HOUSEHOLD, capital.getRoyalHousehold());

            writeUuidSet(capitalTag, CapitalSavedData.KEY_DISINHERITED_ROYAL_CHILDREN, capital.getDisinheritedRoyalChildren());
            writeUuidSet(capitalTag, CapitalSavedData.KEY_LEGITIMIZED_ROYAL_CHILDREN, capital.getLegitimizedRoyalChildren());
            writeUuidBooleanMap(capitalTag, CapitalSavedData.KEY_LEGITIMIZED_ROYAL_CHILD_FEMALE, capital.getLegitimizedRoyalChildFemale());
            writeUuidList(capitalTag, CapitalSavedData.KEY_ROYAL_SUCCESSION_ORDER, capital.getRoyalSuccessionOrder());

            writeUuidSet(capitalTag, CapitalSavedData.KEY_DUKES, capital.getDukes());
            writeUuidBooleanMap(capitalTag, CapitalSavedData.KEY_DUKE_FEMALE, capital.getDukeFemale());

            writeUuidSet(capitalTag, CapitalSavedData.KEY_LORDS, capital.getLords());
            writeUuidBooleanMap(capitalTag, CapitalSavedData.KEY_LORD_FEMALE, capital.getLordFemale());

            writeUuidSet(capitalTag, CapitalSavedData.KEY_KNIGHTS, capital.getKnights());
            writeUuidBooleanMap(capitalTag, CapitalSavedData.KEY_KNIGHT_FEMALE, capital.getKnightFemale());

            ListTag chronicleTag = new ListTag();
            for (String entry : capital.getChronicleEntries()) {
                chronicleTag.add(StringTag.valueOf(entry));
            }
            capitalTag.put(CapitalSavedData.KEY_CHRONICLE_ENTRIES, chronicleTag);

            ListTag mourningClothesTag = new ListTag();
            capital.getMourningOriginalClothes().forEach((uuid, clothes) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID(CapitalSavedData.KEY_ENTITY_ID, uuid);
                entryTag.putString(CapitalSavedData.KEY_CLOTHES, clothes == null ? "" : clothes);
                mourningClothesTag.add(entryTag);
            });
            capitalTag.put(CapitalSavedData.KEY_MOURNING_ORIGINAL_CLOTHES, mourningClothesTag);

            if (capital.getCommander() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_COMMANDER, capital.getCommander());
            }
            capitalTag.putBoolean(CapitalSavedData.KEY_COMMANDER_FEMALE, capital.isCommanderFemale());
            capitalTag.putLong(CapitalSavedData.KEY_LAST_COMMANDER_RAID_BLESSING_GAME_TIME, capital.getLastCommanderRaidBlessingGameTime());
            capitalTag.putLong(CapitalSavedData.KEY_LAST_COMMANDER_RANDOM_BLESSING_DAY, capital.getLastCommanderRandomBlessingDay());

            writeUuidSet(capitalTag, CapitalSavedData.KEY_ROYAL_GUARDS, capital.getRoyalGuards());
            writeUuidBooleanMap(capitalTag, CapitalSavedData.KEY_ROYAL_GUARD_FEMALE, capital.getRoyalGuardFemale());
            writeUuidSet(capitalTag, CapitalSavedData.KEY_DISGRACED_ROYAL_GUARDS, capital.getDisgracedRoyalGuards());

            if (capital.getRoyalGuardLiege() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_ROYAL_GUARD_LIEGE, capital.getRoyalGuardLiege());
            }

            writeUuidSet(capitalTag, CapitalSavedData.KEY_ROYAL_GUARD_PATROLLING, capital.getRoyalGuardPatrolling());

            ListTag guardAnchorsTag = new ListTag();
            capital.getRoyalGuardPatrolAnchors().forEach((uuid, anchor) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID(CapitalSavedData.KEY_GUARD_ID, uuid);
                entryTag.putInt(CapitalSavedData.KEY_X, anchor.getX());
                entryTag.putInt(CapitalSavedData.KEY_Y, anchor.getY());
                entryTag.putInt(CapitalSavedData.KEY_Z, anchor.getZ());
                guardAnchorsTag.add(entryTag);
            });
            capitalTag.put(CapitalSavedData.KEY_ROYAL_GUARD_PATROL_ANCHORS, guardAnchorsTag);

            ListTag guardModesTag = new ListTag();
            capital.getRoyalGuardDutyModes().forEach((uuid, mode) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID(CapitalSavedData.KEY_GUARD_ID, uuid);
                entryTag.putString(CapitalSavedData.KEY_MODE, mode.name());
                guardModesTag.add(entryTag);
            });
            capitalTag.put(CapitalSavedData.KEY_ROYAL_GUARD_DUTY_MODES, guardModesTag);

            capitalTag.putLong(CapitalSavedData.KEY_LAST_ROYAL_GUARD_PROMPT_DAY, capital.getLastRoyalGuardPromptDay());

            if (capital.getPendingPlayerGuardSelectionRequester() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER, capital.getPendingPlayerGuardSelectionRequester());
            }

            capitalList.add(capitalTag);
        }

        tag.put(CapitalSavedData.KEY_CAPITALS, capitalList);
        return tag;
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
            tag.putUUID(CapitalSavedData.KEY_ID, id);
            tag.putBoolean(CapitalSavedData.KEY_FLAG, flag != null && flag);
            list.add(tag);
        });
        parent.put(key, list);
    }
}