package com.majesttyx.mcacapitals.dialogue;

import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CapitalDialogueTranslationIndex {

    private static final int MAX_VARIANT_INDEX = 999;
    private static final Map<String, List<String>> CACHE = new HashMap<>();
    private static Language cachedLanguage = Language.getInstance();

    private CapitalDialogueTranslationIndex() {
    }

    static boolean hasKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        Language language = Language.getInstance();
        String value = language.getOrDefault(key);
        return value != null && !value.isBlank() && !value.equals(key);
    }

    static List<String> findDotNumberedKeys(String baseKey) {
        if (baseKey == null || baseKey.isBlank()) {
            return List.of();
        }

        Language language = Language.getInstance();
        synchronized (CACHE) {
            if (language != cachedLanguage) {
                CACHE.clear();
                cachedLanguage = language;
            }

            List<String> cached = CACHE.get(baseKey);
            if (cached != null) {
                return cached;
            }

            List<String> keys = new ArrayList<>();
            for (int index = 1; index <= MAX_VARIANT_INDEX; index++) {
                String key = baseKey + "." + String.format(Locale.ROOT, "%02d", index);
                String value = language.getOrDefault(key);
                if (value != null && !value.isBlank() && !value.equals(key)) {
                    keys.add(key);
                }
            }

            List<String> result = List.copyOf(keys);
            CACHE.put(baseKey, result);
            return result;
        }
    }
}
