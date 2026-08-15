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

public final class PlayerCapitalAllegianceSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_player_allegiance";

    private static final String KEY_RECORDS = "Records";
    private static final String KEY_DECLINED_PROMPTS = "DeclinedPrompts";
    private static final String KEY_PLAYER_ID = "PlayerId";
    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_LAST_CHANGE_DAY = "LastChangeDay";

    private final Map<UUID, AllegianceRecord> records = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> declinedPrompts = new LinkedHashMap<>();

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

    public boolean hasDeclinedPrompt(UUID playerId, UUID capitalId) {
        if (playerId == null || capitalId == null) {
            return false;
        }

        Set<UUID> capitalIds = declinedPrompts.get(playerId);
        return capitalIds != null && capitalIds.contains(capitalId);
    }

    public void markPromptDeclined(UUID playerId, UUID capitalId) {
        if (playerId == null || capitalId == null) {
            return;
        }

        Set<UUID> capitalIds = declinedPrompts.computeIfAbsent(
                playerId,
                ignored -> new LinkedHashSet<>()
        );
        if (capitalIds.add(capitalId)) {
            setDirty();
        }
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

        ListTag declinedPromptTags = new ListTag();
        for (Map.Entry<UUID, Set<UUID>> entry : declinedPrompts.entrySet()) {
            for (UUID capitalId : entry.getValue()) {
                if (capitalId == null) {
                    continue;
                }
                CompoundTag declinedTag = new CompoundTag();
                declinedTag.putUUID(KEY_PLAYER_ID, entry.getKey());
                declinedTag.putUUID(KEY_CAPITAL_ID, capitalId);
                declinedPromptTags.add(declinedTag);
            }
        }
        tag.put(KEY_DECLINED_PROMPTS, declinedPromptTags);
        return tag;
    }

    public static PlayerCapitalAllegianceSavedData load(
            CompoundTag tag
    ) {
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

        ListTag declinedPromptTags = tag.getList(
                KEY_DECLINED_PROMPTS,
                Tag.TAG_COMPOUND
        );
        for (Tag raw : declinedPromptTags) {
            CompoundTag declinedTag = (CompoundTag) raw;
            if (!declinedTag.hasUUID(KEY_PLAYER_ID)
                    || !declinedTag.hasUUID(KEY_CAPITAL_ID)) {
                continue;
            }
            UUID playerId = declinedTag.getUUID(KEY_PLAYER_ID);
            UUID capitalId = declinedTag.getUUID(KEY_CAPITAL_ID);
            data.declinedPrompts
                    .computeIfAbsent(playerId, ignored -> new LinkedHashSet<>())
                    .add(capitalId);
        }

        return data;
    }

    public record AllegianceRecord(UUID capitalId, long lastChangeDay) {
    }
}
