package com.majesttyx.mcacapitals.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CapitalWarSavedData extends SavedData {

    public static final String DATA_NAME = "mcacapitals_war_data";

    private static final String KEY_GRIEVANCES = "Grievances";
    private static final String KEY_RECOVERY = "Recovery";
    private static final String KEY_UNJUST_PENALTIES = "UnjustPenalties";
    private static final String KEY_SOURCE_CAPITAL_ID = "SourceCapitalId";
    private static final String KEY_TARGET_CAPITAL_ID = "TargetCapitalId";
    private static final String KEY_CAUSE = "Cause";
    private static final String KEY_EXPIRES_AT_DAY = "ExpiresAtDay";
    private static final String KEY_CAPITAL_ID = "CapitalId";
    private static final String KEY_AVAILABLE_AT_DAY = "AvailableAtDay";

    private final Map<CapitalRouteKey, Grievance> grievances =
            new LinkedHashMap<>();
    private final Map<UUID, Long> campaignAvailableDays =
            new LinkedHashMap<>();
    private final Map<UUID, Long> unjustPenaltyUntilDays =
            new LinkedHashMap<>();

    public void recordGrievance(
            UUID sourceCapitalId,
            UUID targetCapitalId,
            CapitalWarCause cause,
            long expiresAtDay
    ) {
        if (sourceCapitalId == null
                || targetCapitalId == null
                || sourceCapitalId.equals(targetCapitalId)
                || cause == null
                || !cause.isJustified()) {
            return;
        }

        CapitalRouteKey key = new CapitalRouteKey(
                sourceCapitalId,
                targetCapitalId
        );
        Grievance existing = grievances.get(key);
        long normalizedExpiry = Math.max(0L, expiresAtDay);

        if (existing == null
                || priority(cause) >= priority(existing.cause())
                || existing.expiresAtDay() < normalizedExpiry) {
            grievances.put(
                    key,
                    new Grievance(cause, normalizedExpiry)
            );
            setDirty();
        }
    }

    public CapitalWarCause getGrievance(
            UUID sourceCapitalId,
            UUID targetCapitalId,
            long currentDay
    ) {
        if (sourceCapitalId == null
                || targetCapitalId == null
                || sourceCapitalId.equals(targetCapitalId)) {
            return null;
        }

        CapitalRouteKey key = new CapitalRouteKey(
                sourceCapitalId,
                targetCapitalId
        );
        Grievance grievance = grievances.get(key);

        if (grievance == null) {
            return null;
        }

        if (grievance.expiresAtDay() > 0L
                && currentDay > grievance.expiresAtDay()) {
            grievances.remove(key);
            setDirty();
            return null;
        }

        return grievance.cause();
    }

    public void consumeGrievance(
            UUID sourceCapitalId,
            UUID targetCapitalId
    ) {
        if (sourceCapitalId == null
                || targetCapitalId == null
                || sourceCapitalId.equals(targetCapitalId)) {
            return;
        }

        if (grievances.remove(new CapitalRouteKey(
                sourceCapitalId,
                targetCapitalId
        )) != null) {
            setDirty();
        }
    }

    public long getCampaignAvailableDay(UUID capitalId) {
        return capitalId == null
                ? 0L
                : campaignAvailableDays.getOrDefault(capitalId, 0L);
    }

    public void setCampaignAvailableDay(
            UUID capitalId,
            long availableDay
    ) {
        if (capitalId == null) {
            return;
        }

        long normalized = Math.max(0L, availableDay);
        Long previous = campaignAvailableDays.put(capitalId, normalized);

        if (previous == null || previous != normalized) {
            setDirty();
        }
    }

    public long getUnjustPenaltyUntilDay(UUID capitalId) {
        return capitalId == null
                ? 0L
                : unjustPenaltyUntilDays.getOrDefault(capitalId, 0L);
    }

    public void setUnjustPenaltyUntilDay(
            UUID capitalId,
            long untilDay
    ) {
        if (capitalId == null) {
            return;
        }

        long normalized = Math.max(0L, untilDay);
        Long previous = unjustPenaltyUntilDays.put(capitalId, normalized);

        if (previous == null || previous != normalized) {
            setDirty();
        }
    }

    public boolean removeCapital(UUID capitalId) {
        if (capitalId == null) {
            return false;
        }

        boolean changed = grievances.entrySet().removeIf(entry ->
                entry.getKey().sourceCapitalId().equals(capitalId)
                        || entry.getKey().targetCapitalId().equals(capitalId)
        );
        changed |= campaignAvailableDays.remove(capitalId) != null;
        changed |= unjustPenaltyUntilDays.remove(capitalId) != null;

        if (changed) {
            setDirty();
        }

        return changed;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag grievanceTags = new ListTag();

        for (Map.Entry<CapitalRouteKey, Grievance> entry :
                grievances.entrySet()) {
            CompoundTag grievanceTag = new CompoundTag();
            grievanceTag.putUUID(
                    KEY_SOURCE_CAPITAL_ID,
                    entry.getKey().sourceCapitalId()
            );
            grievanceTag.putUUID(
                    KEY_TARGET_CAPITAL_ID,
                    entry.getKey().targetCapitalId()
            );
            grievanceTag.putString(
                    KEY_CAUSE,
                    entry.getValue().cause().getSerializedName()
            );
            grievanceTag.putLong(
                    KEY_EXPIRES_AT_DAY,
                    entry.getValue().expiresAtDay()
            );
            grievanceTags.add(grievanceTag);
        }

        tag.put(KEY_GRIEVANCES, grievanceTags);
        tag.put(KEY_RECOVERY, saveCapitalDays(
                campaignAvailableDays,
                KEY_AVAILABLE_AT_DAY
        ));
        tag.put(KEY_UNJUST_PENALTIES, saveCapitalDays(
                unjustPenaltyUntilDays,
                KEY_EXPIRES_AT_DAY
        ));

        return tag;
    }

    public static CapitalWarSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        CapitalWarSavedData data = new CapitalWarSavedData();

        ListTag grievanceTags = tag.getList(
                KEY_GRIEVANCES,
                Tag.TAG_COMPOUND
        );
        for (Tag rawTag : grievanceTags) {
            CompoundTag grievanceTag = (CompoundTag) rawTag;
            if (!grievanceTag.hasUUID(KEY_SOURCE_CAPITAL_ID)
                    || !grievanceTag.hasUUID(KEY_TARGET_CAPITAL_ID)) {
                continue;
            }

            UUID source = grievanceTag.getUUID(KEY_SOURCE_CAPITAL_ID);
            UUID target = grievanceTag.getUUID(KEY_TARGET_CAPITAL_ID);
            if (source.equals(target)) {
                continue;
            }

            CapitalWarCause cause = CapitalWarCause.fromSerializedName(
                    grievanceTag.getString(KEY_CAUSE)
            );
            if (!cause.isJustified()) {
                continue;
            }

            data.grievances.put(
                    new CapitalRouteKey(source, target),
                    new Grievance(
                            cause,
                            grievanceTag.getLong(KEY_EXPIRES_AT_DAY)
                    )
            );
        }

        loadCapitalDays(
                tag.getList(KEY_RECOVERY, Tag.TAG_COMPOUND),
                KEY_AVAILABLE_AT_DAY,
                data.campaignAvailableDays
        );
        loadCapitalDays(
                tag.getList(KEY_UNJUST_PENALTIES, Tag.TAG_COMPOUND),
                KEY_EXPIRES_AT_DAY,
                data.unjustPenaltyUntilDays
        );

        return data;
    }

    private static ListTag saveCapitalDays(
            Map<UUID, Long> values,
            String valueKey
    ) {
        ListTag tags = new ListTag();
        for (Map.Entry<UUID, Long> entry : values.entrySet()) {
            CompoundTag valueTag = new CompoundTag();
            valueTag.putUUID(KEY_CAPITAL_ID, entry.getKey());
            valueTag.putLong(valueKey, entry.getValue());
            tags.add(valueTag);
        }
        return tags;
    }

    private static void loadCapitalDays(
            ListTag tags,
            String valueKey,
            Map<UUID, Long> destination
    ) {
        for (Tag rawTag : tags) {
            CompoundTag valueTag = (CompoundTag) rawTag;
            if (valueTag.hasUUID(KEY_CAPITAL_ID)) {
                destination.put(
                        valueTag.getUUID(KEY_CAPITAL_ID),
                        Math.max(0L, valueTag.getLong(valueKey))
                );
            }
        }
    }

    private static int priority(CapitalWarCause cause) {
        return switch (cause) {
            case TREATY_BROKEN -> 7;
            case PREVIOUS_AGGRESSION -> 6;
            case ALLY_ATTACKED -> 5;
            case REFUSED_REPARATIONS -> 4;
            case FOREIGN_STORAGE_RAID -> 4;
            case SERIOUS_ASYLUM_DISPUTE -> 4;
            case ASYLUM_DISPUTE -> 3;
            case HARMED_CROWN_OFFICIAL -> 2;
            case HOSTILE_RELATIONS -> 1;
            case UNJUST -> 0;
        };
    }

    private record Grievance(
            CapitalWarCause cause,
            long expiresAtDay
    ) {
    }
}