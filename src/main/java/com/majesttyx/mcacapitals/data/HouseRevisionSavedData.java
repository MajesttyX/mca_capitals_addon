package com.majesttyx.mcacapitals.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class HouseRevisionSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_house_revisions";

    private static final String KEY_REVISIONS = "Revisions";
    private static final String KEY_HOUSE = "House";
    private static final String KEY_WORDS = "Words";

    private final Map<String, String> houseWords = new HashMap<>();

    public HouseRevisionSavedData() {
    }

    public static HouseRevisionSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        HouseRevisionSavedData data = new HouseRevisionSavedData();
        if (tag == null) {
            return data;
        }

        ListTag revisions = tag.getList(KEY_REVISIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < revisions.size(); i++) {
            CompoundTag entry = revisions.getCompound(i);
            String house = entry.getString(KEY_HOUSE);
            if (house == null || house.isBlank()) {
                continue;
            }
            data.houseWords.put(house, entry.getString(KEY_WORDS));
        }

        return data;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag revisions = new ListTag();

        for (Map.Entry<String, String> revision : houseWords.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString(KEY_HOUSE, revision.getKey());
            entry.putString(KEY_WORDS, revision.getValue());
            revisions.add(entry);
        }

        tag.put(KEY_REVISIONS, revisions);
        return tag;
    }

    public String getHouseWords(String houseKey) {
        return houseKey == null ? null : houseWords.get(houseKey);
    }

    public boolean setHouseWords(String houseKey, String words) {
        if (houseKey == null || houseKey.isBlank()) {
            return false;
        }

        String normalizedWords = words == null
                ? ""
                : words.trim().replaceAll("\\s+", " ");
        String previous = houseWords.put(houseKey, normalizedWords);
        boolean changed = previous == null || !previous.equals(normalizedWords);
        if (changed) {
            setDirty();
        }
        return changed;
    }
}
