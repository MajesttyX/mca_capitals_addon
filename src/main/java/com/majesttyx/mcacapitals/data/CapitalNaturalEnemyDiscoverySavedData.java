package com.majesttyx.mcacapitals.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CapitalNaturalEnemyDiscoverySavedData
        extends SavedData {

    public static final String DATA_NAME =
            "mcacapitals_natural_enemy_discovery_data";

    private static final String KEY_DISCOVERIES =
            "Discoveries";

    private static final String KEY_CAPITAL_ID =
            "CapitalId";

    private static final String KEY_LAST_DISCOVERY_DAY =
            "LastDiscoveryDay";

    private final Map<UUID, Long>
            lastDiscoveryDayByCapital =
            new LinkedHashMap<>();

    public static CapitalNaturalEnemyDiscoverySavedData get(
            ServerLevel level
    ) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        CapitalNaturalEnemyDiscoverySavedData::load,
                        CapitalNaturalEnemyDiscoverySavedData::new,
                        DATA_NAME
                );
    }

    public long getLastDiscoveryDay(
            UUID capitalId
    ) {
        if (capitalId == null) {
            return Long.MIN_VALUE;
        }

        return lastDiscoveryDayByCapital.getOrDefault(
                capitalId,
                Long.MIN_VALUE
        );
    }

    public void setLastDiscoveryDay(
            UUID capitalId,
            long day
    ) {
        if (capitalId == null) {
            return;
        }

        Long previous = lastDiscoveryDayByCapital.put(
                capitalId,
                day
        );

        if (previous == null
                || previous.longValue() != day) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag discoveriesTag =
                new ListTag();

        for (Map.Entry<UUID, Long> entry :
                lastDiscoveryDayByCapital.entrySet()) {
            if (entry.getKey() == null
                    || entry.getValue() == null) {
                continue;
            }

            CompoundTag discoveryTag =
                    new CompoundTag();

            discoveryTag.putUUID(
                    KEY_CAPITAL_ID,
                    entry.getKey()
            );

            discoveryTag.putLong(
                    KEY_LAST_DISCOVERY_DAY,
                    entry.getValue()
            );

            discoveriesTag.add(discoveryTag);
        }

        tag.put(
                KEY_DISCOVERIES,
                discoveriesTag
        );

        return tag;
    }

    public static CapitalNaturalEnemyDiscoverySavedData load(
            CompoundTag tag
    ) {
        CapitalNaturalEnemyDiscoverySavedData data =
                new CapitalNaturalEnemyDiscoverySavedData();

        ListTag discoveriesTag = tag.getList(
                KEY_DISCOVERIES,
                Tag.TAG_COMPOUND
        );

        for (Tag rawEntry : discoveriesTag) {
            CompoundTag discoveryTag =
                    (CompoundTag) rawEntry;

            if (!discoveryTag.hasUUID(KEY_CAPITAL_ID)
                    || !discoveryTag.contains(
                    KEY_LAST_DISCOVERY_DAY,
                    Tag.TAG_LONG
            )) {
                continue;
            }

            data.lastDiscoveryDayByCapital.put(
                    discoveryTag.getUUID(KEY_CAPITAL_ID),
                    discoveryTag.getLong(KEY_LAST_DISCOVERY_DAY)
            );
        }

        return data;
    }
}