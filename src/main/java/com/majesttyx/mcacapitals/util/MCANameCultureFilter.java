package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.config.MCACapitalsConfig;
import forge.net.mca.resources.Names;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MCANameCultureFilter {

    private MCANameCultureFilter() {
    }

    public static void applyConfiguredCultureFilter() {
        List<String> loadedRegions = Names.REGION_NAMES;
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
}