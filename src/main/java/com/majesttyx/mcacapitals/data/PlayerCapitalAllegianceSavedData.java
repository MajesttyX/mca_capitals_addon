package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerCapitalAllegianceSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_player_allegiance";
    private static final String KEY_RECORDS = "Records";
    private static final String KEY_PLAYER_ID = "PlayerId";
    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_LAST_CHANGE_DAY = "LastChangeDay";

    private final Map<UUID, AllegianceRecord> records = new LinkedHashMap<>();

    public AllegianceRecord getRecord(UUID playerId) {
        return playerId == null ? null : records.get(playerId);
    }
    public UUID getDeclaredCapitalId(UUID playerId) {
        AllegianceRecord record = getRecord(playerId);
        return record == null ? null : record.capitalId();
    }

    public long getLastChangeDay(UUID playerId) {
        AllegianceRecord record = getRecord(playerId);
        return record == null ? 0L : record.lastChangeDay();
    }

    public void setDeclaration(UUID playerId, UUID capitalId, long changeDay) {
        if (playerId == null || capitalId == null) {
            return;
        }
        AllegianceRecord replacement = new AllegianceRecord(
                capitalId,
                Math.max(0L, changeDay)
        );
        AllegianceRecord previous = records.put(playerId, replacement);
        if (!replacement.equals(previous)) {
            setDirty();
        }
    }
    public boolean clearDeclaration(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        if (records.remove(playerId) != null) {
            setDirty();
            return true;
        }
        return false;
    }
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag recordTags = new ListTag();
        for (Map.Entry<UUID, AllegianceRecord> entry : records.entrySet()) {
            CompoundTag recordTag = new CompoundTag();
            recordTag.putUUID(KEY_PLAYER_ID, entry.getKey());
            recordTag.putUUID(KEY_CAPITAL_ID, entry.getValue().capitalId());
            recordTag.putLong(KEY_LAST_CHANGE_DAY, entry.getValue().lastChangeDay());
            recordTags.add(recordTag);
        }
        tag.put(KEY_RECORDS, recordTags);
        return tag;
    }
    public static PlayerCapitalAllegianceSavedData load(CompoundTag tag) {
        PlayerCapitalAllegianceSavedData data = new PlayerCapitalAllegianceSavedData();
        ListTag recordTags = tag.getList(KEY_RECORDS, Tag.TAG_COMPOUND);
        for (Tag raw : recordTags) {
            CompoundTag recordTag = (CompoundTag) raw;
            if (!recordTag.hasUUID(KEY_PLAYER_ID)
                    || !recordTag.hasUUID(KEY_CAPITAL_ID)) {
                continue;
            }
            data.records.put(
                    recordTag.getUUID(KEY_PLAYER_ID),
                    new AllegianceRecord(
                            recordTag.getUUID(KEY_CAPITAL_ID),
                            Math.max(0L, recordTag.getLong(KEY_LAST_CHANGE_DAY))
                    )
            );
        }
        return data;
    }
    public record AllegianceRecord(UUID capitalId, long lastChangeDay) {
    }
}
