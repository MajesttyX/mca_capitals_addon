package com.example.mcacapitals.data;

import com.example.mcacapitals.noble.NobleTitle;
import com.example.mcacapitals.player.PlayerCapitalTitleRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerCapitalTitleSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_player_titles";

    private static final String KEY_RECORDS = "Records";
    private static final String KEY_PLAYER_ID = "PlayerId";
    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_GRANTED_TITLE = "GrantedTitle";
    private static final String KEY_COMMANDER = "Commander";
    private static final String KEY_CACHED_PLAYER_NAME = "CachedPlayerName";

    private final Map<String, PlayerCapitalTitleRecord> records = new HashMap<>();

    public static PlayerCapitalTitleSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PlayerCapitalTitleSavedData::load,
                        PlayerCapitalTitleSavedData::new,
                        DATA_NAME
                );
    }

    public Map<String, PlayerCapitalTitleRecord> getRecords() {
        return records;
    }

    public PlayerCapitalTitleRecord get(UUID playerId, UUID capitalId) {
        if (playerId == null || capitalId == null) {
            return null;
        }
        return records.get(key(playerId, capitalId));
    }

    public PlayerCapitalTitleRecord getOrCreate(UUID playerId, UUID capitalId) {
        setDirty();
        return records.computeIfAbsent(key(playerId, capitalId), ignored -> new PlayerCapitalTitleRecord(playerId, capitalId));
    }

    public void remove(UUID playerId, UUID capitalId) {
        if (playerId == null || capitalId == null) {
            return;
        }
        if (records.remove(key(playerId, capitalId)) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (PlayerCapitalTitleRecord record : records.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(KEY_PLAYER_ID, record.getPlayerId());
            entry.putUUID(KEY_CAPITAL_ID, record.getCapitalId());
            entry.putString(KEY_GRANTED_TITLE, record.getGrantedTitle().name());
            entry.putBoolean(KEY_COMMANDER, record.isCommander());

            if (record.getCachedPlayerName() != null && !record.getCachedPlayerName().isBlank()) {
                entry.putString(KEY_CACHED_PLAYER_NAME, record.getCachedPlayerName());
            }

            list.add(entry);
        }

        tag.put(KEY_RECORDS, list);
        return tag;
    }

    public static PlayerCapitalTitleSavedData load(CompoundTag tag) {
        PlayerCapitalTitleSavedData data = new PlayerCapitalTitleSavedData();
        ListTag list = tag.getList(KEY_RECORDS, Tag.TAG_COMPOUND);

        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;

            if (!entry.hasUUID(KEY_PLAYER_ID) || !entry.hasUUID(KEY_CAPITAL_ID)) {
                continue;
            }

            UUID playerId = entry.getUUID(KEY_PLAYER_ID);
            UUID capitalId = entry.getUUID(KEY_CAPITAL_ID);

            PlayerCapitalTitleRecord record = new PlayerCapitalTitleRecord(playerId, capitalId);

            if (entry.contains(KEY_GRANTED_TITLE, Tag.TAG_STRING)) {
                try {
                    record.setGrantedTitle(NobleTitle.valueOf(entry.getString(KEY_GRANTED_TITLE)));
                } catch (IllegalArgumentException ignored) {
                    record.setGrantedTitle(NobleTitle.COMMONER);
                }
            }

            record.setCommander(entry.getBoolean(KEY_COMMANDER));

            if (entry.contains(KEY_CACHED_PLAYER_NAME, Tag.TAG_STRING)) {
                record.setCachedPlayerName(entry.getString(KEY_CACHED_PLAYER_NAME));
            }

            data.records.put(key(playerId, capitalId), record);
        }

        return data;
    }

    private static String key(UUID playerId, UUID capitalId) {
        return playerId + "|" + capitalId;
    }
}