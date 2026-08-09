package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CapitalInterregnumSavedData extends SavedData {

    public static final String DATA_NAME =
            "mcacapitals_interregnums";
    private static final String KEY_INTERREGNUMS =
            "Interregnums";

    private final Map<UUID, CapitalInterregnumRecord>
            records = new LinkedHashMap<>();

    public CapitalInterregnumRecord getRecord(
            UUID capitalId
    ) {
        if (capitalId == null) {
            return null;
        }

        return records.get(capitalId);
    }
    public boolean begin(
            CapitalInterregnumRecord record
    ) {
        if (record == null
                || records.containsKey(record.getCapitalId())) {
            return false;
        }

        records.put(record.getCapitalId(), record);
        setDirty();
        return true;
    }

    public boolean remove(UUID capitalId) {
        if (capitalId == null) {
            return false;
        }

        boolean removed = records.remove(capitalId) != null;
        if (removed) {
            setDirty();
        }

        return removed;
    }

    public Map<UUID, CapitalInterregnumRecord> getSnapshot() {
        return new LinkedHashMap<>(records);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag recordsTag = new ListTag();
        for (CapitalInterregnumRecord record : records.values()) {
            if (record != null) {
                recordsTag.add(record.save());
            }
        }

        tag.put(KEY_INTERREGNUMS, recordsTag);
        return tag;
    }

    public static CapitalInterregnumSavedData load(CompoundTag tag) {
        CapitalInterregnumSavedData data =
                new CapitalInterregnumSavedData();
        ListTag recordsTag = tag.getList(
                KEY_INTERREGNUMS,
                Tag.TAG_COMPOUND
        );

        for (Tag rawRecord : recordsTag) {
            CapitalInterregnumRecord record =
                    CapitalInterregnumRecord.load(
                            (CompoundTag) rawRecord
                    );
            if (record != null) {
                data.records.put(
                        record.getCapitalId(),
                        record
                );
            }
        }

        return data;
    }
}
