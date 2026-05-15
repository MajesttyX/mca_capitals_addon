package com.example.mcacapitals.data;

import com.example.mcacapitals.capital.CapitalRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalSavedDataWriter {

    private CapitalSavedDataWriter() {
    }

    static CompoundTag saveCapitals(CompoundTag tag, List<CapitalRecord> capitals) {
        ListTag capitalsList = new ListTag();

        for (CapitalRecord capital : capitals) {
            CompoundTag capitalTag = new CompoundTag();

            capitalTag.putUUID(CapitalSavedData.KEY_CAPITAL_ID, capital.getCapitalId());

            if (capital.getVillageId() != null) {
                capitalTag.putInt(CapitalSavedData.KEY_VILLAGE_ID, capital.getVillageId());
            }

            if (capital.getState() != null) {
                capitalTag.putString(CapitalSavedData.KEY_STATE, capital.getState().name());
            }

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

            capitalTag.put(CapitalSavedData.KEY_ROYAL_CHILDREN, writeUuidSet(capital.getRoyalChildren()));
            capitalTag.put(CapitalSavedData.KEY_ROYAL_CHILD_FEMALE, writeBooleanMap(capital.getRoyalChildFemale()));
            capitalTag.put(CapitalSavedData.KEY_ROYAL_HOUSEHOLD, writeUuidSet(capital.getRoyalHousehold()));
            capitalTag.put(CapitalSavedData.KEY_DISINHERITED_ROYAL_CHILDREN, writeUuidSet(capital.getDisinheritedRoyalChildren()));
            capitalTag.put(CapitalSavedData.KEY_LEGITIMIZED_ROYAL_CHILDREN, writeUuidSet(capital.getLegitimizedRoyalChildren()));
            capitalTag.put(CapitalSavedData.KEY_LEGITIMIZED_ROYAL_CHILD_FEMALE, writeBooleanMap(capital.getLegitimizedRoyalChildFemale()));
            capitalTag.put(CapitalSavedData.KEY_ROYAL_SUCCESSION_ORDER, writeUuidList(capital.getRoyalSuccessionOrder()));

            capitalTag.put(CapitalSavedData.KEY_DUKES, writeUuidSet(capital.getDukes()));
            capitalTag.put(CapitalSavedData.KEY_DUKE_FEMALE, writeBooleanMap(capital.getDukeFemale()));

            capitalTag.put(CapitalSavedData.KEY_DOWAGER_PRINCES, writeUuidMap(capital.getDowagerPrinceSources()));
            capitalTag.put(CapitalSavedData.KEY_DOWAGER_PRINCE_FEMALE, writeBooleanMap(capital.getDowagerPrinceFemale()));

            capitalTag.put(CapitalSavedData.KEY_DOWAGER_DUKES, writeUuidMap(capital.getDowagerDukeSources()));
            capitalTag.put(CapitalSavedData.KEY_DOWAGER_DUKE_FEMALE, writeBooleanMap(capital.getDowagerDukeFemale()));

            capitalTag.put(CapitalSavedData.KEY_LORDS, writeUuidSet(capital.getLords()));
            capitalTag.put(CapitalSavedData.KEY_LORD_FEMALE, writeBooleanMap(capital.getLordFemale()));

            capitalTag.put(CapitalSavedData.KEY_KNIGHTS, writeUuidSet(capital.getKnights()));
            capitalTag.put(CapitalSavedData.KEY_KNIGHT_FEMALE, writeBooleanMap(capital.getKnightFemale()));

            capitalTag.put(CapitalSavedData.KEY_CHRONICLE_ENTRIES, writeStringList(capital.getChronicleEntries()));
            capitalTag.put(CapitalSavedData.KEY_MOURNING_ORIGINAL_CLOTHES, writeStringMap(capital.getMourningOriginalClothes()));

            capitalTag.put(CapitalSavedData.KEY_ROYAL_GUARDS, writeUuidSet(capital.getRoyalGuards()));
            capitalTag.put(CapitalSavedData.KEY_ROYAL_GUARD_FEMALE, writeBooleanMap(capital.getRoyalGuardFemale()));
            capitalTag.put(CapitalSavedData.KEY_DISGRACED_ROYAL_GUARDS, writeUuidSet(capital.getDisgracedRoyalGuards()));
            if (capital.getRoyalGuardLiege() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_ROYAL_GUARD_LIEGE, capital.getRoyalGuardLiege());
            }
            capitalTag.put(CapitalSavedData.KEY_ROYAL_GUARD_PATROLLING, writeUuidSet(capital.getRoyalGuardPatrolling()));
            capitalTag.put(CapitalSavedData.KEY_ROYAL_GUARD_PATROL_ANCHORS, writeBlockPosMap(capital.getRoyalGuardPatrolAnchors()));
            capitalTag.put(CapitalSavedData.KEY_ROYAL_GUARD_DUTY_MODES, writeGuardDutyModeMap(capital.getRoyalGuardDutyModes()));
            capitalTag.putLong(CapitalSavedData.KEY_LAST_ROYAL_GUARD_PROMPT_DAY, capital.getLastRoyalGuardPromptDay());
            if (capital.getPendingPlayerGuardSelectionRequester() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER, capital.getPendingPlayerGuardSelectionRequester());
            }

            capitalTag.put(CapitalSavedData.KEY_PRINCE_CONSORT_SOURCES, writeUuidMap(capital.getPrinceConsortSources()));
            capitalTag.put(CapitalSavedData.KEY_PRINCE_CONSORT_FEMALE, writeBooleanMap(capital.getPrinceConsortFemale()));
            capitalTag.put(CapitalSavedData.KEY_MARRIAGE_DUKE_SOURCES, writeUuidMap(capital.getMarriageDukeSources()));
            capitalTag.put(CapitalSavedData.KEY_MARRIAGE_DUKE_FEMALE, writeBooleanMap(capital.getMarriageDukeFemale()));

            if (capital.getCommander() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_COMMANDER, capital.getCommander());
            }
            capitalTag.putBoolean(CapitalSavedData.KEY_COMMANDER_FEMALE, capital.isCommanderFemale());

            if (capital.getHand() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_HAND, capital.getHand());
            }
            capitalTag.putBoolean(CapitalSavedData.KEY_HAND_FEMALE, capital.isHandFemale());

            if (capital.getHerald() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_HERALD, capital.getHerald());
            }
            capitalTag.putBoolean(CapitalSavedData.KEY_HERALD_FEMALE, capital.isHeraldFemale());
            if (capital.getHeraldDisplayName() != null) {
                capitalTag.putString(CapitalSavedData.KEY_HERALD_DISPLAY_NAME, capital.getHeraldDisplayName());
            }

            if (capital.getGrandMaester() != null) {
                capitalTag.putUUID(CapitalSavedData.KEY_GRAND_MAESTER, capital.getGrandMaester());
            }
            capitalTag.putBoolean(CapitalSavedData.KEY_GRAND_MAESTER_FEMALE, capital.isGrandMaesterFemale());

            capitalTag.putLong(CapitalSavedData.KEY_LAST_COMMANDER_RAID_BLESSING_GAME_TIME, capital.getLastCommanderRaidBlessingGameTime());
            capitalTag.putLong(CapitalSavedData.KEY_LAST_COMMANDER_RANDOM_BLESSING_DAY, capital.getLastCommanderRandomBlessingDay());

            capitalsList.add(capitalTag);
        }

        tag.put(CapitalSavedData.KEY_CAPITALS, capitalsList);
        return tag;
    }

    private static ListTag writeUuidSet(Set<UUID> set) {
        ListTag list = new ListTag();
        for (UUID id : set) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(CapitalSavedData.KEY_ID, id);
            list.add(entry);
        }
        return list;
    }

    private static ListTag writeUuidList(List<UUID> listIn) {
        ListTag list = new ListTag();
        for (UUID id : listIn) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(CapitalSavedData.KEY_ID, id);
            list.add(entry);
        }
        return list;
    }

    private static ListTag writeBooleanMap(Map<UUID, Boolean> map) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Boolean> entry : map.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(CapitalSavedData.KEY_ID, entry.getKey());
            tag.putBoolean(CapitalSavedData.KEY_FLAG, entry.getValue());
            list.add(tag);
        }
        return list;
    }

    private static ListTag writeUuidMap(Map<UUID, UUID> map) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, UUID> entry : map.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(CapitalSavedData.KEY_ID, entry.getKey());
            tag.putUUID(CapitalSavedData.KEY_ENTITY_ID, entry.getValue());
            list.add(tag);
        }
        return list;
    }

    private static ListTag writeStringList(List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    private static ListTag writeStringMap(Map<UUID, String> map) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, String> entry : map.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(CapitalSavedData.KEY_ENTITY_ID, entry.getKey());
            tag.putString(CapitalSavedData.KEY_CLOTHES, entry.getValue());
            list.add(tag);
        }
        return list;
    }

    private static ListTag writeBlockPosMap(Map<UUID, net.minecraft.core.BlockPos> map) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, net.minecraft.core.BlockPos> entry : map.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(CapitalSavedData.KEY_GUARD_ID, entry.getKey());
            tag.putInt(CapitalSavedData.KEY_X, entry.getValue().getX());
            tag.putInt(CapitalSavedData.KEY_Y, entry.getValue().getY());
            tag.putInt(CapitalSavedData.KEY_Z, entry.getValue().getZ());
            list.add(tag);
        }
        return list;
    }

    private static ListTag writeGuardDutyModeMap(Map<UUID, CapitalRecord.GuardDutyMode> map) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, CapitalRecord.GuardDutyMode> entry : map.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(CapitalSavedData.KEY_GUARD_ID, entry.getKey());
            tag.putString(CapitalSavedData.KEY_MODE, entry.getValue().name());
            list.add(tag);
        }
        return list;
    }
}