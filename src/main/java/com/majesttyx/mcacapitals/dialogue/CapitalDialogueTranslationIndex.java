package com.majesttyx.mcacapitals.dialogue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.majesttyx.mcacapitals.MCACapitals;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class CapitalDialogueTranslationIndex {

    private static final int MAX_VARIANT_INDEX = 999;
    private static final String SERVER_KEY_MANIFEST =
            "/data/mcacapitals/server_translation_keys.json";
    private static final Map<String, List<String>> CACHE = new HashMap<>();
    private static final Set<String> KNOWN_KEYS = loadKnownKeys();

    private CapitalDialogueTranslationIndex() {
    }

    static boolean hasKey(String key) {
        return key != null
                && !key.isBlank()
                && KNOWN_KEYS.contains(key);
    }

    static List<String> findDotNumberedKeys(String baseKey) {
        if (baseKey == null || baseKey.isBlank()) {
            return List.of();
        }

        synchronized (CACHE) {
            List<String> cached = CACHE.get(baseKey);
            if (cached != null) {
                return cached;
            }

            List<String> keys = new ArrayList<>();
            for (int index = 1; index <= MAX_VARIANT_INDEX; index++) {
                String key = baseKey + "." + String.format(Locale.ROOT, "%02d", index);
                if (KNOWN_KEYS.contains(key)) {
                    keys.add(key);
                }
            }

            List<String> result = List.copyOf(keys);
            CACHE.put(baseKey, result);
            return result;
        }
    }

    private static Set<String> loadKnownKeys() {
        try (InputStream stream = CapitalDialogueTranslationIndex.class
                .getResourceAsStream(SERVER_KEY_MANIFEST)) {
            if (stream == null) {
                MCACapitals.LOGGER.error(
                        "[MCACapitals] Missing server translation-key manifest: {}",
                        SERVER_KEY_MANIFEST
                );
                return Set.of();
            }

            try (InputStreamReader reader = new InputStreamReader(
                    stream,
                    StandardCharsets.UTF_8
            )) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonArray()) {
                    MCACapitals.LOGGER.error(
                            "[MCACapitals] Invalid server translation-key manifest: {}",
                            SERVER_KEY_MANIFEST
                    );
                    return Set.of();
                }

                Set<String> keys = new HashSet<>();
                for (JsonElement element : root.getAsJsonArray()) {
                    if (element != null && element.isJsonPrimitive()) {
                        String key = element.getAsString();
                        if (key != null && !key.isBlank()) {
                            keys.add(key);
                        }
                    }
                }
                return Set.copyOf(keys);
            }
        } catch (Throwable throwable) {
            MCACapitals.LOGGER.error(
                    "[MCACapitals] Failed to load server translation-key manifest.",
                    throwable
            );
            return Set.of();
        }
    }
}
