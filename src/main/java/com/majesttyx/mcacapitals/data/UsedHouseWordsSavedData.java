package com.majesttyx.mcacapitals.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UsedHouseWordsSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_used_house_words";

    private static final String KEY_RECORDS = "Records";
    private static final String KEY_PHRASE = "Phrase";
    private static final String KEY_BUCKET = "Bucket";
    private static final String KEY_HOUSE_NAME = "HouseName";
    private static final String KEY_FOUNDER_ID = "FounderId";
    private static final String KEY_FOUNDER_NAME = "FounderName";
    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_CAPITAL_NAME = "CapitalName";
    private static final String KEY_GAME_TIME = "GameTime";

    private final Map<String, UsedHouseWordsRecord> records = new HashMap<>();

    public static UsedHouseWordsSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                UsedHouseWordsSavedData::new,
                                UsedHouseWordsSavedData::load,
                                null
                        ),
                        DATA_NAME
                );
    }

    public boolean isPhraseUsed(String phrase) {
        phrase = normalizePhrase(phrase);
        return !phrase.isBlank() && records.containsKey(phrase);
    }

    public Set<String> getUsedBucketsForCapital(UUID capitalId) {
        Set<String> buckets = new HashSet<>();
        if (capitalId == null) {
            return buckets;
        }

        for (UsedHouseWordsRecord record : records.values()) {
            if (record != null && capitalId.equals(record.capitalId()) && record.bucket() != null && !record.bucket().isBlank()) {
                buckets.add(record.bucket());
            }
        }

        return buckets;
    }

    public void markUsed(
            String phrase,
            String bucket,
            String houseName,
            UUID founderId,
            String founderName,
            UUID capitalId,
            String capitalName,
            long gameTime
    ) {
        phrase = normalizePhrase(phrase);
        if (phrase.isBlank()) {
            return;
        }

        records.put(phrase, new UsedHouseWordsRecord(
                phrase,
                bucket == null ? "" : bucket,
                houseName == null ? "" : houseName,
                founderId,
                founderName == null ? "" : founderName,
                capitalId,
                capitalName == null ? "" : capitalName,
                gameTime
        ));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        for (UsedHouseWordsRecord record : records.values()) {
            if (record == null || record.phrase() == null || record.phrase().isBlank()) {
                continue;
            }

            CompoundTag entry = new CompoundTag();
            entry.putString(KEY_PHRASE, record.phrase());
            entry.putString(KEY_BUCKET, record.bucket());
            entry.putString(KEY_HOUSE_NAME, record.houseName());
            entry.putString(KEY_FOUNDER_NAME, record.founderName());
            entry.putString(KEY_CAPITAL_NAME, record.capitalName());
            entry.putLong(KEY_GAME_TIME, record.gameTime());

            if (record.founderId() != null) {
                entry.putUUID(KEY_FOUNDER_ID, record.founderId());
            }

            if (record.capitalId() != null) {
                entry.putUUID(KEY_CAPITAL_ID, record.capitalId());
            }

            list.add(entry);
        }

        tag.put(KEY_RECORDS, list);
        return tag;
    }

    public static UsedHouseWordsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        UsedHouseWordsSavedData data = new UsedHouseWordsSavedData();
        ListTag list = tag.getList(KEY_RECORDS, Tag.TAG_COMPOUND);

        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            if (!entry.contains(KEY_PHRASE, Tag.TAG_STRING)) {
                continue;
            }

            String phrase = normalizePhrase(entry.getString(KEY_PHRASE));
            if (phrase.isBlank()) {
                continue;
            }

            UUID founderId = entry.hasUUID(KEY_FOUNDER_ID) ? entry.getUUID(KEY_FOUNDER_ID) : null;
            UUID capitalId = entry.hasUUID(KEY_CAPITAL_ID) ? entry.getUUID(KEY_CAPITAL_ID) : null;

            data.records.put(phrase, new UsedHouseWordsRecord(
                    phrase,
                    entry.getString(KEY_BUCKET),
                    entry.getString(KEY_HOUSE_NAME),
                    founderId,
                    entry.getString(KEY_FOUNDER_NAME),
                    capitalId,
                    entry.getString(KEY_CAPITAL_NAME),
                    entry.getLong(KEY_GAME_TIME)
            ));
        }

        return data;
    }

    private static String normalizePhrase(String phrase) {
        return phrase == null ? "" : phrase.trim().replaceAll("\\s+", " ");
    }

    private record UsedHouseWordsRecord(
            String phrase,
            String bucket,
            String houseName,
            UUID founderId,
            String founderName,
            UUID capitalId,
            String capitalName,
            long gameTime
    ) {
    }
}