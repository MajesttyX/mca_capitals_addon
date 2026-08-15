package com.majesttyx.mcacapitals.identity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class HouseWordsLocalization {

    public static final String TRANSLATION_PREFIX = "mcacapitals.house_words.";

    private static final String ENGLISH_RESOURCE =
            "assets/mcacapitals_house_words/lang/en_us.json";

    private static final Map<String, String> CANONICAL_ENGLISH = loadCanonicalEnglish();

    private HouseWordsLocalization() {
    }

    public static String translationKey(String stableId) {
        String normalizedId = normalizeStableId(stableId);
        return normalizedId.isBlank() ? "" : TRANSLATION_PREFIX + normalizedId;
    }

    public static String canonicalEnglish(String stableId) {
        String key = translationKey(stableId);
        return key.isBlank() ? "" : CANONICAL_ENGLISH.getOrDefault(key, "");
    }

    public static Component displayComponent(String storedText) {
        String normalizedText = normalizeStoredText(storedText);
        if (normalizedText.isBlank()) {
            return Component.empty();
        }

        if (normalizedText.startsWith(TRANSLATION_PREFIX)) {
            String stableId = normalizedText.substring(TRANSLATION_PREFIX.length());
            String key = translationKey(stableId);
            if (!key.isBlank() && CANONICAL_ENGLISH.containsKey(key)) {
                return Component.translatable(key);
            }
            return Component.literal(normalizedText);
        }

        String stableId = stableIdFromLegacyText(normalizedText);
        String key = translationKey(stableId);
        String canonical = CANONICAL_ENGLISH.get(key);
        if (canonical != null && normalizeStoredText(canonical).equals(normalizedText)) {
            return Component.translatable(key);
        }

        return Component.literal(normalizedText);
    }

    static String normalizeDefinitionId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.startsWith(TRANSLATION_PREFIX)) {
            return normalizeStableId(trimmed.substring(TRANSLATION_PREFIX.length()));
        }

        if (trimmed.matches("[a-z0-9_]+")) {
            return normalizeStableId(trimmed);
        }

        return stableIdFromLegacyText(trimmed);
    }

    private static String stableIdFromLegacyText(String value) {
        String normalized = Normalizer.normalize(
                normalizeStoredText(value),
                Normalizer.Form.NFKD
        );

        StringBuilder result = new StringBuilder();
        boolean lastWasSeparator = false;

        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            if (character <= 127 && Character.isLetterOrDigit(character)) {
                result.append(Character.toLowerCase(character));
                lastWasSeparator = false;
            } else if (!lastWasSeparator && result.length() > 0) {
                result.append('_');
                lastWasSeparator = true;
            }
        }

        while (!result.isEmpty() && result.charAt(result.length() - 1) == '_') {
            result.deleteCharAt(result.length() - 1);
        }

        return result.toString();
    }

    private static String normalizeStableId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[a-z0-9_]+") ? normalized : "";
    }

    private static String normalizeStoredText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static Map<String, String> loadCanonicalEnglish() {
        InputStream stream = MCACapitals.class.getClassLoader().getResourceAsStream(ENGLISH_RESOURCE);
        if (stream == null) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Missing canonical House Words language resource at {}",
                    ENGLISH_RESOURCE
            );
            return Map.of();
        }

        try (InputStream inputStream = stream;
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return Map.of();
            }

            JsonObject object = root.getAsJsonObject();
            Map<String, String> loaded = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (!entry.getKey().startsWith(TRANSLATION_PREFIX)
                        || entry.getValue() == null
                        || !entry.getValue().isJsonPrimitive()) {
                    continue;
                }

                String value = normalizeStoredText(entry.getValue().getAsString());
                if (!value.isBlank()) {
                    loaded.put(entry.getKey(), value);
                }
            }

            return Collections.unmodifiableMap(loaded);
        } catch (Exception ex) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Failed to load canonical House Words language resource at {}",
                    ENGLISH_RESOURCE,
                    ex
            );
            return Map.of();
        }
    }
}
