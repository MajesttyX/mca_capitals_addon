package com.majesttyx.mcacapitals.identity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.util.MCANameRegionBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class SurnamePool {

    private static final ResourceLocation COMMON_SURNAMES = ResourceLocation.fromNamespaceAndPath(
            MCACapitals.MODID,
            "surnames/common_surnames.json"
    );

    private static final String CULTURE_SURNAME_PATH = "surnames/cultures/%s.json";
    private static final List<String> FALLBACK_SURNAMES = List.of("Ashford");

    private static List<String> cachedCommonSurnames;
    private static final Map<String, List<String>> CACHED_CULTURE_SURNAMES = new HashMap<>();

    private SurnamePool() {
    }

    static String generate(ServerLevel level, Entity entity) {
        UUID villagerId = entity == null ? null : entity.getUUID();
        String cultureBucket = MCANameRegionBridge.getCitizenNameRegion(entity);
        List<String> surnames = getSurnames(level, cultureBucket);

        if (surnames.isEmpty()) {
            surnames = FALLBACK_SURNAMES;
        }

        int index = Math.floorMod(
                (String.valueOf(villagerId) + ":" + cultureBucket + ":" + (level == null ? 0L : level.getGameTime())).hashCode(),
                surnames.size()
        );

        return surnames.get(index);
    }

    static List<String> getSurnames(ServerLevel level, String cultureBucket) {
        cultureBucket = MCANameRegionBridge.normalizeRegion(cultureBucket);

        if (!MCANameRegionBridge.DEFAULT_REGION.equals(cultureBucket)) {
            List<String> cultureSurnames = getCultureSurnames(level, cultureBucket);
            if (!cultureSurnames.isEmpty()) {
                return cultureSurnames;
            }
        }

        return getCommonSurnames(level);
    }

    static void clearCache() {
        cachedCommonSurnames = null;
        CACHED_CULTURE_SURNAMES.clear();
    }

    private static List<String> getCommonSurnames(ServerLevel level) {
        if (cachedCommonSurnames != null) {
            return cachedCommonSurnames;
        }

        cachedCommonSurnames = loadSurnames(level, COMMON_SURNAMES, true);
        return cachedCommonSurnames;
    }

    private static List<String> getCultureSurnames(ServerLevel level, String cultureBucket) {
        List<String> cached = CACHED_CULTURE_SURNAMES.get(cultureBucket);
        if (cached != null) {
            return cached;
        }

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                MCACapitals.MODID,
                String.format(CULTURE_SURNAME_PATH, cultureBucket)
        );

        List<String> loaded = loadSurnames(level, location, false);
        CACHED_CULTURE_SURNAMES.put(cultureBucket, loaded);
        return loaded;
    }

    private static List<String> loadSurnames(ServerLevel level, ResourceLocation location, boolean warnAndFallback) {
        if (level == null || level.getServer() == null) {
            return warnAndFallback ? FALLBACK_SURNAMES : Collections.emptyList();
        }

        Optional<Resource> resource = level.getServer().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            if (warnAndFallback) {
                MCACapitals.LOGGER.warn(
                        "MCA Capitals surname list missing at {}. Using fallback surname list.",
                        location
                );
                return FALLBACK_SURNAMES;
            }

            return Collections.emptyList();
        }

        try (InputStream inputStream = resource.get().open();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                if (warnAndFallback) {
                    MCACapitals.LOGGER.warn(
                            "MCA Capitals surname list at {} must be a JSON object. Using fallback surname list.",
                            location
                    );
                    return FALLBACK_SURNAMES;
                }

                MCACapitals.LOGGER.warn(
                        "MCA Capitals culture surname list at {} must be a JSON object. Ignoring this culture bucket.",
                        location
                );
                return Collections.emptyList();
            }

            JsonObject object = root.getAsJsonObject();
            JsonElement surnamesElement = object.get("surnames");

            if (surnamesElement == null || !surnamesElement.isJsonArray()) {
                if (warnAndFallback) {
                    MCACapitals.LOGGER.warn(
                            "MCA Capitals surname list at {} must contain a surnames array. Using fallback surname list.",
                            location
                    );
                    return FALLBACK_SURNAMES;
                }

                MCACapitals.LOGGER.warn(
                        "MCA Capitals culture surname list at {} must contain a surnames array. Ignoring this culture bucket.",
                        location
                );
                return Collections.emptyList();
            }

            LinkedHashSet<String> unique = new LinkedHashSet<>();
            JsonArray surnamesArray = surnamesElement.getAsJsonArray();

            for (JsonElement element : surnamesArray) {
                if (element == null || !element.isJsonPrimitive()) {
                    continue;
                }

                String surname = element.getAsString();
                if (surname == null) {
                    continue;
                }

                surname = surname.trim();
                if (!surname.isBlank()) {
                    unique.add(surname);
                }
            }

            if (unique.isEmpty()) {
                if (warnAndFallback) {
                    MCACapitals.LOGGER.warn(
                            "MCA Capitals surname list at {} has no valid surnames. Using fallback surname list.",
                            location
                    );
                    return FALLBACK_SURNAMES;
                }

                return Collections.emptyList();
            }

            return Collections.unmodifiableList(new ArrayList<>(unique));
        } catch (Exception ex) {
            if (warnAndFallback) {
                MCACapitals.LOGGER.warn(
                        "Failed to load MCA Capitals surname list at {}. Using fallback surname list.",
                        location,
                        ex
                );
                return FALLBACK_SURNAMES;
            }

            MCACapitals.LOGGER.warn(
                    "Failed to load MCA Capitals culture surname list at {}. Ignoring this culture bucket.",
                    location,
                    ex
            );
            return Collections.emptyList();
        }
    }
}