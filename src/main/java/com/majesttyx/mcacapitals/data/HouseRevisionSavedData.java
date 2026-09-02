package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HouseRevisionSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_house_revisions";

    private static final String KEY_RECORDS = "Records";
    private static final String KEY_FOUNDER_ID = "FounderId";
    private static final String KEY_HOUSE_NAME = "HouseName";
    private static final String KEY_HOUSE_WORDS = "HouseWords";
    private static final String KEY_REVISED_AT = "RevisedAtGameTime";

    private final Map<UUID, HouseRevisionRecord> records = new HashMap<>();

    public static HouseRevisionSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        HouseRevisionSavedData::load,
                        HouseRevisionSavedData::new,
                        DATA_NAME
                );
    }

    public HouseRevisionRecord get(UUID founderId) {
        if (founderId == null) {
            return null;
        }
        return records.get(founderId);
    }

    public HouseRevisionRecord put(
            UUID founderId,
            String houseName,
            String houseWords,
            long revisedAtGameTime
    ) {
        if (founderId == null) {
            return null;
        }

        HouseRevisionRecord record = new HouseRevisionRecord(
                founderId,
                normalize(houseName),
                normalize(houseWords),
                revisedAtGameTime
        );

        records.put(founderId, record);
        setDirty();
        return record;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (HouseRevisionRecord record : records.values()) {
            if (record == null
                    || record.founderId() == null
                    || record.houseName().isBlank()) {
                continue;
            }

            CompoundTag entry = new CompoundTag();
            entry.putUUID(KEY_FOUNDER_ID, record.founderId());
            entry.putString(KEY_HOUSE_NAME, record.houseName());
            entry.putString(KEY_HOUSE_WORDS, record.houseWords());
            entry.putLong(KEY_REVISED_AT, record.revisedAtGameTime());
            list.add(entry);
        }

        tag.put(KEY_RECORDS, list);
        return tag;
    }

    public static HouseRevisionSavedData load(CompoundTag tag) {
        HouseRevisionSavedData data = new HouseRevisionSavedData();
        ListTag list = tag.getList(KEY_RECORDS, Tag.TAG_COMPOUND);

        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            if (!entry.hasUUID(KEY_FOUNDER_ID)
                    || !entry.contains(KEY_HOUSE_NAME, Tag.TAG_STRING)) {
                continue;
            }

            UUID founderId = entry.getUUID(KEY_FOUNDER_ID);
            String houseName = normalize(entry.getString(KEY_HOUSE_NAME));
            if (houseName.isBlank()) {
                continue;
            }

            String houseWords = entry.contains(KEY_HOUSE_WORDS, Tag.TAG_STRING)
                    ? normalize(entry.getString(KEY_HOUSE_WORDS))
                    : "";

            long revisedAt = entry.contains(KEY_REVISED_AT, Tag.TAG_LONG)
                    ? entry.getLong(KEY_REVISED_AT)
                    : 0L;

            data.records.put(
                    founderId,
                    new HouseRevisionRecord(founderId, houseName, houseWords, revisedAt)
            );
        }

        return data;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    public record HouseRevisionRecord(
            UUID founderId,
            String houseName,
            String houseWords,
            long revisedAtGameTime
    ) {
    }
}
