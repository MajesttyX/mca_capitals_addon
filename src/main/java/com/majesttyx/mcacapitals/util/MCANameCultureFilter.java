package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.config.MCACapitalsConfig;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MCANameCultureFilter {

    private static final String[] NAMES_CLASSES = new String[] {
            "forge.net.mca.resources.Names",
            "net.mca.resources.Names",
            "fabric.net.mca.resources.Names",
            "quilt.net.mca.resources.Names",
            "net.conczin.mca.resources.Names"
    };

    private MCANameCultureFilter() {
    }

    public static void applyConfiguredCultureFilter() {
        List<String> loadedRegions = getLoadedRegions();
        if (loadedRegions == null || loadedRegions.isEmpty()) {
            return;
        }

        Set<String> enabled = MCACapitalsConfig.enabledCultureNameBuckets();
        if (enabled.isEmpty()) {
            return;
        }

        LinkedHashSet<String> filtered = new LinkedHashSet<>();

        for (String region : loadedRegions) {
            String normalized = MCACapitalsConfig.normalizeCultureNameBucket(region);
            if (enabled.contains(normalized)) {
                filtered.add(region);
            }
        }

        if (filtered.isEmpty()) {
            MCACapitals.LOGGER.warn("MCA Capitals name culture filter removed every MCA name bucket. Keeping MCA Reborn's original bucket list instead.");
            return;
        }

        int originalSize = loadedRegions.size();
        loadedRegions.clear();
        loadedRegions.addAll(filtered);

        MCACapitals.LOGGER.info(
                "MCA Capitals enabled {} of {} MCA Reborn name culture buckets: {}",
                loadedRegions.size(),
                originalSize,
                loadedRegions
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> getLoadedRegions() {
        for (String className : NAMES_CLASSES) {
            try {
                Class<?> namesClass = Class.forName(className);
                Field field = namesClass.getDeclaredField("REGION_NAMES");
                field.setAccessible(true);
                Object value = field.get(null);

                if (value instanceof List<?> list) {
                    return (List<String>) list;
                }
            } catch (Throwable ignored) {
                // Try the next MCA package path.
            }
        }

        return null;
    }
}