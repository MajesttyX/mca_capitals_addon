package com.majesttyx.mcacapitals.config;

import com.majesttyx.mcacapitals.MCACapitals;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class MCACapitalsConfig {

    private static final String CONFIG_FILE_NAME = "mcacapitals.properties";
    private static final String ENABLED_CULTURE_NAME_BUCKETS_KEY = "enabledCultureNameBuckets";

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

    private static final Set<String> VALID_CULTURE_NAME_BUCKETS = Set.copyOf(ALL_CULTURE_NAME_BUCKETS);

    private MCACapitalsConfig() {
    }

    public static Set<String> enabledCultureNameBuckets() {
        LinkedHashSet<String> enabled = new LinkedHashSet<>();

        for (String value : loadEnabledCultureNameBuckets()) {
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

    private static List<String> loadEnabledCultureNameBuckets() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        ensureConfigExists(configPath);

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configPath)) {
            properties.load(reader);
        } catch (IOException exception) {
            MCACapitals.LOGGER.warn("Could not read MCA Capitals config. Using default name culture buckets.", exception);
            return ALL_CULTURE_NAME_BUCKETS;
        }

        String raw = properties.getProperty(ENABLED_CULTURE_NAME_BUCKETS_KEY, "");
        if (raw == null || raw.isBlank()) {
            return ALL_CULTURE_NAME_BUCKETS;
        }

        List<String> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            String normalized = normalizeCultureNameBucket(part);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }

        if (values.isEmpty()) {
            return ALL_CULTURE_NAME_BUCKETS;
        }

        return values;
    }

    private static void ensureConfigExists(Path configPath) {
        if (Files.exists(configPath)) {
            return;
        }

        try {
            Files.createDirectories(configPath.getParent());

            Properties properties = new Properties();
            properties.setProperty(ENABLED_CULTURE_NAME_BUCKETS_KEY, String.join(",", ALL_CULTURE_NAME_BUCKETS));

            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(
                        writer,
                        "MCA Capitals Fabric config. Use comma-separated culture bucket names. Leave valid defaults enabled for 1:1 behavior."
                );
            }
        } catch (IOException exception) {
            MCACapitals.LOGGER.warn("Could not create MCA Capitals config. Using default name culture buckets.", exception);
        }
    }

    private static List<String> buildAllCultureNameBuckets() {
        ArrayList<String> buckets = new ArrayList<>(DEFAULT_CULTURE_NAME_BUCKETS);
        buckets.addAll(FANTASY_CULTURE_NAME_BUCKETS);
        return List.copyOf(buckets);
    }
}