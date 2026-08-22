package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.Locale;

public final class MCANameRegionBridge {

    public static final String DEFAULT_REGION = "common";

    private static final String[] NAMES_CLASSES = new String[] {
            "fabric.net.conczin.mca.resources.Names"
    };

    private MCANameRegionBridge() {
    }

    public static String getCitizenNameRegion(Entity entity) {
        if (entity == null) {
            return DEFAULT_REGION;
        }

        for (String className : NAMES_CLASSES) {
            String region = tryGetCitizenNation(className, entity);
            if (region != null && !region.isBlank()) {
                return normalizeRegion(region);
            }
        }

        MCACapitals.LOGGER.debug(
                "Could not resolve MCA name region for {}. Using common surname bucket.",
                entity.getUUID()
        );
        return DEFAULT_REGION;
    }

    public static String normalizeRegion(String region) {
        if (region == null) {
            return DEFAULT_REGION;
        }

        String normalized = region.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return DEFAULT_REGION;
        }

        normalized = normalized.replaceAll("[^a-z0-9_\\-]", "");
        return normalized.isBlank() ? DEFAULT_REGION : normalized;
    }

    private static String tryGetCitizenNation(String className, Entity entity) {
        try {
            Class<?> namesClass = Class.forName(className);
            Method method = namesClass.getMethod("getCitizenNation", Entity.class);
            Object value = method.invoke(null, entity);
            return value instanceof String string ? string : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}