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

    private final List<CapitalRecord> capitals = new ArrayList<>();

    public List<CapitalRecord> getCapitals() {
        return capitals;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag capitalList = new ListTag();

        for (CapitalRecord capital : capitals) {
            CompoundTag capitalTag = new CompoundTag();

            capitalTag.putUUID("CapitalId", capital.getCapitalId());

            if (capital.getVillageId() != null) {
                capitalTag.putInt("VillageId", capital.getVillageId());
            }

            capitalTag.putString("State", capital.getState().name());

            if (capital.getSovereign() != null) {
                capitalTag.putUUID("Sovereign", capital.getSovereign());
            }
            capitalTag.putBoolean("SovereignFemale", capital.isSovereignFemale());

            if (capital.getConsort() != null) {
                capitalTag.putUUID("Consort", capital.getConsort());
            }
            capitalTag.putBoolean("ConsortFemale", capital.isConsortFemale());

            if (capital.getDowager() != null) {
                capitalTag.putUUID("Dowager", capital.getDowager());
            }
            capitalTag.putBoolean("DowagerFemale", capital.isDowagerFemale());

            if (capital.getHeir() != null) {
                capitalTag.putUUID("Heir", capital.getHeir());
            }
            capitalTag.putBoolean("HeirFemale", capital.isHeirFemale());
            capitalTag.putString("HeirMode", capital.getHeirMode().name());

            capitalTag.putBoolean("PlayerSovereign", capital.isPlayerSovereign());
            if (capital.getPlayerSovereignId() != null) {
                capitalTag.putUUID("PlayerSovereignId", capital.getPlayerSovereignId());
            }
            if (capital.getPlayerSovereignName() != null) {
                capitalTag.putString("PlayerSovereignName", capital.getPlayerSovereignName());
            }

            capitalTag.putBoolean("PlayerConsort", capital.isPlayerConsort());
            if (capital.getPlayerConsortId() != null) {
                capitalTag.putUUID("PlayerConsortId", capital.getPlayerConsortId());
            }
            if (capital.getPlayerConsortName() != null) {
                capitalTag.putString("PlayerConsortName", capital.getPlayerConsortName());
            }

            capitalTag.putBoolean("MonarchyRejected", capital.isMonarchyRejected());

            capitalTag.putBoolean("MourningActive", capital.isMourningActive());
            capitalTag.putLong("MourningEndDay", capital.getMourningEndDay());

            writeUuidSet(capitalTag, "RoyalChildren", capital.getRoyalChildren());
            writeUuidBooleanMap(capitalTag, "RoyalChildFemale", capital.getRoyalChildFemale());

            writeUuidSet(capitalTag, "DisinheritedRoyalChildren", capital.getDisinheritedRoyalChildren());
            writeUuidSet(capitalTag, "LegitimizedRoyalChildren", capital.getLegitimizedRoyalChildren());
            writeUuidBooleanMap(capitalTag, "LegitimizedRoyalChildFemale", capital.getLegitimizedRoyalChildFemale());
            writeUuidList(capitalTag, "RoyalSuccessionOrder", capital.getRoyalSuccessionOrder());

            writeUuidSet(capitalTag, "Dukes", capital.getDukes());
            writeUuidBooleanMap(capitalTag, "DukeFemale", capital.getDukeFemale());

            writeUuidSet(capitalTag, "Lords", capital.getLords());
            writeUuidBooleanMap(capitalTag, "LordFemale", capital.getLordFemale());

            writeUuidSet(capitalTag, "Knights", capital.getKnights());
            writeUuidBooleanMap(capitalTag, "KnightFemale", capital.getKnightFemale());

            ListTag chronicleTag = new ListTag();
            for (String entry : capital.getChronicleEntries()) {
                chronicleTag.add(StringTag.valueOf(entry));
            }
            capitalTag.put("ChronicleEntries", chronicleTag);

            ListTag mourningClothesTag = new ListTag();
            capital.getMourningOriginalClothes().forEach((uuid, clothes) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID("EntityId", uuid);
                entryTag.putString("Clothes", clothes == null ? "" : clothes);
                mourningClothesTag.add(entryTag);
            });
            capitalTag.put("MourningOriginalClothes", mourningClothesTag);

            if (capital.getCommander() != null) {
                capitalTag.putUUID("Commander", capital.getCommander());
            }
            capitalTag.putBoolean("CommanderFemale", capital.isCommanderFemale());
            capitalTag.putLong("LastCommanderRaidBlessingGameTime", capital.getLastCommanderRaidBlessingGameTime());
            capitalTag.putLong("LastCommanderRandomBlessingDay", capital.getLastCommanderRandomBlessingDay());

            writeUuidSet(capitalTag, "RoyalGuards", capital.getRoyalGuards());
            writeUuidBooleanMap(capitalTag, "RoyalGuardFemale", capital.getRoyalGuardFemale());
            writeUuidSet(capitalTag, "DisgracedRoyalGuards", capital.getDisgracedRoyalGuards());

            if (capital.getRoyalGuardLiege() != null) {
                capitalTag.putUUID("RoyalGuardLiege", capital.getRoyalGuardLiege());
            }

            writeUuidSet(capitalTag, "RoyalGuardPatrolling", capital.getRoyalGuardPatrolling());

            ListTag guardAnchorsTag = new ListTag();
            capital.getRoyalGuardPatrolAnchors().forEach((uuid, anchor) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID("GuardId", uuid);
                entryTag.putInt("X", anchor.getX());
                entryTag.putInt("Y", anchor.getY());
                entryTag.putInt("Z", anchor.getZ());
                guardAnchorsTag.add(entryTag);
            });
            capitalTag.put("RoyalGuardPatrolAnchors", guardAnchorsTag);

            ListTag guardModesTag = new ListTag();
            capital.getRoyalGuardDutyModes().forEach((uuid, mode) -> {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID("GuardId", uuid);
                entryTag.putString("Mode", mode.name());
                guardModesTag.add(entryTag);
            });
            capitalTag.put("RoyalGuardDutyModes", guardModesTag);

            capitalTag.putLong("LastRoyalGuardPromptDay", capital.getLastRoyalGuardPromptDay());

            if (capital.getPendingPlayerGuardSelectionRequester() != null) {
                capitalTag.putUUID("PendingPlayerGuardSelectionRequester", capital.getPendingPlayerGuardSelectionRequester());
            }

            capitalList.add(capitalTag);
        }

        tag.put("Capitals", capitalList);
        return tag;
    }

    public static CapitalSavedData load(CompoundTag tag) {
        CapitalSavedData data = new CapitalSavedData();
        ListTag capitalList = tag.getList("Capitals", Tag.TAG_COMPOUND);

        for (Tag baseTag : capitalList) {
            CompoundTag capitalTag = (CompoundTag) baseTag;

            UUID capitalId = capitalTag.getUUID("CapitalId");
            Integer villageId = capitalTag.contains("VillageId") ? capitalTag.getInt("VillageId") : null;

            CapitalRecord capital = new CapitalRecord(capitalId, villageId);

            if (capitalTag.contains("State")) {
                try {
                    capital.setState(CapitalState.valueOf(capitalTag.getString("State")));
                } catch (IllegalArgumentException ignored) {
                    capital.setState(CapitalState.ACTIVE);
                }
            }

            if (capitalTag.contains("Sovereign")) {
                capital.setSovereign(capitalTag.getUUID("Sovereign"));
            }
            capital.setSovereignFemale(capitalTag.getBoolean("SovereignFemale"));

            if (capitalTag.contains("Consort")) {
                capital.setConsort(capitalTag.getUUID("Consort"));
            }
            capital.setConsortFemale(capitalTag.getBoolean("ConsortFemale"));

            if (capitalTag.contains("Dowager")) {
                capital.setDowager(capitalTag.getUUID("Dowager"));
            }
            capital.setDowagerFemale(capitalTag.getBoolean("DowagerFemale"));

            if (capitalTag.contains("Heir")) {
                capital.setHeir(capitalTag.getUUID("Heir"));
            }
            capital.setHeirFemale(capitalTag.getBoolean("HeirFemale"));
            if (capitalTag.contains("HeirMode")) {
                try {
                    capital.setHeirMode(CapitalRecord.HeirMode.valueOf(capitalTag.getString("HeirMode")));
                } catch (IllegalArgumentException ignored) {
                    capital.setHeirMode(CapitalRecord.HeirMode.DYNASTIC);
                }
            }

            capital.setPlayerSovereign(capitalTag.getBoolean("PlayerSovereign"));
            if (capitalTag.contains("PlayerSovereignId")) {
                capital.setPlayerSovereignId(capitalTag.getUUID("PlayerSovereignId"));
            }
            if (capitalTag.contains("PlayerSovereignName")) {
                capital.setPlayerSovereignName(capitalTag.getString("PlayerSovereignName"));
            }

            capital.setPlayerConsort(capitalTag.getBoolean("PlayerConsort"));
            if (capitalTag.contains("PlayerConsortId")) {
                capital.setPlayerConsortId(capitalTag.getUUID("PlayerConsortId"));
            }
            if (capitalTag.contains("PlayerConsortName")) {
                capital.setPlayerConsortName(capitalTag.getString("PlayerConsortName"));
            }

            capital.setMonarchyRejected(capitalTag.getBoolean("MonarchyRejected"));

            capital.setMourningActive(capitalTag.getBoolean("MourningActive"));
            capital.setMourningEndDay(capitalTag.getLong("MourningEndDay"));

            readUuidSet(capitalTag.getList("RoyalChildren", Tag.TAG_STRING), capital.getRoyalChildren());
            readUuidBooleanMap(capitalTag.getList("RoyalChildFemale", Tag.TAG_COMPOUND), capital.getRoyalChildFemale());

            readUuidSet(capitalTag.getList("DisinheritedRoyalChildren", Tag.TAG_STRING), capital.getDisinheritedRoyalChildren());
            readUuidSet(capitalTag.getList("LegitimizedRoyalChildren", Tag.TAG_STRING), capital.getLegitimizedRoyalChildren());
            readUuidBooleanMap(capitalTag.getList("LegitimizedRoyalChildFemale", Tag.TAG_COMPOUND), capital.getLegitimizedRoyalChildFemale());
            readUuidList(capitalTag.getList("RoyalSuccessionOrder", Tag.TAG_STRING), capital.getRoyalSuccessionOrder());

            readUuidSet(capitalTag.getList("Dukes", Tag.TAG_STRING), capital.getDukes());
            readUuidBooleanMap(capitalTag.getList("DukeFemale", Tag.TAG_COMPOUND), capital.getDukeFemale());

            readUuidSet(capitalTag.getList("Lords", Tag.TAG_STRING), capital.getLords());
            readUuidBooleanMap(capitalTag.getList("LordFemale", Tag.TAG_COMPOUND), capital.getLordFemale());

            readUuidSet(capitalTag.getList("Knights", Tag.TAG_STRING), capital.getKnights());
            readUuidBooleanMap(capitalTag.getList("KnightFemale", Tag.TAG_COMPOUND), capital.getKnightFemale());

            ListTag chronicleTag = capitalTag.getList("ChronicleEntries", Tag.TAG_STRING);
            for (Tag entryTag : chronicleTag) {
                capital.getChronicleEntries().add(entryTag.getAsString());
            }

            ListTag mourningClothesTag = capitalTag.getList("MourningOriginalClothes", Tag.TAG_COMPOUND);
            for (Tag entryBase : mourningClothesTag) {
                CompoundTag entryTag = (CompoundTag) entryBase;
                UUID entityId = entryTag.getUUID("EntityId");
                String clothes = entryTag.getString("Clothes");
                capital.getMourningOriginalClothes().put(entityId, clothes);
            }

            if (capitalTag.contains("Commander")) {
                capital.setCommander(capitalTag.getUUID("Commander"));
            }
            capital.setCommanderFemale(capitalTag.getBoolean("CommanderFemale"));
            capital.setLastCommanderRaidBlessingGameTime(capitalTag.getLong("LastCommanderRaidBlessingGameTime"));
            capital.setLastCommanderRandomBlessingDay(capitalTag.getLong("LastCommanderRandomBlessingDay"));

            readUuidSet(capitalTag.getList("RoyalGuards", Tag.TAG_STRING), capital.getRoyalGuards());
            readUuidBooleanMap(capitalTag.getList("RoyalGuardFemale", Tag.TAG_COMPOUND), capital.getRoyalGuardFemale());
            readUuidSet(capitalTag.getList("DisgracedRoyalGuards", Tag.TAG_STRING), capital.getDisgracedRoyalGuards());

            if (capitalTag.contains("RoyalGuardLiege")) {
                capital.setRoyalGuardLiege(capitalTag.getUUID("RoyalGuardLiege"));
            }

            readUuidSet(capitalTag.getList("RoyalGuardPatrolling", Tag.TAG_STRING), capital.getRoyalGuardPatrolling());

            ListTag guardAnchorsTag = capitalTag.getList("RoyalGuardPatrolAnchors", Tag.TAG_COMPOUND);
            for (Tag entryBase : guardAnchorsTag) {
                CompoundTag entryTag = (CompoundTag) entryBase;
                UUID guardId = entryTag.getUUID("GuardId");
                capital.setRoyalGuardPatrolAnchor(guardId,
                        new BlockPos(entryTag.getInt("X"), entryTag.getInt("Y"), entryTag.getInt("Z")));
            }

            ListTag guardModesTag = capitalTag.getList("RoyalGuardDutyModes", Tag.TAG_COMPOUND);
            for (Tag entryBase : guardModesTag) {
                CompoundTag entryTag = (CompoundTag) entryBase;
                UUID guardId = entryTag.getUUID("GuardId");
                try {
                    capital.setRoyalGuardDutyMode(guardId, CapitalRecord.GuardDutyMode.valueOf(entryTag.getString("Mode")));
                } catch (IllegalArgumentException ignored) {
                    capital.setRoyalGuardDutyMode(guardId, CapitalRecord.GuardDutyMode.FOLLOW_SOVEREIGN);
                }
            }

            capital.setLastRoyalGuardPromptDay(capitalTag.getLong("LastRoyalGuardPromptDay"));

            if (capitalTag.contains("PendingPlayerGuardSelectionRequester")) {
                capital.setPendingPlayerGuardSelectionRequester(capitalTag.getUUID("PendingPlayerGuardSelectionRequester"));
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
            tag.putUUID("Id", id);
            tag.putBoolean("Flag", flag != null && flag);
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
                target.put(tag.getUUID("Id"), tag.getBoolean("Flag"));
            } catch (Exception ignored) {
            }
        }
    }
}
