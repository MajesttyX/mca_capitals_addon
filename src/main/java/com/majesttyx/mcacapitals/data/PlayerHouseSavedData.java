package com.majesttyx.mcacapitals.data;

import com.majesttyx.mcacapitals.house.PlayerHouseInheritanceMode;
import com.majesttyx.mcacapitals.house.PlayerHouseRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerHouseSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_player_houses";

    private static final String KEY_RECORDS = "Records";
    private static final String KEY_PLAYER_ID = "PlayerId";
    private static final String KEY_HOUSE_NAME = "HouseName";
    private static final String KEY_HOUSE_WORDS = "HouseWords";
    private static final String KEY_INHERITANCE_MODE = "InheritanceMode";
    private static final String KEY_SET_AT_GAME_TIME = "HouseNameSetAtGameTime";
    private static final String KEY_SET_IN_CAPITAL_ID = "HouseNameSetInCapitalId";
    private static final String KEY_SET_IN_CAPITAL_NAME = "HouseNameSetInCapitalName";

    private final Map<UUID, PlayerHouseRecord> records = new HashMap<>();

    public static PlayerHouseSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PlayerHouseSavedData::load,
                        PlayerHouseSavedData::new,
                        DATA_NAME
                );
    }

    public PlayerHouseRecord get(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        return records.get(playerId);
    }

    public PlayerHouseRecord getOrCreate(UUID playerId) {
        PlayerHouseRecord existing = get(playerId);
        if (existing != null) {
            return existing;
        }

        PlayerHouseRecord created = new PlayerHouseRecord(playerId);
        records.put(playerId, created);
        setDirty();
        return created;
    }

    public void remove(UUID playerId) {
        if (playerId == null) {
            return;
        }

        if (records.remove(playerId) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (PlayerHouseRecord record : records.values()) {
            if (record == null || record.getPlayerId() == null || !record.hasHouseName()) {
                continue;
            }

            CompoundTag entry = new CompoundTag();
            entry.putUUID(KEY_PLAYER_ID, record.getPlayerId());
            entry.putString(KEY_HOUSE_NAME, record.getHouseName());
            entry.putString(KEY_HOUSE_WORDS, record.getHouseWords());
            entry.putString(KEY_INHERITANCE_MODE, record.getInheritanceMode().name());
            entry.putLong(KEY_SET_AT_GAME_TIME, record.getHouseNameSetAtGameTime());

            if (record.getHouseNameSetInCapitalId() != null) {
                entry.putUUID(KEY_SET_IN_CAPITAL_ID, record.getHouseNameSetInCapitalId());
            }

            if (record.getHouseNameSetInCapitalName() != null && !record.getHouseNameSetInCapitalName().isBlank()) {
                entry.putString(KEY_SET_IN_CAPITAL_NAME, record.getHouseNameSetInCapitalName());
            }

            list.add(entry);
        }

        tag.put(KEY_RECORDS, list);
        return tag;
    }

    public static PlayerHouseSavedData load(CompoundTag tag) {
        PlayerHouseSavedData data = new PlayerHouseSavedData();
        ListTag list = tag.getList(KEY_RECORDS, Tag.TAG_COMPOUND);

        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            if (!entry.hasUUID(KEY_PLAYER_ID) || !entry.contains(KEY_HOUSE_NAME, Tag.TAG_STRING)) {
                continue;
            }

            UUID playerId = entry.getUUID(KEY_PLAYER_ID);
            PlayerHouseRecord record = new PlayerHouseRecord(playerId);
            record.setHouseName(entry.getString(KEY_HOUSE_NAME));

            if (entry.contains(KEY_HOUSE_WORDS, Tag.TAG_STRING)) {
                record.setHouseWords(entry.getString(KEY_HOUSE_WORDS));
            }

            record.setInheritanceMode(parseMode(entry.getString(KEY_INHERITANCE_MODE)));
            record.setHouseNameSetAtGameTime(entry.getLong(KEY_SET_AT_GAME_TIME));

            if (entry.hasUUID(KEY_SET_IN_CAPITAL_ID)) {
                record.setHouseNameSetInCapitalId(entry.getUUID(KEY_SET_IN_CAPITAL_ID));
            }

            if (entry.contains(KEY_SET_IN_CAPITAL_NAME, Tag.TAG_STRING)) {
                record.setHouseNameSetInCapitalName(entry.getString(KEY_SET_IN_CAPITAL_NAME));
            }

            if (record.hasHouseName()) {
                data.records.put(playerId, record);
            }
        }

        return data;
    }

    private static PlayerHouseInheritanceMode parseMode(String value) {
        try {
            return PlayerHouseInheritanceMode.valueOf(value);
        } catch (Exception ignored) {
            return PlayerHouseInheritanceMode.FOLLOW_CAPITAL_LAW;
        }
    }
}