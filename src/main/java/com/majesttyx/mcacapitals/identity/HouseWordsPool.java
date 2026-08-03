package com.majesttyx.mcacapitals.identity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.data.UsedHouseWordsSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class HouseWordsPool {

    static final String UNASSIGNED = "UNASSIGNED";

    private static final ResourceLocation HOUSE_WORDS = ResourceLocation.fromNamespaceAndPath(
            MCACapitals.MODID,
            "house_words/house_words.json"
    );

    private static final List<String> PERSONALITY_BUCKET_ORDER = List.of(
            UNASSIGNED,
            "FRIENDLY",
            "FLIRTY",
            "PLAYFUL",
            "GLOOMY",
            "SENSITIVE",
            "GREEDY",
            "ODD",
            "CRABBY",
            "EXTROVERTED",
            "INTROVERTED",
            "RELAXED",
            "ANXIOUS",
            "PEACEFUL",
            "UPBEAT"
    );

    private static Map<String, List<String>> cachedHouseWords;

    private HouseWordsPool() {
    }

    static Selection select(ServerLevel level, String founderPersonality, UUID capitalId) {
        Map<String, List<String>> buckets = getBuckets(level);
        UsedHouseWordsSavedData usedData = UsedHouseWordsSavedData.get(level);

        String preferred = normalizeBucket(founderPersonality);

        Selection preferredSelection = randomUnusedFromBucket(level, preferred, buckets, usedData);
        if (preferredSelection.hasWords()) {
            return preferredSelection;
        }

        if (!UNASSIGNED.equals(preferred)) {
            Selection unassignedSelection = randomUnusedFromBucket(level, UNASSIGNED, buckets, usedData);
            if (unassignedSelection.hasWords()) {
                return unassignedSelection;
            }
        }

        Set<String> usedBucketsInCapital = usedData.getUsedBucketsForCapital(capitalId);

        List<String> capitalFreshBuckets = new ArrayList<>();
        for (String bucket : PERSONALITY_BUCKET_ORDER) {
            if (bucket.equals(preferred) || bucket.equals(UNASSIGNED)) {
                continue;
            }
            if (!usedBucketsInCapital.contains(bucket)) {
                capitalFreshBuckets.add(bucket);
            }
        }

        Selection capitalFreshSelection = randomUnusedFromBuckets(level, capitalFreshBuckets, buckets, usedData);
        if (capitalFreshSelection.hasWords()) {
            return capitalFreshSelection;
        }

        List<String> anyOtherBuckets = new ArrayList<>();
        for (String bucket : PERSONALITY_BUCKET_ORDER) {
            if (bucket.equals(preferred) || bucket.equals(UNASSIGNED)) {
                continue;
            }
            anyOtherBuckets.add(bucket);
        }

        Selection anyOtherSelection = randomUnusedFromBuckets(level, anyOtherBuckets, buckets, usedData);
        if (anyOtherSelection.hasWords()) {
            return anyOtherSelection;
        }

        return Selection.empty(preferred);
    }

    static void clearCache() {
        cachedHouseWords = null;
    }

    private static Selection randomUnusedFromBucket(
            ServerLevel level,
            String bucket,
            Map<String, List<String>> buckets,
            UsedHouseWordsSavedData usedData
    ) {
        return randomUnusedFromBuckets(level, List.of(bucket), buckets, usedData);
    }

    private static Selection randomUnusedFromBuckets(
            ServerLevel level,
            List<String> bucketOrder,
            Map<String, List<String>> buckets,
            UsedHouseWordsSavedData usedData
    ) {
        List<Selection> candidates = new ArrayList<>();

        for (String bucket : bucketOrder) {
            List<String> phrases = buckets.getOrDefault(bucket, Collections.emptyList());
            for (String phrase : phrases) {
                if (!phrase.isBlank() && !usedData.isPhraseUsed(phrase)) {
                    candidates.add(new Selection(bucket, phrase));
                }
            }
        }

        if (candidates.isEmpty()) {
            return Selection.empty(bucketOrder.isEmpty() ? UNASSIGNED : bucketOrder.get(0));
        }

        int index = level == null ? 0 : level.random.nextInt(candidates.size());
        return candidates.get(index);
    }

    private static Map<String, List<String>> getBuckets(ServerLevel level) {
        if (cachedHouseWords != null) {
            return cachedHouseWords;
        }

        cachedHouseWords = loadBuckets(level);
        return cachedHouseWords;
    }

    private static Map<String, List<String>> loadBuckets(ServerLevel level) {
        Map<String, List<String>> emptyBuckets = emptyBucketMap();

        if (level == null || level.getServer() == null) {
            return emptyBuckets;
        }

        Optional<Resource> resource = level.getServer().getResourceManager().getResource(HOUSE_WORDS);
        if (resource.isEmpty()) {
            MCACapitals.LOGGER.warn("MCA Capitals House Words list missing at {}.", HOUSE_WORDS);
            return emptyBuckets;
        }

        try (InputStream inputStream = resource.get().open();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                MCACapitals.LOGGER.warn("MCA Capitals House Words list at {} must be a JSON object.", HOUSE_WORDS);
                return emptyBuckets;
            }

            JsonObject object = root.getAsJsonObject();
            Map<String, List<String>> loaded = new LinkedHashMap<>();
            Set<String> globallySeen = new LinkedHashSet<>();

            for (String bucket : PERSONALITY_BUCKET_ORDER) {
                JsonElement bucketElement = object.get(bucket);
                LinkedHashSet<String> uniqueForBucket = new LinkedHashSet<>();

                if (bucketElement != null && bucketElement.isJsonArray()) {
                    JsonArray array = bucketElement.getAsJsonArray();
                    for (JsonElement element : array) {
                        if (element == null || !element.isJsonPrimitive()) {
                            continue;
                        }

                        String phrase = normalizePhrase(element.getAsString());
                        if (phrase.isBlank() || globallySeen.contains(phrase)) {
                            continue;
                        }

                        globallySeen.add(phrase);
                        uniqueForBucket.add(phrase);
                    }
                }

                loaded.put(bucket, Collections.unmodifiableList(new ArrayList<>(uniqueForBucket)));
            }

            return Collections.unmodifiableMap(loaded);
        } catch (Exception ex) {
            MCACapitals.LOGGER.warn("Failed to load MCA Capitals House Words list at {}.", HOUSE_WORDS, ex);
            return emptyBuckets;
        }
    }

    private static Map<String, List<String>> emptyBucketMap() {
        Map<String, List<String>> buckets = new HashMap<>();
        for (String bucket : PERSONALITY_BUCKET_ORDER) {
            buckets.put(bucket, Collections.emptyList());
        }
        return Collections.unmodifiableMap(buckets);
    }

    static String normalizeBucket(String value) {
        if (value == null || value.isBlank()) {
            return UNASSIGNED;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return PERSONALITY_BUCKET_ORDER.contains(normalized) ? normalized : UNASSIGNED;
    }

    private static String normalizePhrase(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    record Selection(String bucket, String phrase) {
        static Selection empty(String preferredBucket) {
            return new Selection(normalizeBucket(preferredBucket), "");
        }

        boolean hasWords() {
            return phrase != null && !phrase.isBlank();
        }
    }
}