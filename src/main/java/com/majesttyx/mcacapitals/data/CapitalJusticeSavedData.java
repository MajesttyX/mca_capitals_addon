package com.majesttyx.mcacapitals.data;

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
    private static final String KEY_PUBLIC_STATUSES = "PublicCrownStatuses";
    private static final String KEY_PUBLIC_STATUS_SOVEREIGN = "PublicStatusSovereign";
    private static final String KEY_CONFIRMED_CASE_COUNTS = "ConfirmedCaseCounts";
    private static final String KEY_LAST_RESOLVED_DAYS = "LastResolvedDays";
    private static final String KEY_JUDGMENTS = "Judgments";
    private static final String KEY_SENTENCE_END_DAYS = "SentenceEndDays";
    private static final String KEY_LAST_NPC_JUDGMENT_DAY = "LastNpcJudgmentDay";
    private static final String KEY_TARGET_ID = "TargetId";
    private static final String KEY_GAME_TIME = "GameTime";
    private static final String KEY_DAY = "Day";
    private static final String KEY_COUNT = "Count";
    private static final String KEY_STATUS = "Status";
    private static final String KEY_JUDGMENT = "Judgment";

    private final Map<UUID, Map<UUID, Long>> lastAccusationDayByCapital = new LinkedHashMap<>();
    private final Map<UUID, Long> lastExileScanDayByCapital = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> discoveredExilesByCapital = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> arrestWarrantsByCapital = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> detainedPrisonersByCapital = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, Long>> arrestWarrantIssuedGameTimeByCapital = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, Long>> detentionStartDayByCapital = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, CapitalPublicCrownStatus>> publicStatusesByCapital = new LinkedHashMap<>();
    private final Map<UUID, UUID> publicStatusSovereignByCapital = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, Integer>> confirmedCaseCountsByCapital = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, Long>> lastResolvedDayByCapital = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, CapitalJudgmentType>> judgmentsByCapital = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, Long>> sentenceEndDayByCapital = new LinkedHashMap<>();
    private final Map<UUID, Long> lastNpcJudgmentDayByCapital = new LinkedHashMap<>();

    public long getLastAccusationDay(UUID capitalId, UUID playerId) {
        return getLong(lastAccusationDayByCapital, capitalId, playerId);
    }

    public void setLastAccusationDay(UUID capitalId, UUID playerId, long day) {
        putLong(lastAccusationDayByCapital, capitalId, playerId, day);
    }

    public long getLastExileScanDay(UUID capitalId) {
        return capitalId == null ? Long.MIN_VALUE : lastExileScanDayByCapital.getOrDefault(capitalId, Long.MIN_VALUE);
    }

    public void setLastExileScanDay(UUID capitalId, long day) {
        if (capitalId != null) {
            lastExileScanDayByCapital.put(capitalId, day);
            setDirty();
        }
    }

    public boolean hasDiscoveredExile(UUID capitalId, UUID targetId) {
        return contains(discoveredExilesByCapital, capitalId, targetId);
    }

    public Set<UUID> getDiscoveredExiles(UUID capitalId) {
        return snapshot(discoveredExilesByCapital, capitalId);
    }

    public void markDiscoveredExile(UUID capitalId, UUID targetId) {
        add(discoveredExilesByCapital, capitalId, targetId);
    }

    public boolean clearDiscoveredExile(UUID capitalId, UUID targetId) {
        return remove(discoveredExilesByCapital, capitalId, targetId);
    }

    public boolean hasArrestWarrant(UUID capitalId, UUID targetId) {
        return contains(arrestWarrantsByCapital, capitalId, targetId);
    }

    public Set<UUID> getArrestWarrants(UUID capitalId) {
        return snapshot(arrestWarrantsByCapital, capitalId);
    }

    public boolean issueArrestWarrant(UUID capitalId, UUID targetId, long gameTime) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        boolean added = arrestWarrantsByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashSet<>())
                .add(targetId);

        Map<UUID, Long> times = arrestWarrantIssuedGameTimeByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>());

        if (added || !times.containsKey(targetId)) {
            times.put(targetId, gameTime);
            setDirty();
        }

        return added;
    }

    public boolean clearArrestWarrant(UUID capitalId, UUID targetId) {
        boolean changed = removeWithoutDirty(arrestWarrantsByCapital, capitalId, targetId);
        changed |= removeFromNestedMap(arrestWarrantIssuedGameTimeByCapital, capitalId, targetId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public long getArrestWarrantIssuedGameTime(UUID capitalId, UUID targetId) {
        return getLong(arrestWarrantIssuedGameTimeByCapital, capitalId, targetId);
    }

    public boolean isDetainedPrisoner(UUID capitalId, UUID targetId) {
        return contains(detainedPrisonersByCapital, capitalId, targetId);
    }

    public Set<UUID> getDetainedPrisoners(UUID capitalId) {
        return snapshot(detainedPrisonersByCapital, capitalId);
    }

    public boolean markDetainedPrisoner(UUID capitalId, UUID targetId, long day) {
        if (capitalId == null || targetId == null) {
            return false;
        }

        boolean added = detainedPrisonersByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashSet<>())
                .add(targetId);

        Map<UUID, Long> days = detentionStartDayByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>());

        if (added || !days.containsKey(targetId)) {
            days.put(targetId, day);
            setDirty();
        }

        return added;
    }

    public boolean clearDetainedPrisoner(UUID capitalId, UUID targetId) {
        boolean changed = removeWithoutDirty(detainedPrisonersByCapital, capitalId, targetId);
        changed |= removeFromNestedMap(detentionStartDayByCapital, capitalId, targetId);
        changed |= removeFromNestedMap(judgmentsByCapital, capitalId, targetId);
        changed |= removeFromNestedMap(sentenceEndDayByCapital, capitalId, targetId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public long getDetentionStartDay(UUID capitalId, UUID targetId) {
        return getLong(detentionStartDayByCapital, capitalId, targetId);
    }

    public CapitalPublicCrownStatus getPublicStatus(UUID capitalId, UUID targetId) {
        return getValue(publicStatusesByCapital, capitalId, targetId);
    }

    public Map<UUID, CapitalPublicCrownStatus> getPublicStatuses(UUID capitalId) {
        Map<UUID, CapitalPublicCrownStatus> values = capitalId == null ? null : publicStatusesByCapital.get(capitalId);
        return values == null ? Map.of() : new LinkedHashMap<>(values);
    }

    public void setPublicStatus(UUID capitalId, UUID targetId, CapitalPublicCrownStatus status) {
        if (capitalId == null || targetId == null) {
            return;
        }

        if (status == null) {
            if (removeFromNestedMap(publicStatusesByCapital, capitalId, targetId)) {
                setDirty();
            }
            return;
        }

        publicStatusesByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>())
                .put(targetId, status);
        setDirty();
    }

    public void clearResolvedPublicStatuses(UUID capitalId) {
        Map<UUID, CapitalPublicCrownStatus> statuses = publicStatusesByCapital.get(capitalId);
        if (statuses == null) {
            return;
        }

        boolean changed = statuses.entrySet().removeIf(entry ->
                entry.getValue() == CapitalPublicCrownStatus.RECOGNIZED_FRIEND
                        || entry.getValue() == CapitalPublicCrownStatus.RESTORED_TO_PEACE
                        || entry.getValue() == CapitalPublicCrownStatus.DISCOVERED_ENEMY
                        && !hasArrestWarrant(capitalId, entry.getKey())
                        && !isDetainedPrisoner(capitalId, entry.getKey())
        );

        if (changed) {
            setDirty();
        }
    }

    public UUID getPublicStatusSovereign(UUID capitalId) {
        return capitalId == null ? null : publicStatusSovereignByCapital.get(capitalId);
    }

    public void setPublicStatusSovereign(UUID capitalId, UUID sovereignId) {
        if (capitalId == null) {
            return;
        }

        if (sovereignId == null) {
            publicStatusSovereignByCapital.remove(capitalId);
        } else {
            publicStatusSovereignByCapital.put(capitalId, sovereignId);
        }
        setDirty();
    }

    public int getConfirmedCaseCount(UUID capitalId, UUID targetId) {
        Integer count = getValue(confirmedCaseCountsByCapital, capitalId, targetId);
        return count == null ? 0 : count;
    }

    public int incrementConfirmedCaseCount(UUID capitalId, UUID targetId) {
        if (capitalId == null || targetId == null) {
            return 0;
        }

        Map<UUID, Integer> counts = confirmedCaseCountsByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>());
        int updated = counts.getOrDefault(targetId, 0) + 1;
        counts.put(targetId, updated);
        setDirty();
        return updated;
    }

    public long getLastResolvedDay(UUID capitalId, UUID targetId) {
        return getLong(lastResolvedDayByCapital, capitalId, targetId);
    }

    public void setLastResolvedDay(UUID capitalId, UUID targetId, long day) {
        putLong(lastResolvedDayByCapital, capitalId, targetId, day);
    }

    public CapitalJudgmentType getJudgment(UUID capitalId, UUID targetId) {
        return getValue(judgmentsByCapital, capitalId, targetId);
    }

    public void setJudgment(UUID capitalId, UUID targetId, CapitalJudgmentType judgment) {
        if (capitalId == null || targetId == null) {
            return;
        }

        if (judgment == null) {
            if (removeFromNestedMap(judgmentsByCapital, capitalId, targetId)) {
                setDirty();
            }
            return;
        }

        judgmentsByCapital
                .computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>())
                .put(targetId, judgment);
        setDirty();
    }

    public long getSentenceEndDay(UUID capitalId, UUID targetId) {
        return getLong(sentenceEndDayByCapital, capitalId, targetId);
    }

    public void setSentenceEndDay(UUID capitalId, UUID targetId, long day) {
        putLong(sentenceEndDayByCapital, capitalId, targetId, day);
    }

    public long getLastNpcJudgmentDay(UUID capitalId) {
        return capitalId == null ? Long.MIN_VALUE : lastNpcJudgmentDayByCapital.getOrDefault(capitalId, Long.MIN_VALUE);
    }

    public void setLastNpcJudgmentDay(UUID capitalId, long day) {
        if (capitalId != null) {
            lastNpcJudgmentDayByCapital.put(capitalId, day);
            setDirty();
        }
    }

    public boolean clearJusticeCase(UUID capitalId, UUID targetId) {
        boolean changed = clearArrestWarrant(capitalId, targetId);
        changed |= clearDetainedPrisoner(capitalId, targetId);
        return changed;
    }

    public boolean removeCapital(UUID capitalId) {
        if (capitalId == null) {
            return false;
        }

        boolean changed = false;
        changed |= lastAccusationDayByCapital.remove(capitalId) != null;
        changed |= lastExileScanDayByCapital.remove(capitalId) != null;
        changed |= discoveredExilesByCapital.remove(capitalId) != null;
        changed |= arrestWarrantsByCapital.remove(capitalId) != null;
        changed |= detainedPrisonersByCapital.remove(capitalId) != null;
        changed |= arrestWarrantIssuedGameTimeByCapital.remove(capitalId) != null;
        changed |= detentionStartDayByCapital.remove(capitalId) != null;
        changed |= publicStatusesByCapital.remove(capitalId) != null;
        changed |= publicStatusSovereignByCapital.remove(capitalId) != null;
        changed |= confirmedCaseCountsByCapital.remove(capitalId) != null;
        changed |= lastResolvedDayByCapital.remove(capitalId) != null;
        changed |= judgmentsByCapital.remove(capitalId) != null;
        changed |= sentenceEndDayByCapital.remove(capitalId) != null;
        changed |= lastNpcJudgmentDayByCapital.remove(capitalId) != null;

        if (changed) {
            setDirty();
        }
        return changed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag capitalList = new ListTag();
        Set<UUID> allCapitalIds = new LinkedHashSet<>();
        allCapitalIds.addAll(lastAccusationDayByCapital.keySet());
        allCapitalIds.addAll(lastExileScanDayByCapital.keySet());
        allCapitalIds.addAll(discoveredExilesByCapital.keySet());
        allCapitalIds.addAll(arrestWarrantsByCapital.keySet());
        allCapitalIds.addAll(detainedPrisonersByCapital.keySet());
        allCapitalIds.addAll(arrestWarrantIssuedGameTimeByCapital.keySet());
        allCapitalIds.addAll(detentionStartDayByCapital.keySet());
        allCapitalIds.addAll(publicStatusesByCapital.keySet());
        allCapitalIds.addAll(publicStatusSovereignByCapital.keySet());
        allCapitalIds.addAll(confirmedCaseCountsByCapital.keySet());
        allCapitalIds.addAll(lastResolvedDayByCapital.keySet());
        allCapitalIds.addAll(judgmentsByCapital.keySet());
        allCapitalIds.addAll(sentenceEndDayByCapital.keySet());
        allCapitalIds.addAll(lastNpcJudgmentDayByCapital.keySet());

        for (UUID capitalId : allCapitalIds) {
            CompoundTag capitalTag = new CompoundTag();
            capitalTag.putUUID(KEY_CAPITAL_ID, capitalId);
            capitalTag.put(KEY_PLAYERS, writeUuidLongMap(lastAccusationDayByCapital.get(capitalId), KEY_LAST_ACCUSATION_DAY, KEY_PLAYER_ID));

            if (lastExileScanDayByCapital.containsKey(capitalId)) {
                capitalTag.putLong(KEY_LAST_EXILE_SCAN_DAY, lastExileScanDayByCapital.get(capitalId));
            }
            if (publicStatusSovereignByCapital.containsKey(capitalId)) {
                capitalTag.putUUID(KEY_PUBLIC_STATUS_SOVEREIGN, publicStatusSovereignByCapital.get(capitalId));
            }
            if (lastNpcJudgmentDayByCapital.containsKey(capitalId)) {
                capitalTag.putLong(KEY_LAST_NPC_JUDGMENT_DAY, lastNpcJudgmentDayByCapital.get(capitalId));
            }

            capitalTag.put(KEY_DISCOVERED_EXILES, writeUuidSet(discoveredExilesByCapital.get(capitalId)));
            capitalTag.put(KEY_ARREST_WARRANTS, writeUuidSet(arrestWarrantsByCapital.get(capitalId)));
            capitalTag.put(KEY_DETAINED_PRISONERS, writeUuidSet(detainedPrisonersByCapital.get(capitalId)));
            capitalTag.put(KEY_ARREST_WARRANT_TIMES, writeUuidLongMap(arrestWarrantIssuedGameTimeByCapital.get(capitalId), KEY_GAME_TIME, KEY_TARGET_ID));
            capitalTag.put(KEY_DETENTION_START_DAYS, writeUuidLongMap(detentionStartDayByCapital.get(capitalId), KEY_DAY, KEY_TARGET_ID));
            capitalTag.put(KEY_PUBLIC_STATUSES, writeStatusMap(publicStatusesByCapital.get(capitalId)));
            capitalTag.put(KEY_CONFIRMED_CASE_COUNTS, writeUuidIntMap(confirmedCaseCountsByCapital.get(capitalId)));
            capitalTag.put(KEY_LAST_RESOLVED_DAYS, writeUuidLongMap(lastResolvedDayByCapital.get(capitalId), KEY_DAY, KEY_TARGET_ID));
            capitalTag.put(KEY_JUDGMENTS, writeJudgmentMap(judgmentsByCapital.get(capitalId)));
            capitalTag.put(KEY_SENTENCE_END_DAYS, writeUuidLongMap(sentenceEndDayByCapital.get(capitalId), KEY_DAY, KEY_TARGET_ID));
            capitalList.add(capitalTag);
        }

        tag.put(KEY_CAPITALS, capitalList);
        return tag;
    }

    public static CapitalJusticeSavedData load(CompoundTag tag) {
        CapitalJusticeSavedData data = new CapitalJusticeSavedData();
        ListTag capitalList = tag.getList(KEY_CAPITALS, Tag.TAG_COMPOUND);

        for (Tag raw : capitalList) {
            CompoundTag capitalTag = (CompoundTag) raw;
            if (!capitalTag.hasUUID(KEY_CAPITAL_ID)) {
                continue;
            }

            UUID capitalId = capitalTag.getUUID(KEY_CAPITAL_ID);
            putIfNotEmpty(data.lastAccusationDayByCapital, capitalId, readUuidLongMap(capitalTag.getList(KEY_PLAYERS, Tag.TAG_COMPOUND), KEY_LAST_ACCUSATION_DAY, KEY_PLAYER_ID));

            if (capitalTag.contains(KEY_LAST_EXILE_SCAN_DAY)) {
                data.lastExileScanDayByCapital.put(capitalId, capitalTag.getLong(KEY_LAST_EXILE_SCAN_DAY));
            }
            if (capitalTag.hasUUID(KEY_PUBLIC_STATUS_SOVEREIGN)) {
                data.publicStatusSovereignByCapital.put(capitalId, capitalTag.getUUID(KEY_PUBLIC_STATUS_SOVEREIGN));
            }
            if (capitalTag.contains(KEY_LAST_NPC_JUDGMENT_DAY)) {
                data.lastNpcJudgmentDayByCapital.put(capitalId, capitalTag.getLong(KEY_LAST_NPC_JUDGMENT_DAY));
            }

            putIfNotEmpty(data.discoveredExilesByCapital, capitalId, readUuidSet(capitalTag.getList(KEY_DISCOVERED_EXILES, Tag.TAG_COMPOUND)));
            putIfNotEmpty(data.arrestWarrantsByCapital, capitalId, readUuidSet(capitalTag.getList(KEY_ARREST_WARRANTS, Tag.TAG_COMPOUND)));
            putIfNotEmpty(data.detainedPrisonersByCapital, capitalId, readUuidSet(capitalTag.getList(KEY_DETAINED_PRISONERS, Tag.TAG_COMPOUND)));
            putIfNotEmpty(data.arrestWarrantIssuedGameTimeByCapital, capitalId, readUuidLongMap(capitalTag.getList(KEY_ARREST_WARRANT_TIMES, Tag.TAG_COMPOUND), KEY_GAME_TIME, KEY_TARGET_ID));
            putIfNotEmpty(data.detentionStartDayByCapital, capitalId, readUuidLongMap(capitalTag.getList(KEY_DETENTION_START_DAYS, Tag.TAG_COMPOUND), KEY_DAY, KEY_TARGET_ID));
            putIfNotEmpty(data.publicStatusesByCapital, capitalId, readStatusMap(capitalTag.getList(KEY_PUBLIC_STATUSES, Tag.TAG_COMPOUND)));
            putIfNotEmpty(data.confirmedCaseCountsByCapital, capitalId, readUuidIntMap(capitalTag.getList(KEY_CONFIRMED_CASE_COUNTS, Tag.TAG_COMPOUND)));
            putIfNotEmpty(data.lastResolvedDayByCapital, capitalId, readUuidLongMap(capitalTag.getList(KEY_LAST_RESOLVED_DAYS, Tag.TAG_COMPOUND), KEY_DAY, KEY_TARGET_ID));
            putIfNotEmpty(data.judgmentsByCapital, capitalId, readJudgmentMap(capitalTag.getList(KEY_JUDGMENTS, Tag.TAG_COMPOUND)));
            putIfNotEmpty(data.sentenceEndDayByCapital, capitalId, readUuidLongMap(capitalTag.getList(KEY_SENTENCE_END_DAYS, Tag.TAG_COMPOUND), KEY_DAY, KEY_TARGET_ID));
        }

        return data;
    }

    private static boolean contains(Map<UUID, Set<UUID>> map, UUID capitalId, UUID targetId) {
        Set<UUID> values = capitalId == null ? null : map.get(capitalId);
        return targetId != null && values != null && values.contains(targetId);
    }

    private static Set<UUID> snapshot(Map<UUID, Set<UUID>> map, UUID capitalId) {
        Set<UUID> values = capitalId == null ? null : map.get(capitalId);
        return values == null || values.isEmpty() ? Set.of() : new LinkedHashSet<>(values);
    }

    private void add(Map<UUID, Set<UUID>> map, UUID capitalId, UUID targetId) {
        if (capitalId != null && targetId != null && map.computeIfAbsent(capitalId, ignored -> new LinkedHashSet<>()).add(targetId)) {
            setDirty();
        }
    }

    private boolean remove(Map<UUID, Set<UUID>> map, UUID capitalId, UUID targetId) {
        boolean changed = removeWithoutDirty(map, capitalId, targetId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    private static boolean removeWithoutDirty(Map<UUID, Set<UUID>> map, UUID capitalId, UUID targetId) {
        Set<UUID> values = capitalId == null ? null : map.get(capitalId);
        return targetId != null && values != null && values.remove(targetId);
    }

    private static <T> T getValue(Map<UUID, Map<UUID, T>> map, UUID capitalId, UUID targetId) {
        Map<UUID, T> values = capitalId == null ? null : map.get(capitalId);
        return targetId == null || values == null ? null : values.get(targetId);
    }

    private static long getLong(Map<UUID, Map<UUID, Long>> map, UUID capitalId, UUID targetId) {
        Long value = getValue(map, capitalId, targetId);
        return value == null ? Long.MIN_VALUE : value;
    }

    private void putLong(Map<UUID, Map<UUID, Long>> map, UUID capitalId, UUID targetId, long value) {
        if (capitalId != null && targetId != null) {
            map.computeIfAbsent(capitalId, ignored -> new LinkedHashMap<>()).put(targetId, value);
            setDirty();
        }
    }

    private static <T> boolean removeFromNestedMap(Map<UUID, Map<UUID, T>> map, UUID capitalId, UUID targetId) {
        Map<UUID, T> values = capitalId == null ? null : map.get(capitalId);
        return targetId != null && values != null && values.remove(targetId) != null;
    }

    private static ListTag writeUuidSet(Set<UUID> values) {
        ListTag list = new ListTag();
        if (values != null) {
            for (UUID value : values) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID(KEY_TARGET_ID, value);
                list.add(entry);
            }
        }
        return list;
    }

    private static Set<UUID> readUuidSet(ListTag list) {
        Set<UUID> values = new LinkedHashSet<>();
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            if (entry.hasUUID(KEY_TARGET_ID)) {
                values.add(entry.getUUID(KEY_TARGET_ID));
            }
        }
        return values;
    }

    private static ListTag writeUuidLongMap(Map<UUID, Long> values, String valueKey, String uuidKey) {
        ListTag list = new ListTag();
        if (values != null) {
            for (Map.Entry<UUID, Long> value : values.entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID(uuidKey, value.getKey());
                entry.putLong(valueKey, value.getValue());
                list.add(entry);
            }
        }
        return list;
    }

    private static Map<UUID, Long> readUuidLongMap(ListTag list, String valueKey, String uuidKey) {
        Map<UUID, Long> values = new LinkedHashMap<>();
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            if (entry.hasUUID(uuidKey)) {
                values.put(entry.getUUID(uuidKey), entry.getLong(valueKey));
            }
        }
        return values;
    }

    private static ListTag writeUuidIntMap(Map<UUID, Integer> values) {
        ListTag list = new ListTag();
        if (values != null) {
            for (Map.Entry<UUID, Integer> value : values.entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID(KEY_TARGET_ID, value.getKey());
                entry.putInt(KEY_COUNT, value.getValue());
                list.add(entry);
            }
        }
        return list;
    }

    private static Map<UUID, Integer> readUuidIntMap(ListTag list) {
        Map<UUID, Integer> values = new LinkedHashMap<>();
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            if (entry.hasUUID(KEY_TARGET_ID)) {
                values.put(entry.getUUID(KEY_TARGET_ID), Math.max(0, entry.getInt(KEY_COUNT)));
            }
        }
        return values;
    }

    private static ListTag writeStatusMap(Map<UUID, CapitalPublicCrownStatus> values) {
        ListTag list = new ListTag();
        if (values != null) {
            for (Map.Entry<UUID, CapitalPublicCrownStatus> value : values.entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID(KEY_TARGET_ID, value.getKey());
                entry.putString(KEY_STATUS, value.getValue().getSerializedName());
                list.add(entry);
            }
        }
        return list;
    }

    private static Map<UUID, CapitalPublicCrownStatus> readStatusMap(ListTag list) {
        Map<UUID, CapitalPublicCrownStatus> values = new LinkedHashMap<>();
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            CapitalPublicCrownStatus status = CapitalPublicCrownStatus.fromSerializedName(entry.getString(KEY_STATUS));
            if (entry.hasUUID(KEY_TARGET_ID) && status != null) {
                values.put(entry.getUUID(KEY_TARGET_ID), status);
            }
        }
        return values;
    }

    private static ListTag writeJudgmentMap(Map<UUID, CapitalJudgmentType> values) {
        ListTag list = new ListTag();
        if (values != null) {
            for (Map.Entry<UUID, CapitalJudgmentType> value : values.entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID(KEY_TARGET_ID, value.getKey());
                entry.putString(KEY_JUDGMENT, value.getValue().getSerializedName());
                list.add(entry);
            }
        }
        return list;
    }

    private static Map<UUID, CapitalJudgmentType> readJudgmentMap(ListTag list) {
        Map<UUID, CapitalJudgmentType> values = new LinkedHashMap<>();
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            CapitalJudgmentType judgment = CapitalJudgmentType.fromSerializedName(entry.getString(KEY_JUDGMENT));
            if (entry.hasUUID(KEY_TARGET_ID) && judgment != null) {
                values.put(entry.getUUID(KEY_TARGET_ID), judgment);
            }
        }
        return values;
    }

    private static <T> void putIfNotEmpty(Map<UUID, T> destination, UUID capitalId, T value) {
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        if (value instanceof Set<?> set && set.isEmpty()) {
            return;
        }
        destination.put(capitalId, value);
    }
}