package com.majesttyx.mcacapitals.dialogue;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CapitalDialogueLibrary {

    enum Category {
        RANK_OFFICES("rank_offices"),
        CHAT_CHATTER("chat_chatter");

        private final String path;

        Category(String path) {
            this.path = path;
        }

        String path() {
            return path;
        }
    }

    record Definition(Category category) {
        Definition {
            if (category == null) {
                throw new IllegalArgumentException("category");
            }
        }
    }

    private static final Map<CapitalDialogueKey, Definition> DEFINITIONS = buildDefinitions();

    private CapitalDialogueLibrary() {
    }

    public static Component getRandomLine(
            Entity speaker,
            CapitalDialogueKey key,
            RandomSource random,
            Object... args
    ) {
        List<String> translationKeys = translationKeys(speaker, key);
        if (translationKeys.isEmpty()) {
            return null;
        }

        RandomSource actualRandom = random != null ? random : RandomSource.create();
        String translationKey = translationKeys.get(actualRandom.nextInt(translationKeys.size()));
        Object[] translationArgs = args == null ? new Object[0] : args;
        return Component.translatable(translationKey, translationArgs);
    }

    public static int getLineCount(CapitalDialogueKey key) {
        Definition definition = DEFINITIONS.get(key);
        if (definition == null) {
            return 0;
        }

        return CapitalDialogueTranslationIndex.findDotNumberedKeys(
                translationBase(
                        CapitalDialoguePersonalityResolver.defaultPersonality(),
                        definition,
                        key
                )
        ).size();
    }

    public static Component getIndexedLine(
            Entity speaker,
            CapitalDialogueKey key,
            int index,
            Object... args
    ) {
        List<String> translationKeys = translationKeys(speaker, key);
        if (translationKeys.isEmpty()) {
            return null;
        }

        int safeIndex = Math.floorMod(index, translationKeys.size());
        Object[] translationArgs = args == null ? new Object[0] : args;
        return Component.translatable(translationKeys.get(safeIndex), translationArgs);
    }

    static Definition definition(Category category) {
        return new Definition(category);
    }

    private static List<String> translationKeys(Entity speaker, CapitalDialogueKey key) {
        Definition definition = DEFINITIONS.get(key);
        if (definition == null) {
            return List.of();
        }

        String personality = CapitalDialoguePersonalityResolver.resolve(speaker);
        if (personality == null) {
            return List.of();
        }

        List<String> keys = CapitalDialogueTranslationIndex.findDotNumberedKeys(
                translationBase(personality, definition, key)
        );
        if (!keys.isEmpty()) {
            return keys;
        }

        String fallback = CapitalDialoguePersonalityResolver.defaultPersonality();
        if (fallback.equals(personality)) {
            return List.of();
        }

        return CapitalDialogueTranslationIndex.findDotNumberedKeys(
                translationBase(fallback, definition, key)
        );
    }

    private static String translationBase(
            String personality,
            Definition definition,
            CapitalDialogueKey key
    ) {
        return "mcacapitals.dialogue."
                + personality
                + "."
                + definition.category().path()
                + "."
                + key.name().toLowerCase(Locale.ROOT);
    }

    private static Map<CapitalDialogueKey, Definition> buildDefinitions() {
        Map<CapitalDialogueKey, Definition> definitions = new EnumMap<>(CapitalDialogueKey.class);
        CapitalDialogueLibraryCommon.register(definitions);
        CapitalDialogueLibrarySovereign.register(definitions);
        CapitalDialogueLibraryTitles.register(definitions);
        CapitalDialogueLibraryBetrothal.register(definitions);
        CapitalDialogueLibraryNews.register(definitions);
        return Map.copyOf(definitions);
    }
}
