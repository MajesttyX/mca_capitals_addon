package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.MCACapitals;
import net.conczin.mca.resources.Names;
import net.minecraft.world.entity.Entity;

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
            MCACapitals.LOGGER.debug("Could not resolve MCA name region for {}. Using common surname bucket.", entity.getUUID(), ex);
            return DEFAULT_REGION;
        }
    }

    public static String normalizeRegion(String region) {
        if (region == null) {
            return DEFAULT_REGION;
        }

        String normalized = region.trim().toLowerCase();
        if (normalized.isBlank()) {
            return DEFAULT_REGION;
        }
        return normalized.replaceAll("[^a-z0-9_\\-]", "");
    }
}