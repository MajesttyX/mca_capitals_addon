package com.majesttyx.mcacapitals.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MCACapitalsConfig {

    public static final List<String> DEFAULT_CULTURE_NAME_BUCKETS = List.of(
            "albania",
            "arabia",
            "armenia",
            "austria",
            "azerbaijan",
            "belarus",
            "belgium",
            "bosnia",
            "bulgaria",
            "china",
            "croatia",
            "czechrepublic",
            "denmark",
            "eastfrisia",
            "estonia",
            "finland",
            "france",
            "georgia",
            "germany",
            "greatbritain",
            "greece",
            "hungary",
            "iceland",
            "india",
            "ireland",
            "israel",
            "italy",
            "japan",
            "kazakhstan",
            "korea",
            "kosovo",
            "latvia",
            "lithuania",
            "luxembourg",
            "macedonia",
            "malta",
            "modernusa",
            "moldova",
            "montenegro",
            "norway",
            "poland",
            "portugal",
            "romania",
            "russia",
            "serbia",
            "slovakia",
            "slovenia",
            "spain",
            "sweden",
            "swiss",
            "thenetherlands",
            "turkey",
            "ukraine",
            "usa",
            "vietnam"
    );

    public static final List<String> FANTASY_CULTURE_NAME_BUCKETS = List.of(
            "dragonborn",
            "fae",
            "merfolk",
            "dwarven",
            "elven",
            "folklore",
            "tiefling",
            "aasimar"
    );

    public static final List<String> ALL_CULTURE_NAME_BUCKETS = buildAllCultureNameBuckets();

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_CULTURE_NAME_BUCKETS;

    private static final Set<String> VALID_CULTURE_NAME_BUCKETS = Set.copyOf(ALL_CULTURE_NAME_BUCKETS);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("names");

        ENABLED_CULTURE_NAME_BUCKETS = builder
                .comment(
                        "MCA Reborn name culture buckets that are allowed to appear in the world.",
                        "All existing MCA Reborn 1.21.1 culture buckets and MCA Capitals fantasy culture buckets are enabled by default.",
                        "Fantasy buckets: dragonborn, fae, merfolk, dwarven, elven, folklore, tiefling, aasimar.",
                        "To force one culture only, leave only one entry, for example: [\"spain\"] or [\"dragonborn\"].",
                        "If this list is empty or contains no valid entries, MCA Capitals keeps all loaded MCA culture buckets enabled."
                )
                .defineListAllowEmpty(
                        "enabledCultureNameBuckets",
                        () -> new ArrayList<>(ALL_CULTURE_NAME_BUCKETS),
                        () -> "dragonborn",
                        MCACapitalsConfig::isValidCultureNameBucketValue
                );

        builder.pop();

        SPEC = builder.build();
    }

    private MCACapitalsConfig() {
    }

    public static Set<String> enabledCultureNameBuckets() {
        LinkedHashSet<String> enabled = new LinkedHashSet<>();

        for (String value : ENABLED_CULTURE_NAME_BUCKETS.get()) {
            String normalized = normalizeCultureNameBucket(value);
            if (VALID_CULTURE_NAME_BUCKETS.contains(normalized)) {
                enabled.add(normalized);
            }
        }

        return enabled;
    }

    public static String normalizeCultureNameBucket(String bucket) {
        if (bucket == null) {
            return "";
        }

        return bucket.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_\\-]", "");
    }

    private static boolean isValidCultureNameBucketValue(Object value) {
        if (!(value instanceof String bucket)) {
            return false;
        }

        String normalized = normalizeCultureNameBucket(bucket);
        return VALID_CULTURE_NAME_BUCKETS.contains(normalized);
    }

    private static List<String> buildAllCultureNameBuckets() {
        ArrayList<String> buckets = new ArrayList<>(DEFAULT_CULTURE_NAME_BUCKETS);
        buckets.addAll(FANTASY_CULTURE_NAME_BUCKETS);
        return List.copyOf(buckets);
    }
}