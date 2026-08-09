package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalRefugeeSavedData
        extends SavedData {
    public static final String DATA_NAME =
            "mcacapitals_refugees";

    private static final String KEY_REFUGEES =
            "Refugees";

    private final Map<
            UUID,
            CapitalRefugeeRecord
            > refugees =
            new LinkedHashMap<>();

    public CapitalRefugeeRecord getRecord(
            UUID refugeeId
    ) {
        if (refugeeId == null) {
            return null;
        }

        return refugees.get(refugeeId);
    }
    public CapitalRefugeeRecord markExiled(
            UUID refugeeId,
            UUID originCapitalId,
            int originVillageId,
            String originCapitalName,
            long exiledAt
    ) {
        if (refugeeId == null
                || originCapitalId == null) {
            return null;
        }

        CapitalRefugeeRecord existing =
                refugees.get(refugeeId);
        if (existing != null) {
            if (existing.getStatus()
                    == CapitalRefugeeStatus
                    .ASYLUM_GRANTED) {
                existing.clearAsylum();
                setDirty();
            }

            return existing;
        }
        CapitalRefugeeRecord record =
                new CapitalRefugeeRecord(
                        refugeeId,
                        originCapitalId,
                        originVillageId,
                        originCapitalName,
                        exiledAt
                );

        refugees.put(
                refugeeId,
                record
        );

        setDirty();
        return record;
    }
    public boolean grantAsylum(
            UUID refugeeId,
            UUID asylumCapitalId,
            long grantedAt
    ) {
        CapitalRefugeeRecord record =
                getRecord(refugeeId);

        if (record == null
                || asylumCapitalId == null) {
            return false;
        }

        record.grantAsylum(
                asylumCapitalId,
                grantedAt
        );

        setDirty();
        return true;
    }
    public boolean clearAsylum(
            UUID refugeeId
    ) {
        CapitalRefugeeRecord record =
                getRecord(refugeeId);

        if (record == null
                || record.getStatus()
                != CapitalRefugeeStatus
                .ASYLUM_GRANTED) {
            return false;
        }

        record.clearAsylum();
        setDirty();

        return true;
    }
    public List<CapitalRefugeeRecord>
    getAwaitingAsylum() {
        List<CapitalRefugeeRecord> result =
                new ArrayList<>();

        for (CapitalRefugeeRecord record :
                refugees.values()) {
            if (record != null
                    && record.isAwaitingAsylum()) {
                result.add(record);
            }
        }

        return List.copyOf(result);
    }
    public List<CapitalRefugeeRecord> getAsylees(
            UUID asylumCapitalId
    ) {
        if (asylumCapitalId == null) {
            return List.of();
        }

        List<CapitalRefugeeRecord> result =
                new ArrayList<>();
        for (CapitalRefugeeRecord record :
                refugees.values()) {
            if (record != null
                    && record.hasAsylumIn(
                    asylumCapitalId
            )) {
                result.add(record);
            }
        }

        return List.copyOf(result);
    }

    public Map<UUID, CapitalRefugeeRecord>
    getSnapshot() {
        return new LinkedHashMap<>(refugees);
    }
    public boolean removeCapital(
            UUID capitalId
    ) {
        if (capitalId == null) {
            return false;
        }

        boolean changed = false;

        for (CapitalRefugeeRecord record :
                refugees.values()) {
            if (record != null
                    && record.hasAsylumIn(
                    capitalId
            )) {
                record.clearAsylum();
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }

        return changed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag refugeesTag =
                new ListTag();

        for (CapitalRefugeeRecord record :
                refugees.values()) {
            if (record != null) {
                refugeesTag.add(record.save());
            }
        }
        tag.put(
                KEY_REFUGEES,
                refugeesTag
        );

        return tag;
    }

    public static CapitalRefugeeSavedData load(CompoundTag tag) {
        CapitalRefugeeSavedData data =
                new CapitalRefugeeSavedData();

        ListTag refugeesTag =
                tag.getList(
                        KEY_REFUGEES,
                        Tag.TAG_COMPOUND
                );
        for (Tag rawRecord : refugeesTag) {
            CapitalRefugeeRecord record =
                    CapitalRefugeeRecord.load(
                            (CompoundTag) rawRecord
                    );

            if (record != null) {
                data.refugees.put(
                        record.getRefugeeId(),
                        record
                );
            }
        }

        return data;
    }
}
