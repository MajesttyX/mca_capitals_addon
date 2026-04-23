package com.example.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PendingVillagerBetrothalSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_pending_villager_betrothals";

    private static final String KEY_PAIRS = "Pairs";
    private static final String KEY_FIRST = "First";
    private static final String KEY_SECOND = "Second";

    private final Set<PendingPair> pairs = new LinkedHashSet<>();

    public static PendingVillagerBetrothalSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PendingVillagerBetrothalSavedData::load,
                        PendingVillagerBetrothalSavedData::new,
                        DATA_NAME
                );
    }

    public List<PendingPair> getPairs() {
        List<PendingPair> snapshot = new ArrayList<>(pairs);
        snapshot.sort(Comparator
                .comparing((PendingPair pair) -> pair.first().toString())
                .thenComparing(pair -> pair.second().toString()));
        return snapshot;
    }

    public boolean hasPendingBetrothal(UUID villagerId) {
        if (villagerId == null) {
            return false;
        }

        for (PendingPair pair : pairs) {
            if (villagerId.equals(pair.first()) || villagerId.equals(pair.second())) {
                return true;
            }
        }

        return false;
    }

    public UUID getPartner(UUID villagerId) {
        if (villagerId == null) {
            return null;
        }

        for (PendingPair pair : pairs) {
            if (villagerId.equals(pair.first())) {
                return pair.second();
            }
            if (villagerId.equals(pair.second())) {
                return pair.first();
            }
        }

        return null;
    }

    public boolean containsPair(UUID firstId, UUID secondId) {
        if (firstId == null || secondId == null) {
            return false;
        }
        return pairs.contains(PendingPair.of(firstId, secondId));
    }

    public void setPair(UUID firstId, UUID secondId) {
        if (firstId == null || secondId == null || firstId.equals(secondId)) {
            return;
        }

        PendingPair canonical = PendingPair.of(firstId, secondId);

        pairs.removeIf(pair ->
                pair.first().equals(firstId)
                        || pair.second().equals(firstId)
                        || pair.first().equals(secondId)
                        || pair.second().equals(secondId)
        );

        pairs.add(canonical);
        setDirty();
    }

    public void removePair(UUID firstId, UUID secondId) {
        if (firstId == null || secondId == null) {
            return;
        }

        if (pairs.remove(PendingPair.of(firstId, secondId))) {
            setDirty();
        }
    }

    public void removeVillager(UUID villagerId) {
        if (villagerId == null) {
            return;
        }

        boolean changed = pairs.removeIf(pair ->
                villagerId.equals(pair.first()) || villagerId.equals(pair.second())
        );

        if (changed) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (PendingPair pair : getPairs()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(KEY_FIRST, pair.first());
            entry.putUUID(KEY_SECOND, pair.second());
            list.add(entry);
        }

        tag.put(KEY_PAIRS, list);
        return tag;
    }

    public static PendingVillagerBetrothalSavedData load(CompoundTag tag) {
        PendingVillagerBetrothalSavedData data = new PendingVillagerBetrothalSavedData();
        ListTag list = tag.getList(KEY_PAIRS, Tag.TAG_COMPOUND);

        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;

            if (!entry.hasUUID(KEY_FIRST) || !entry.hasUUID(KEY_SECOND)) {
                continue;
            }

            UUID firstId = entry.getUUID(KEY_FIRST);
            UUID secondId = entry.getUUID(KEY_SECOND);

            if (firstId.equals(secondId)) {
                continue;
            }

            data.pairs.add(PendingPair.of(firstId, secondId));
        }

        return data;
    }

    public record PendingPair(UUID first, UUID second) {
        public static PendingPair of(UUID first, UUID second) {
            if (first.toString().compareTo(second.toString()) <= 0) {
                return new PendingPair(first, second);
            }
            return new PendingPair(second, first);
        }
    }
}