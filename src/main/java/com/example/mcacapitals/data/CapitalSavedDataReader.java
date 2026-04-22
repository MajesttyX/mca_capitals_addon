package com.example.mcacapitals.data;

import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.capital.CapitalState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

final class CapitalSavedDataReader {

    private CapitalSavedDataReader() {
    }

    static CapitalSavedData loadCapitals(CompoundTag tag) {
        CapitalSavedData data = new CapitalSavedData();
        ListTag capitalsList = tag.getList(CapitalSavedData.KEY_CAPITALS, Tag.TAG_COMPOUND);

        for (Tag entry : capitalsList) {
            CompoundTag capitalTag = (CompoundTag) entry;

            UUID capitalId = capitalTag.getUUID(CapitalSavedData.KEY_CAPITAL_ID);
            Integer villageId = capitalTag.contains(CapitalSavedData.KEY_VILLAGE_ID)
                    ? capitalTag.getInt(CapitalSavedData.KEY_VILLAGE_ID)
                    : null;

            CapitalRecord capital = new CapitalRecord(capitalId, villageId);

            if (capitalTag.contains(CapitalSavedData.KEY_STATE)) {
                try {
                    capital.setState(CapitalState.valueOf(capitalTag.getString(CapitalSavedData.KEY_STATE)));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (capitalTag.hasUUID(CapitalSavedData.KEY_SOVEREIGN)) {
                capital.setSovereign(capitalTag.getUUID(CapitalSavedData.KEY_SOVEREIGN));
            }
            capital.setSovereignFemale(capitalTag.getBoolean(CapitalSavedData.KEY_SOVEREIGN_FEMALE));

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

            if (capitalTag.contains(CapitalSavedData.KEY_HEIR_MODE)) {
                try {
                    capital.setHeirMode(CapitalRecord.HeirMode.valueOf(capitalTag.getString(CapitalSavedData.KEY_HEIR_MODE)));
                } catch (IllegalArgumentException ignored) {
                    capital.setHeirMode(CapitalRecord.HeirMode.DYNASTIC);
                }
            }

            capital.setPlayerSovereign(capitalTag.getBoolean(CapitalSavedData.KEY_PLAYER_SOVEREIGN));
            if (capitalTag.hasUUID(CapitalSavedData.KEY_PLAYER_SOVEREIGN_ID)) {
                capital.setPlayerSovereignId(capitalTag.getUUID(CapitalSavedData.KEY_PLAYER_SOVEREIGN_ID));
            }
            if (capitalTag.contains(CapitalSavedData.KEY_PLAYER_SOVEREIGN_NAME)) {
                capital.setPlayerSovereignName(capitalTag.getString(CapitalSavedData.KEY_PLAYER_SOVEREIGN_NAME));
            }

            capital.setPlayerConsort(capitalTag.getBoolean(CapitalSavedData.KEY_PLAYER_CONSORT));
            if (capitalTag.hasUUID(CapitalSavedData.KEY_PLAYER_CONSORT_ID)) {
                capital.setPlayerConsortId(capitalTag.getUUID(CapitalSavedData.KEY_PLAYER_CONSORT_ID));
            }
            if (capitalTag.contains(CapitalSavedData.KEY_PLAYER_CONSORT_NAME)) {
                capital.setPlayerConsortName(capitalTag.getString(CapitalSavedData.KEY_PLAYER_CONSORT_NAME));
            }

            capital.setMonarchyRejected(capitalTag.getBoolean(CapitalSavedData.KEY_MONARCHY_REJECTED));

            capital.setMourningActive(capitalTag.getBoolean(CapitalSavedData.KEY_MOURNING_ACTIVE));
            capital.setMourningEndDay(capitalTag.getLong(CapitalSavedData.KEY_MOURNING_END_DAY));

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_ROYAL_CHILDREN, Tag.TAG_COMPOUND), capital.getRoyalChildren());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_ROYAL_CHILD_FEMALE, Tag.TAG_COMPOUND), capital.getRoyalChildFemale());
            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_ROYAL_HOUSEHOLD, Tag.TAG_COMPOUND), capital.getRoyalHousehold());
            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_DISINHERITED_ROYAL_CHILDREN, Tag.TAG_COMPOUND), capital.getDisinheritedRoyalChildren());
            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_LEGITIMIZED_ROYAL_CHILDREN, Tag.TAG_COMPOUND), capital.getLegitimizedRoyalChildren());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_LEGITIMIZED_ROYAL_CHILD_FEMALE, Tag.TAG_COMPOUND), capital.getLegitimizedRoyalChildFemale());
            readUuidList(capitalTag.getList(CapitalSavedData.KEY_ROYAL_SUCCESSION_ORDER, Tag.TAG_COMPOUND), capital.getRoyalSuccessionOrder());

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_DUKES, Tag.TAG_COMPOUND), capital.getDukes());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_DUKE_FEMALE, Tag.TAG_COMPOUND), capital.getDukeFemale());

            readUuidMap(capitalTag.getList(CapitalSavedData.KEY_DOWAGER_PRINCES, Tag.TAG_COMPOUND), capital.getDowagerPrinceSources());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_DOWAGER_PRINCE_FEMALE, Tag.TAG_COMPOUND), capital.getDowagerPrinceFemale());

            readUuidMap(capitalTag.getList(CapitalSavedData.KEY_DOWAGER_DUKES, Tag.TAG_COMPOUND), capital.getDowagerDukeSources());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_DOWAGER_DUKE_FEMALE, Tag.TAG_COMPOUND), capital.getDowagerDukeFemale());

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_LORDS, Tag.TAG_COMPOUND), capital.getLords());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_LORD_FEMALE, Tag.TAG_COMPOUND), capital.getLordFemale());

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_KNIGHTS, Tag.TAG_COMPOUND), capital.getKnights());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_KNIGHT_FEMALE, Tag.TAG_COMPOUND), capital.getKnightFemale());

            readStringList(capitalTag.getList(CapitalSavedData.KEY_CHRONICLE_ENTRIES, Tag.TAG_STRING), capital.getChronicleEntries());
            readStringMap(capitalTag.getList(CapitalSavedData.KEY_MOURNING_ORIGINAL_CLOTHES, Tag.TAG_COMPOUND), capital.getMourningOriginalClothes());

            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARDS, Tag.TAG_COMPOUND), capital.getRoyalGuards());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARD_FEMALE, Tag.TAG_COMPOUND), capital.getRoyalGuardFemale());
            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_DISGRACED_ROYAL_GUARDS, Tag.TAG_COMPOUND), capital.getDisgracedRoyalGuards());
            if (capitalTag.hasUUID(CapitalSavedData.KEY_ROYAL_GUARD_LIEGE)) {
                capital.setRoyalGuardLiege(capitalTag.getUUID(CapitalSavedData.KEY_ROYAL_GUARD_LIEGE));
            }
            readUuidSet(capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARD_PATROLLING, Tag.TAG_COMPOUND), capital.getRoyalGuardPatrolling());
            readBlockPosMap(capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARD_PATROL_ANCHORS, Tag.TAG_COMPOUND), capital.getRoyalGuardPatrolAnchors());
            readGuardDutyModeMap(capitalTag.getList(CapitalSavedData.KEY_ROYAL_GUARD_DUTY_MODES, Tag.TAG_COMPOUND), capital.getRoyalGuardDutyModes());
            capital.setLastRoyalGuardPromptDay(capitalTag.getLong(CapitalSavedData.KEY_LAST_ROYAL_GUARD_PROMPT_DAY));
            if (capitalTag.hasUUID(CapitalSavedData.KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER)) {
                capital.setPendingPlayerGuardSelectionRequester(capitalTag.getUUID(CapitalSavedData.KEY_PENDING_PLAYER_GUARD_SELECTION_REQUESTER));
            }

            readUuidMap(capitalTag.getList(CapitalSavedData.KEY_PRINCE_CONSORT_SOURCES, Tag.TAG_COMPOUND), capital.getPrinceConsortSources());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_PRINCE_CONSORT_FEMALE, Tag.TAG_COMPOUND), capital.getPrinceConsortFemale());
            readUuidMap(capitalTag.getList(CapitalSavedData.KEY_MARRIAGE_DUKE_SOURCES, Tag.TAG_COMPOUND), capital.getMarriageDukeSources());
            readBooleanMap(capitalTag.getList(CapitalSavedData.KEY_MARRIAGE_DUKE_FEMALE, Tag.TAG_COMPOUND), capital.getMarriageDukeFemale());

            if (capitalTag.hasUUID(CapitalSavedData.KEY_COMMANDER)) {
                capital.setCommander(capitalTag.getUUID(CapitalSavedData.KEY_COMMANDER));
            }
            capital.setCommanderFemale(capitalTag.getBoolean(CapitalSavedData.KEY_COMMANDER_FEMALE));

            if (capitalTag.hasUUID(CapitalSavedData.KEY_HAND)) {
                capital.setHand(capitalTag.getUUID(CapitalSavedData.KEY_HAND));
            }
            capital.setHandFemale(capitalTag.getBoolean(CapitalSavedData.KEY_HAND_FEMALE));

            if (capitalTag.hasUUID(CapitalSavedData.KEY_HERALD)) {
                capital.setHerald(capitalTag.getUUID(CapitalSavedData.KEY_HERALD));
            }
            capital.setHeraldFemale(capitalTag.getBoolean(CapitalSavedData.KEY_HERALD_FEMALE));

            if (capitalTag.hasUUID(CapitalSavedData.KEY_GRAND_MAESTER)) {
                capital.setGrandMaester(capitalTag.getUUID(CapitalSavedData.KEY_GRAND_MAESTER));
            }
            capital.setGrandMaesterFemale(capitalTag.getBoolean(CapitalSavedData.KEY_GRAND_MAESTER_FEMALE));

            capital.setLastCommanderRaidBlessingGameTime(capitalTag.getLong(CapitalSavedData.KEY_LAST_COMMANDER_RAID_BLESSING_GAME_TIME));
            capital.setLastCommanderRandomBlessingDay(capitalTag.getLong(CapitalSavedData.KEY_LAST_COMMANDER_RANDOM_BLESSING_DAY));

            data.getCapitals().add(capital);
        }

        return data;
    }

    private static void readUuidSet(ListTag list, java.util.Set<UUID> out) {
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            if (tag.hasUUID(CapitalSavedData.KEY_ID)) {
                out.add(tag.getUUID(CapitalSavedData.KEY_ID));
            }
        }
    }

    private static void readUuidList(ListTag list, java.util.List<UUID> out) {
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            if (tag.hasUUID(CapitalSavedData.KEY_ID)) {
                out.add(tag.getUUID(CapitalSavedData.KEY_ID));
            }
        }
    }

    private static void readBooleanMap(ListTag list, java.util.Map<UUID, Boolean> out) {
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            if (tag.hasUUID(CapitalSavedData.KEY_ID)) {
                out.put(tag.getUUID(CapitalSavedData.KEY_ID), tag.getBoolean(CapitalSavedData.KEY_FLAG));
            }
        }
    }

    private static void readUuidMap(ListTag list, java.util.Map<UUID, UUID> out) {
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            if (tag.hasUUID(CapitalSavedData.KEY_ID) && tag.hasUUID(CapitalSavedData.KEY_ENTITY_ID)) {
                out.put(tag.getUUID(CapitalSavedData.KEY_ID), tag.getUUID(CapitalSavedData.KEY_ENTITY_ID));
            }
        }
    }

    private static void readStringList(ListTag list, java.util.List<String> out) {
        for (Tag entry : list) {
            out.add(((StringTag) entry).getAsString());
        }
    }

    private static void readStringMap(ListTag list, java.util.Map<UUID, String> out) {
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            if (tag.hasUUID(CapitalSavedData.KEY_ENTITY_ID)) {
                out.put(tag.getUUID(CapitalSavedData.KEY_ENTITY_ID), tag.getString(CapitalSavedData.KEY_CLOTHES));
            }
        }
    }

    private static void readBlockPosMap(ListTag list, java.util.Map<UUID, BlockPos> out) {
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            if (tag.hasUUID(CapitalSavedData.KEY_GUARD_ID)) {
                out.put(
                        tag.getUUID(CapitalSavedData.KEY_GUARD_ID),
                        new BlockPos(
                                tag.getInt(CapitalSavedData.KEY_X),
                                tag.getInt(CapitalSavedData.KEY_Y),
                                tag.getInt(CapitalSavedData.KEY_Z)
                        )
                );
            }
        }
    }

    private static void readGuardDutyModeMap(ListTag list, java.util.Map<UUID, CapitalRecord.GuardDutyMode> out) {
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            if (tag.hasUUID(CapitalSavedData.KEY_GUARD_ID) && tag.contains(CapitalSavedData.KEY_MODE)) {
                try {
                    out.put(
                            tag.getUUID(CapitalSavedData.KEY_GUARD_ID),
                            CapitalRecord.GuardDutyMode.valueOf(tag.getString(CapitalSavedData.KEY_MODE))
                    );
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}