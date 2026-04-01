package com.example.mcacapitals.dialogue;

import net.minecraft.util.RandomSource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CapitalDialogueLibrary {
    private static final Map<CapitalDialogueKey, List<String>> LINES = buildLines();

    private CapitalDialogueLibrary() {
    }

    public static String getRandomLine(CapitalDialogueKey key, RandomSource random, Object... args) {
        List<String> options = LINES.get(key);
        if (options == null || options.isEmpty()) {
            return "";
        }

        RandomSource actualRandom = random != null ? random : RandomSource.create();
        String template = options.get(actualRandom.nextInt(options.size()));
        if (template == null) {
            return "";
        }

        return args == null || args.length == 0 ? template : template.formatted(args);
    }

    public static int getLineCount(CapitalDialogueKey key) {
        List<String> options = LINES.get(key);
        return options == null ? 0 : options.size();
    }

    public static String getIndexedLine(CapitalDialogueKey key, int index, Object... args) {
        List<String> options = LINES.get(key);
        if (options == null || options.isEmpty()) {
            return "";
        }

        int safeIndex = Math.floorMod(index, options.size());
        String template = options.get(safeIndex);
        if (template == null) {
            return "";
        }

        return args == null || args.length == 0 ? template : template.formatted(args);
    }

    private static Map<CapitalDialogueKey, List<String>> buildLines() {
        Map<CapitalDialogueKey, List<String>> lines = new EnumMap<>(CapitalDialogueKey.class);
        CapitalDialogueLibraryCommon.register(lines);
        CapitalDialogueLibrarySovereign.register(lines);
        CapitalDialogueLibraryTitles.register(lines);
        CapitalDialogueLibraryBetrothal.register(lines);
        CapitalDialogueLibraryNews.register(lines);
        return lines;
    }
}