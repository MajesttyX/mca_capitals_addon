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

public final class CapitalPlayerWarrantSavedData extends SavedData {
    public static final String DATA_NAME = "mcacapitals_player_warrants";
    private static final String KEY_LEAVE_ORDERS = "LeaveOrders";
    private static final String KEY_WARRANTS = "Warrants";
    private static final String KEY_SENTENCES = "Sentences";
    private static final String KEY_PENALIZED_CASES = "PenalizedCases";
    private static final String KEY_PLAYER_ID = "PlayerId";
    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_REMAINING_TICKS = "RemainingTicks";
    private static final String KEY_CASE_KEY = "CaseKey";

    private final Map<RouteKey, LeaveOrder> leaveOrders = new LinkedHashMap<>();
    private final Set<RouteKey> warrants = new LinkedHashSet<>();
    private final Map<RouteKey, Long> sentences = new LinkedHashMap<>();
    private final Set<String> penalizedCases = new LinkedHashSet<>();

    public LeaveOrder getLeaveOrder(UUID playerId, UUID capitalId) {
        return leaveOrders.get(new RouteKey(playerId, capitalId));
    }

    public Map<UUID, LeaveOrder> getLeaveOrders(UUID playerId) {
        Map<UUID, LeaveOrder> result = new LinkedHashMap<>();
        for (Map.Entry<RouteKey, LeaveOrder> entry : leaveOrders.entrySet()) {
            if (entry.getKey().playerId().equals(playerId)) {
                result.put(entry.getKey().capitalId(), entry.getValue());
            }
        }
        return result;
    }

    public void setLeaveOrder(
            UUID playerId,
            UUID capitalId,
            long remainingTicks,
            String caseKey
    ) {
        if (playerId == null || capitalId == null) {
            return;
        }
        leaveOrders.put(
                new RouteKey(playerId, capitalId),
                new LeaveOrder(Math.max(0L, remainingTicks), caseKey == null ? "" : caseKey)
        );
        setDirty();
    }

    public boolean updateLeaveOrder(UUID playerId, UUID capitalId, long remainingTicks) {
        RouteKey key = new RouteKey(playerId, capitalId);
        LeaveOrder existing = leaveOrders.get(key);
        if (existing == null) {
            return false;
        }
        leaveOrders.put(
                key,
                new LeaveOrder(Math.max(0L, remainingTicks), existing.caseKey())
        );
        setDirty();
        return true;
    }

    public boolean clearLeaveOrder(UUID playerId, UUID capitalId) {
        if (leaveOrders.remove(new RouteKey(playerId, capitalId)) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean hasWarrant(UUID playerId, UUID capitalId) {
        return warrants.contains(new RouteKey(playerId, capitalId));
    }

    public Set<UUID> getWarrantCapitals(UUID playerId) {
        Set<UUID> result = new LinkedHashSet<>();
        for (RouteKey key : warrants) {
            if (key.playerId().equals(playerId)) {
                result.add(key.capitalId());
            }
        }
        return result;
    }

    public boolean issueWarrant(UUID playerId, UUID capitalId) {
        if (playerId == null || capitalId == null) {
            return false;
        }
        if (warrants.add(new RouteKey(playerId, capitalId))) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean clearWarrant(UUID playerId, UUID capitalId) {
        RouteKey key = new RouteKey(playerId, capitalId);
        boolean changed = warrants.remove(key);
        changed |= sentences.remove(key) != null;
        changed |= leaveOrders.remove(key) != null;
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public long getSentenceRemaining(UUID playerId, UUID capitalId) {
        return sentences.getOrDefault(new RouteKey(playerId, capitalId), 0L);
    }

    public Map<UUID, Long> getSentences(UUID playerId) {
        Map<UUID, Long> result = new LinkedHashMap<>();
        for (Map.Entry<RouteKey, Long> entry : sentences.entrySet()) {
            if (entry.getKey().playerId().equals(playerId)) {
                result.put(entry.getKey().capitalId(), entry.getValue());
            }
        }
        return result;
    }

    public void setSentence(UUID playerId, UUID capitalId, long remainingTicks) {
        if (playerId == null || capitalId == null) {
            return;
        }
        sentences.put(
                new RouteKey(playerId, capitalId),
                Math.max(0L, remainingTicks)
        );
        setDirty();
    }

    public boolean updateSentence(UUID playerId, UUID capitalId, long remainingTicks) {
        RouteKey key = new RouteKey(playerId, capitalId);
        if (!sentences.containsKey(key)) {
            return false;
        }
        sentences.put(key, Math.max(0L, remainingTicks));
        setDirty();
        return true;
    }

    public boolean markCasePenalized(String caseKey) {
        if (caseKey == null || caseKey.isBlank()) {
            return false;
        }
        if (penalizedCases.add(caseKey)) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean isCasePenalized(String caseKey) {
        return caseKey != null && penalizedCases.contains(caseKey);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag leaveOrderTags = new ListTag();
        for (Map.Entry<RouteKey, LeaveOrder> entry : leaveOrders.entrySet()) {
            CompoundTag value = saveRoute(entry.getKey());
            value.putLong(KEY_REMAINING_TICKS, entry.getValue().remainingTicks());
            value.putString(KEY_CASE_KEY, entry.getValue().caseKey());
            leaveOrderTags.add(value);
        }
        tag.put(KEY_LEAVE_ORDERS, leaveOrderTags);

        ListTag warrantTags = new ListTag();
        for (RouteKey key : warrants) {
            warrantTags.add(saveRoute(key));
        }
        tag.put(KEY_WARRANTS, warrantTags);

        ListTag sentenceTags = new ListTag();
        for (Map.Entry<RouteKey, Long> entry : sentences.entrySet()) {
            CompoundTag value = saveRoute(entry.getKey());
            value.putLong(KEY_REMAINING_TICKS, entry.getValue());
            sentenceTags.add(value);
        }
        tag.put(KEY_SENTENCES, sentenceTags);

        ListTag caseTags = new ListTag();
        for (String caseKey : penalizedCases) {
            CompoundTag value = new CompoundTag();
            value.putString(KEY_CASE_KEY, caseKey);
            caseTags.add(value);
        }
        tag.put(KEY_PENALIZED_CASES, caseTags);
        return tag;
    }

    public static CapitalPlayerWarrantSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        CapitalPlayerWarrantSavedData data = new CapitalPlayerWarrantSavedData();
        for (Tag raw : tag.getList(KEY_LEAVE_ORDERS, Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) raw;
            RouteKey key = loadRoute(value);
            if (key != null) {
                data.leaveOrders.put(
                        key,
                        new LeaveOrder(
                                Math.max(0L, value.getLong(KEY_REMAINING_TICKS)),
                                value.getString(KEY_CASE_KEY)
                        )
                );
            }
        }
        for (Tag raw : tag.getList(KEY_WARRANTS, Tag.TAG_COMPOUND)) {
            RouteKey key = loadRoute((CompoundTag) raw);
            if (key != null) {
                data.warrants.add(key);
            }
        }
        for (Tag raw : tag.getList(KEY_SENTENCES, Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) raw;
            RouteKey key = loadRoute(value);
            if (key != null) {
                data.sentences.put(
                        key,
                        Math.max(0L, value.getLong(KEY_REMAINING_TICKS))
                );
            }
        }
        for (Tag raw : tag.getList(KEY_PENALIZED_CASES, Tag.TAG_COMPOUND)) {
            String caseKey = ((CompoundTag) raw).getString(KEY_CASE_KEY);
            if (!caseKey.isBlank()) {
                data.penalizedCases.add(caseKey);
            }
        }
        return data;
    }

    private static CompoundTag saveRoute(RouteKey key) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(KEY_PLAYER_ID, key.playerId());
        tag.putUUID(KEY_CAPITAL_ID, key.capitalId());
        return tag;
    }

    private static RouteKey loadRoute(CompoundTag tag) {
        if (!tag.hasUUID(KEY_PLAYER_ID) || !tag.hasUUID(KEY_CAPITAL_ID)) {
            return null;
        }
        return new RouteKey(tag.getUUID(KEY_PLAYER_ID), tag.getUUID(KEY_CAPITAL_ID));
    }

    private record RouteKey(UUID playerId, UUID capitalId) {
    }

    public record LeaveOrder(long remainingTicks, String caseKey) {
    }
}
