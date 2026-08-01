package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.MCACapitals;
import forge.net.mca.resources.Names;
import net.minecraft.world.entity.Entity;

import java.util.Locale;

public final class MCANameRegionBridge {

    public static final String DEFAULT_REGION = "common";

    private MCANameRegionBridge() {
    }

    public static String getCitizenNameRegion(Entity entity) {
        if (entity == null) {
            return DEFAULT_REGION;
        }

        try {
            String region = Names.getCitizenNation(entity);
            if (region == null || region.isBlank()) {
                return DEFAULT_REGION;
            }
            return normalizeRegion(region);
        } catch (Exception ex) {
            MCACapitals.LOGGER.debug(
                    "Could not resolve MCA name region for {}. Using common surname bucket.",
                    entity.getUUID(),
                    ex
            );
            return DEFAULT_REGION;
        }
    }

    public static String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return DEFAULT_REGION;
        }

        String normalized = region.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return DEFAULT_REGION;
        }

        normalized = normalized.replaceAll("[^a-z0-9_\\-]", "");
        return normalized.isBlank() ? DEFAULT_REGION : normalized;
    }
}