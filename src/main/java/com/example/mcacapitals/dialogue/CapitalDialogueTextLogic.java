package com.example.mcacapitals.dialogue;

import java.util.regex.Pattern;

final class CapitalDialogueTextLogic {

    private static final String[] KNOWN_TITLES = {
            "Queen Dowager",
            "Prince Consort",
            "Queen Consort",
            "King Consort",
            "Heir Apparent",
            "Princess",
            "Prince",
            "Duchess",
            "Duke",
            "Lady",
            "Lord",
            "Dame",
            "Sir",
            "Queen",
            "King"
    };

    private CapitalDialogueTextLogic() {
    }

    static String sanitizeChronicleText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String result = text.trim();

        result = result.replace(", a prince/princess of the realm,", "");
        result = result.replace(", a prince/princess of the realm", "");
        result = result.replace(", a royal child of the realm,", "");
        result = result.replace(", a royal child of the realm", "");

        result = collapseDuplicateTitles(result);

        result = result.replaceAll("\\s{2,}", " ").trim();
        result = result.replace(" .", ".");

        return result;
    }

    static String ensureSentence(String text) {
        if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) {
            return text;
        }
        return text + ".";
    }

    private static String collapseDuplicateTitles(String text) {
        String result = text;

        boolean changed = true;
        while (changed) {
            changed = false;

            for (String title : KNOWN_TITLES) {
                String escaped = Pattern.quote(title);

                String doubled = "(?i)\\b" + escaped + "\\s+" + escaped + "\\b";
                String beforeName = "(?i)\\b" + escaped + "\\s+" + escaped + "\\s+([A-Z][A-Za-z'\\-]*)";
                String tripled = "(?i)\\b" + escaped + "\\s+" + escaped + "\\s+" + escaped + "\\b";

                String updated = result.replaceAll(tripled, title);
                updated = updated.replaceAll(doubled, title);
                updated = updated.replaceAll(beforeName, title + " $1");

                if (!updated.equals(result)) {
                    result = updated;
                    changed = true;
                }
            }
        }

        return result;
    }
}