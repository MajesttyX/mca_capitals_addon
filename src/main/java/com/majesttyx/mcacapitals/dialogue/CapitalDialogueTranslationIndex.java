package com.majesttyx.mcacapitals.dialogue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.majesttyx.mcacapitals.MCACapitals;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
    private static final Map<String, List<String>> NUMBERED_CACHE = new HashMap<>();
    private static final Set<String> BUNDLED_KEYS = loadBundledKeys();

    private CapitalDialogueTranslationIndex() {
    }

    static boolean hasKey(String key) {
        return key != null
                && !key.isBlank()
                && BUNDLED_KEYS.contains(key);
    }

    static List<String> findDotNumberedKeys(String baseKey) {
        if (baseKey == null || baseKey.isBlank()) {
            return List.of();
        }

        synchronized (NUMBERED_CACHE) {
            List<String> cached = NUMBERED_CACHE.get(baseKey);
            if (cached != null) {
                return cached;
            }

            List<String> keys = new ArrayList<>();
            for (int index = 1; index <= MAX_VARIANT_INDEX; index++) {
                String key = baseKey
                        + "."
                        + String.format(Locale.ROOT, "%02d", index);
                if (BUNDLED_KEYS.contains(key)) {
                    keys.add(key);
                }
            }

            List<String> result = List.copyOf(keys);
            NUMBERED_CACHE.put(baseKey, result);
            return result;
        }
    }

    private static Set<String> loadBundledKeys() {
        Set<String> keys = new HashSet<>();
        ClassLoader loader = CapitalDialogueTranslationIndex.class.getClassLoader();

        for (String personality : CapitalDialoguePersonalityResolver.supportedPersonalities()) {
            loadLanguageKeys(
                    loader,
                    "assets/mcacapitals_dialogue_"
                            + personality
                            + "_call/lang/en_us.json",
                    keys
            );
            loadLanguageKeys(
                    loader,
                    "assets/mcacapitals_dialogue_"
                            + personality
                            + "_response/lang/en_us.json",
                    keys
            );
        }

        if (keys.isEmpty()) {
            MCACapitals.LOGGER.error(
                    "[MCACapitals] No bundled Talk of the Town translation keys could be indexed."
            );
        }

        return Set.copyOf(keys);
    }

    private static void loadLanguageKeys(
            ClassLoader loader,
            String resourcePath,
            Set<String> destination
    ) {
        try (InputStream stream = loader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                MCACapitals.LOGGER.warn(
                        "[MCACapitals] Missing bundled Talk of the Town language resource {}",
                        resourcePath
                );
                return;
            }

            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (!rootElement.isJsonObject()) {
                    return;
                }

                JsonObject root = rootElement.getAsJsonObject();
                destination.addAll(root.keySet());
            }
        } catch (Exception exception) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Failed to index bundled Talk of the Town language resource {}",
                    resourcePath,
                    exception
            );
        }
    }
}
