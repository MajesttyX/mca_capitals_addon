package com.majesttyx.mcacapitals.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalJusticeSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_justice_data";

    private static final String KEY_CAPITALS = "Capitals";
    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_PLAYERS = "Players";
    private static final String KEY_PLAYER_ID = "PlayerId";
    private static final String KEY_LAST_ACCUSATION_DAY = "LastAccusationDay";
    private static final String KEY_LAST_EXILE_SCAN_DAY = "LastExileScanDay";
    private static final String KEY_DISCOVERED_EXILES = "DiscoveredExiles";
    private static final String KEY_ARREST_WARRANTS = "ArrestWarrants";
    private static final String KEY_DETAINED_PRISONERS = "DetainedPrisoners";
    private static final String KEY_ARREST_WARRANT_TIMES = "ArrestWarrantTimes";
    private static final String KEY_DETENTION_START_DAYS = "DetentionStartDays";
    private static final String KEY_TARGET_ID = "TargetId";
    private static final String KEY_GAME_TIME = "GameTime";
    private static final String KEY_DAY = "Day";

    private final Map<UUID, Map<UUID, Long>> lastAccusationDayByCapital = new LinkedHashMap<>();
    private final Map<UUID, Long> lastExileScanDayByCapital = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> discoveredExilesByCapital = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> arrestWarrantsByCapital = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> detainedPrisonersByCapital = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, Long>> arrestWarrantIssuedGameTimeByCapital = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, Long>> detentionStartDayByCapital = new LinkedHashMap<>();

    public long getLastAccusationDay(UUID capitalId, UUID playerId) {
        if (capitalId == null || playerId == null) {
            return Long.MIN_VALUE;
        }

        Map<UUID, Long> playerDays = lastAccusationDayByCapital.get(capitalId);
        if (playerDays == null) {
            return Long.MIN_VALUE;
        }

        return playerDays.getOrDefault(playerId, Long.MIN_VALUE);
    }

    public void setLastAccusationDay(UUID capitalId, UUID playerId, long day) {
        if (capitalId == null || playerId == null) {
            return;
        }

        lastAccusationDayByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>())
                .put(playerId, day);

        setDirty();
    }

    public long getLastExileScanDay(UUID capitalId) {
        if (capitalId == null) {
            return Long.MIN_VALUE;
        }

        return lastExileScanDayByCapital.getOrDefault(capitalId, Long.MIN_VALUE);
    }

    public void setLastExileScanDay(UUID capitalId, long day) {
        if (capitalId == null) {
            return;
        }

        lastExileScanDayByCapital.put(capitalId, day);
        setDirty();
    }

    public boolean hasDiscoveredExile(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        Set<UUID> discovered = discoveredExilesByCapital.get(capitalId);
        return discovered != null && discovered.contains(targetId);
    }

    public Set<UUID> getDiscoveredExiles(UUID capitalId) {
        if (capitalId == null) {
            return Set.of();
        }

        Set<UUID> discovered = discoveredExilesByCapital.get(capitalId);
        if (discovered == null || discovered.isEmpty()) {
            return Set.of();
        }

        return new LinkedHashSet<>(discovered);
    }

    public void markDiscoveredExile(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return;
        }

        discoveredExilesByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashSet<>())
                .add(targetId);

        setDirty();
    }

    public boolean clearDiscoveredExile(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        Set<UUID> discovered = discoveredExilesByCapital.get(capitalId);
        if (discovered == null) {
            return false;
        }

        boolean removed = discovered.remove(targetId);
        if (removed) {
            setDirty();
        }

        return removed;
    }

    public boolean hasArrestWarrant(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        Set<UUID> warrants = arrestWarrantsByCapital.get(capitalId);
        return warrants != null && warrants.contains(targetId);
    }

    public Set<UUID> getArrestWarrants(UUID capitalId) {
        if (capitalId == null) {
            return Set.of();
        }

        Set<UUID> warrants = arrestWarrantsByCapital.get(capitalId);
        if (warrants == null || warrants.isEmpty()) {
            return Set.of();
        }

        return new LinkedHashSet<>(warrants);
    }

    public boolean issueArrestWarrant(UUID capitalId, UUID targetId, long gameTime) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        boolean added = arrestWarrantsByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashSet<>())
                .add(targetId);

        Map<UUID, Long> times = arrestWarrantIssuedGameTimeByCapital.computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>());
        if (added || !times.containsKey(targetId)) {
            times.put(targetId, gameTime);
            setDirty();
        }

        return added;
    }

    public boolean clearArrestWarrant(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        boolean changed = false;

        Set<UUID> warrants = arrestWarrantsByCapital.get(capitalId);
        if (warrants != null && warrants.remove(targetId)) {
            changed = true;
        }

        Map<UUID, Long> times = arrestWarrantIssuedGameTimeByCapital.get(capitalId);
        if (times != null && times.remove(targetId) != null) {
            changed = true;
        }

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public long getArrestWarrantIssuedGameTime(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return Long.MIN_VALUE;
        }

        Map<UUID, Long> times = arrestWarrantIssuedGameTimeByCapital.get(capitalId);
        if (times == null) {
            return Long.MIN_VALUE;
        }

        return times.getOrDefault(targetId, Long.MIN_VALUE);
    }

    public boolean isDetainedPrisoner(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        Set<UUID> detained = detainedPrisonersByCapital.get(capitalId);
        return detained != null && detained.contains(targetId);
    }

    public Set<UUID> getDetainedPrisoners(UUID capitalId) {
        if (capitalId == null) {
            return Set.of();
        }

        Set<UUID> detained = detainedPrisonersByCapital.get(capitalId);
        if (detained == null || detained.isEmpty()) {
            return Set.of();
        }

        return new LinkedHashSet<>(detained);
    }

    public boolean markDetainedPrisoner(UUID capitalId, UUID targetId, long day) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        boolean added = detainedPrisonersByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashSet<>())
                .add(targetId);

        Map<UUID, Long> days = detentionStartDayByCapital.computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>());
        if (added || !days.containsKey(targetId)) {
            days.put(targetId, day);
            setDirty();
        }

        return added;
    }

    public boolean clearDetainedPrisoner(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        boolean changed = false;

        Set<UUID> detained = detainedPrisonersByCapital.get(capitalId);
        if (detained != null && detained.remove(targetId)) {
            changed = true;
        }

        Map<UUID, Long> days = detentionStartDayByCapital.get(capitalId);
        if (days != null && days.remove(targetId) != null) {
            changed = true;
        }

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public long getDetentionStartDay(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return Long.MIN_VALUE;
        }

        Map<UUID, Long> days = detentionStartDayByCapital.get(capitalId);
        if (days == null) {
            return Long.MIN_VALUE;
        }

        return days.getOrDefault(targetId, Long.MIN_VALUE);
    }

    public boolean clearJusticeCase(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        boolean changed = false;
        changed |= clearArrestWarrant(capitalId, targetId);
        changed |= clearDetainedPrisoner(capitalId, targetId);
        changed |= clearDiscoveredExile(capitalId, targetId);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag capitalList = new ListTag();

        Set<UUID> allCapitalIds = new LinkedHashSet<>();
        allCapitalIds.addAll(lastAccusationDayByCapital.keySet());
        allCapitalIds.addAll(lastExileScanDayByCapital.keySet());
        allCapitalIds.addAll(discoveredExilesByCapital.keySet());
        allCapitalIds.addAll(arrestWarrantsByCapital.keySet());
        allCapitalIds.addAll(detainedPrisonersByCapital.keySet());
        allCapitalIds.addAll(arrestWarrantIssuedGameTimeByCapital.keySet());
        allCapitalIds.addAll(detentionStartDayByCapital.keySet());

        for (UUID capitalId : allCapitalIds) {
            CompoundTag capitalTag = new CompoundTag();
            capitalTag.putUUID(KEY_CAPITAL_ID, capitalId);

            ListTag playerList = new ListTag();
            Map<UUID, Long> playerDays = lastAccusationDayByCapital.get(capitalId);
            if (playerDays != null) {
                for (Map.Entry<UUID, Long> playerEntry : playerDays.entrySet()) {
                    CompoundTag playerTag = new CompoundTag();
                    playerTag.putUUID(KEY_PLAYER_ID, playerEntry.getKey());
                    playerTag.putLong(KEY_LAST_ACCUSATION_DAY, playerEntry.getValue());
                    playerList.add(playerTag);
                }
            }
            capitalTag.put(KEY_PLAYERS, playerList);

            if (lastExileScanDayByCapital.containsKey(capitalId)) {
                capitalTag.putLong(KEY_LAST_EXILE_SCAN_DAY, lastExileScanDayByCapital.get(capitalId));
            }

            capitalTag.put(KEY_DISCOVERED_EXILES, writeUuidTargetSet(discoveredExilesByCapital.get(capitalId)));
            capitalTag.put(KEY_ARREST_WARRANTS, writeUuidTargetSet(arrestWarrantsByCapital.get(capitalId)));
            capitalTag.put(KEY_DETAINED_PRISONERS, writeUuidTargetSet(detainedPrisonersByCapital.get(capitalId)));
            capitalTag.put(KEY_ARREST_WARRANT_TIMES, writeUuidLongMap(arrestWarrantIssuedGameTimeByCapital.get(capitalId), KEY_GAME_TIME));
            capitalTag.put(KEY_DETENTION_START_DAYS, writeUuidLongMap(detentionStartDayByCapital.get(capitalId), KEY_DAY));

            capitalList.add(capitalTag);
        }

        tag.put(KEY_CAPITALS, capitalList);
        return tag;
    }

    public static CapitalJusticeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CapitalJusticeSavedData data = new CapitalJusticeSavedData();

        ListTag capitalList = tag.getList(KEY_CAPITALS, Tag.TAG_COMPOUND);
        for (Tag capitalEntryRaw : capitalList) {
            CompoundTag capitalTag = (CompoundTag) capitalEntryRaw;
            if (!capitalTag.hasUUID(KEY_CAPITAL_ID)) {
                continue;
            }

            UUID capitalId = capitalTag.getUUID(KEY_CAPITAL_ID);

            Map<UUID, Long> playerDays = new LinkedHashMap<>();
            ListTag playerList = capitalTag.getList(KEY_PLAYERS, Tag.TAG_COMPOUND);
            for (Tag playerEntryRaw : playerList) {
                CompoundTag playerTag = (CompoundTag) playerEntryRaw;
                if (!playerTag.hasUUID(KEY_PLAYER_ID)) {
                    continue;
                }

                playerDays.put(
                        playerTag.getUUID(KEY_PLAYER_ID),
                        playerTag.getLong(KEY_LAST_ACCUSATION_DAY)
                );
            }

            if (!playerDays.isEmpty()) {
                data.lastAccusationDayByCapital.put(capitalId, playerDays);
            }

            if (capitalTag.contains(KEY_LAST_EXILE_SCAN_DAY)) {
                data.lastExileScanDayByCapital.put(capitalId, capitalTag.getLong(KEY_LAST_EXILE_SCAN_DAY));
            }

            Set<UUID> discoveredExiles = readUuidTargetSet(capitalTag.getList(KEY_DISCOVERED_EXILES, Tag.TAG_COMPOUND));
            if (!discoveredExiles.isEmpty()) {
                data.discoveredExilesByCapital.put(capitalId, discoveredExiles);
            }

            Set<UUID> arrestWarrants = readUuidTargetSet(capitalTag.getList(KEY_ARREST_WARRANTS, Tag.TAG_COMPOUND));
            if (!arrestWarrants.isEmpty()) {
                data.arrestWarrantsByCapital.put(capitalId, arrestWarrants);
            }

            Set<UUID> detainedPrisoners = readUuidTargetSet(capitalTag.getList(KEY_DETAINED_PRISONERS, Tag.TAG_COMPOUND));
            if (!detainedPrisoners.isEmpty()) {
                data.detainedPrisonersByCapital.put(capitalId, detainedPrisoners);
            }

            Map<UUID, Long> warrantTimes = readUuidLongMap(capitalTag.getList(KEY_ARREST_WARRANT_TIMES, Tag.TAG_COMPOUND), KEY_GAME_TIME);
            if (!warrantTimes.isEmpty()) {
                data.arrestWarrantIssuedGameTimeByCapital.put(capitalId, warrantTimes);
            }

            Map<UUID, Long> detentionDays = readUuidLongMap(capitalTag.getList(KEY_DETENTION_START_DAYS, Tag.TAG_COMPOUND), KEY_DAY);
            if (!detentionDays.isEmpty()) {
                data.detentionStartDayByCapital.put(capitalId, detentionDays);
            }
        }

        return data;
    }

    private static ListTag writeUuidTargetSet(Set<UUID> values) {
        ListTag list = new ListTag();

        if (values == null || values.isEmpty()) {
            return list;
        }

        for (UUID targetId : values) {
            CompoundTag targetTag = new CompoundTag();
            targetTag.putUUID(KEY_TARGET_ID, targetId);
            list.add(targetTag);
        }

        return list;
    }

    private static Set<UUID> readUuidTargetSet(ListTag list) {
        Set<UUID> values = new LinkedHashSet<>();

        for (Tag entryRaw : list) {
            CompoundTag entry = (CompoundTag) entryRaw;
            if (entry.hasUUID(KEY_TARGET_ID)) {
                values.add(entry.getUUID(KEY_TARGET_ID));
            }
        }

        return values;
    }

    private static ListTag writeUuidLongMap(Map<UUID, Long> values, String longKey) {
        ListTag list = new ListTag();

        if (values == null || values.isEmpty()) {
            return list;
        }

        for (Map.Entry<UUID, Long> entry : values.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(KEY_TARGET_ID, entry.getKey());
            entryTag.putLong(longKey, entry.getValue());
            list.add(entryTag);
        }

        return list;
    }

    private static Map<UUID, Long> readUuidLongMap(ListTag list, String longKey) {
        Map<UUID, Long> values = new LinkedHashMap<>();

        for (Tag entryRaw : list) {
            CompoundTag entry = (CompoundTag) entryRaw;
            if (entry.hasUUID(KEY_TARGET_ID)) {
                values.put(entry.getUUID(KEY_TARGET_ID), entry.getLong(longKey));
            }
        }

        return values;
    }
}