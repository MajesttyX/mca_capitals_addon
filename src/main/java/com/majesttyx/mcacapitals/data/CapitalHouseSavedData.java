package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.house.CapitalHouseHistoryEntry;
import com.majesttyx.mcacapitals.house.CapitalHouseHistoryType;
import com.majesttyx.mcacapitals.house.CapitalHouseRecord;
import com.majesttyx.mcacapitals.house.CapitalHouseTier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CapitalHouseSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_capital_houses";

    private static final SavedData.Factory<CapitalHouseSavedData> FACTORY =
            new SavedData.Factory<>(CapitalHouseSavedData::new, CapitalHouseSavedData::load, null);

    private static final String KEY_CAPITALS = "Capitals";
    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_HOUSES = "Houses";

    private static final String KEY_HOUSE_ID = "HouseId";
    private static final String KEY_HOUSE_NAME = "HouseName";
    private static final String KEY_TIER = "Tier";
    private static final String KEY_FOUNDER_ID = "FounderId";
    private static final String KEY_HEAD_ID = "HeadId";
    private static final String KEY_PARENT_HOUSE_ID = "ParentHouseId";
    private static final String KEY_FOUNDED_GAME_TIME = "FoundedGameTime";
    private static final String KEY_ACTIVE = "Active";
    private static final String KEY_CURRENT_MEMBERS = "CurrentMembers";
    private static final String KEY_FORMER_MEMBERS = "FormerMembers";
    private static final String KEY_HISTORY = "History";

    private static final String KEY_MEMBER_ID = "MemberId";

    private static final String KEY_HISTORY_TYPE = "Type";
    private static final String KEY_HISTORY_GAME_TIME = "GameTime";
    private static final String KEY_HISTORY_SUBJECT_ID = "SubjectId";
    private static final String KEY_HISTORY_RELATED_HOUSE_ID = "RelatedHouseId";

    private final Map<UUID, LinkedHashMap<UUID, CapitalHouseRecord>> housesByCapital =
            new LinkedHashMap<>();

    public static CapitalHouseSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Collection<CapitalHouseRecord> getHouses(UUID capitalId) {
        Map<UUID, CapitalHouseRecord> houses = housesByCapital.get(capitalId);
        if (houses == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(houses.values());
    }

    public CapitalHouseRecord getHouse(UUID capitalId, UUID houseId) {
        if (capitalId == null || houseId == null) {
            return null;
        }
        Map<UUID, CapitalHouseRecord> houses = housesByCapital.get(capitalId);
        return houses == null ? null : houses.get(houseId);
    }

    public CapitalHouseRecord findHouseByName(UUID capitalId, String houseName) {
        if (capitalId == null || houseName == null || houseName.isBlank()) {
            return null;
        }

        String wanted = houseName.trim();
        for (CapitalHouseRecord house : getHouses(capitalId)) {
            if (house != null && house.getHouseName().equalsIgnoreCase(wanted)) {
                return house;
            }
        }
        return null;
    }

    public CapitalHouseRecord findHouseForMember(UUID capitalId, UUID memberId) {
        if (capitalId == null || memberId == null) {
            return null;
        }
        for (CapitalHouseRecord house : getHouses(capitalId)) {
            if (house != null && house.isCurrentMember(memberId)) {
                return house;
            }
        }
        return null;
    }

    public CapitalHouseRecord createHouse(
            UUID capitalId,
            UUID houseId,
            String houseName
    ) {
        if (capitalId == null || houseId == null || houseName == null || houseName.isBlank()) {
            return null;
        }

        LinkedHashMap<UUID, CapitalHouseRecord> houses =
                housesByCapital.computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>());

        CapitalHouseRecord existing = houses.get(houseId);
        if (existing != null) {
            return existing;
        }

        CapitalHouseRecord created = new CapitalHouseRecord(houseId, capitalId, houseName);
        houses.put(houseId, created);
        setDirty();
        return created;
    }

    public void removeCapital(UUID capitalId) {
        if (capitalId != null && housesByCapital.remove(capitalId) != null) {
            setDirty();
        }
    }

    public void markDirty() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag capitals = new ListTag();

        for (Map.Entry<UUID, LinkedHashMap<UUID, CapitalHouseRecord>> capitalEntry
                : housesByCapital.entrySet()) {
            UUID capitalId = capitalEntry.getKey();
            if (capitalId == null) {
                continue;
            }

            CompoundTag capitalTag = new CompoundTag();
            capitalTag.putUUID(KEY_CAPITAL_ID, capitalId);

            ListTag houses = new ListTag();
            for (CapitalHouseRecord house : capitalEntry.getValue().values()) {
                if (house == null || house.getHouseId() == null || house.getHouseName().isBlank()) {
                    continue;
                }
                houses.add(saveHouse(house));
            }

            capitalTag.put(KEY_HOUSES, houses);
            capitals.add(capitalTag);
        }

        tag.put(KEY_CAPITALS, capitals);
        return tag;
    }

    public static CapitalHouseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CapitalHouseSavedData data = new CapitalHouseSavedData();
        ListTag capitals = tag.getList(KEY_CAPITALS, Tag.TAG_COMPOUND);

        for (Tag rawCapital : capitals) {
            CompoundTag capitalTag = (CompoundTag) rawCapital;
            if (!capitalTag.hasUUID(KEY_CAPITAL_ID)) {
                continue;
            }

            UUID capitalId = capitalTag.getUUID(KEY_CAPITAL_ID);
            ListTag houses = capitalTag.getList(KEY_HOUSES, Tag.TAG_COMPOUND);

            LinkedHashMap<UUID, CapitalHouseRecord> capitalHouses = new LinkedHashMap<>();

            for (Tag rawHouse : houses) {
                CapitalHouseRecord house = loadHouse(capitalId, (CompoundTag) rawHouse);
                if (house != null) {
                    capitalHouses.put(house.getHouseId(), house);
                }
            }

            if (!capitalHouses.isEmpty()) {
                data.housesByCapital.put(capitalId, capitalHouses);
            }
        }

        return data;
    }

    private static CompoundTag saveHouse(CapitalHouseRecord house) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(KEY_HOUSE_ID, house.getHouseId());
        tag.putString(KEY_HOUSE_NAME, house.getHouseName());
        tag.putString(KEY_TIER, house.getTier().name());
        tag.putLong(KEY_FOUNDED_GAME_TIME, house.getFoundedGameTime());
        tag.putBoolean(KEY_ACTIVE, house.isActive());

        if (house.getFounderId() != null) {
            tag.putUUID(KEY_FOUNDER_ID, house.getFounderId());
        }
        if (house.getHeadId() != null) {
            tag.putUUID(KEY_HEAD_ID, house.getHeadId());
        }
        if (house.getParentHouseId() != null) {
            tag.putUUID(KEY_PARENT_HOUSE_ID, house.getParentHouseId());
        }

        tag.put(KEY_CURRENT_MEMBERS, saveUuidList(house.getCurrentMembers()));
        tag.put(KEY_FORMER_MEMBERS, saveUuidList(house.getFormerMembers()));

        ListTag history = new ListTag();
        for (CapitalHouseHistoryEntry entry : house.getHistory()) {
            if (entry == null || entry.type() == null) {
                continue;
            }

            CompoundTag eventTag = new CompoundTag();
            eventTag.putString(KEY_HISTORY_TYPE, entry.type().name());
            eventTag.putLong(KEY_HISTORY_GAME_TIME, entry.gameTime());

            if (entry.subjectId() != null) {
                eventTag.putUUID(KEY_HISTORY_SUBJECT_ID, entry.subjectId());
            }
            if (entry.relatedHouseId() != null) {
                eventTag.putUUID(KEY_HISTORY_RELATED_HOUSE_ID, entry.relatedHouseId());
            }

            history.add(eventTag);
        }
        tag.put(KEY_HISTORY, history);

        return tag;
    }

    private static CapitalHouseRecord loadHouse(UUID capitalId, CompoundTag tag) {
        if (!tag.hasUUID(KEY_HOUSE_ID) || !tag.contains(KEY_HOUSE_NAME, Tag.TAG_STRING)) {
            return null;
        }

        UUID houseId = tag.getUUID(KEY_HOUSE_ID);
        String houseName = tag.getString(KEY_HOUSE_NAME);
        if (houseName.isBlank()) {
            return null;
        }

        CapitalHouseRecord house = new CapitalHouseRecord(houseId, capitalId, houseName);
        house.setTier(parseTier(tag.getString(KEY_TIER)));
        house.setFoundedGameTime(tag.getLong(KEY_FOUNDED_GAME_TIME));
        house.setActive(!tag.contains(KEY_ACTIVE, Tag.TAG_BYTE) || tag.getBoolean(KEY_ACTIVE));

        if (tag.hasUUID(KEY_FOUNDER_ID)) {
            house.setFounderId(tag.getUUID(KEY_FOUNDER_ID));
        }
        if (tag.hasUUID(KEY_HEAD_ID)) {
            house.setHeadId(tag.getUUID(KEY_HEAD_ID));
        }
        if (tag.hasUUID(KEY_PARENT_HOUSE_ID)) {
            house.setParentHouseId(tag.getUUID(KEY_PARENT_HOUSE_ID));
        }

        loadUuidList(tag.getList(KEY_CURRENT_MEMBERS, Tag.TAG_COMPOUND), house, true);
        loadUuidList(tag.getList(KEY_FORMER_MEMBERS, Tag.TAG_COMPOUND), house, false);

        ListTag history = tag.getList(KEY_HISTORY, Tag.TAG_COMPOUND);
        for (Tag rawEvent : history) {
            CompoundTag eventTag = (CompoundTag) rawEvent;
            CapitalHouseHistoryType type = parseHistoryType(eventTag.getString(KEY_HISTORY_TYPE));
            if (type == null) {
                continue;
            }

            UUID subjectId = eventTag.hasUUID(KEY_HISTORY_SUBJECT_ID)
                    ? eventTag.getUUID(KEY_HISTORY_SUBJECT_ID)
                    : null;
            UUID relatedHouseId = eventTag.hasUUID(KEY_HISTORY_RELATED_HOUSE_ID)
                    ? eventTag.getUUID(KEY_HISTORY_RELATED_HOUSE_ID)
                    : null;

            house.addHistory(new CapitalHouseHistoryEntry(
                    type,
                    eventTag.getLong(KEY_HISTORY_GAME_TIME),
                    subjectId,
                    relatedHouseId
            ));
        }

        return house;
    }

    private static ListTag saveUuidList(Collection<UUID> values) {
        ListTag list = new ListTag();
        for (UUID value : values) {
            if (value == null) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putUUID(KEY_MEMBER_ID, value);
            list.add(entry);
        }
        return list;
    }

    private static void loadUuidList(
            ListTag list,
            CapitalHouseRecord house,
            boolean current
    ) {
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            if (!entry.hasUUID(KEY_MEMBER_ID)) {
                continue;
            }
            UUID memberId = entry.getUUID(KEY_MEMBER_ID);
            if (current) {
                house.addCurrentMember(memberId);
            } else {
                house.addFormerMember(memberId);
            }
        }
    }

    private static CapitalHouseTier parseTier(String value) {
        try {
            return CapitalHouseTier.valueOf(value);
        } catch (Exception ignored) {
            return CapitalHouseTier.NOBLE;
        }
    }

    private static CapitalHouseHistoryType parseHistoryType(String value) {
        try {
            return CapitalHouseHistoryType.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
