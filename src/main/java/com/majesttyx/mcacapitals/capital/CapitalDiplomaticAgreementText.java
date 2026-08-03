package com.majesttyx.mcacapitals.capital;

import net.minecraft.server.level.ServerLevel;

final class CapitalDiplomaticAgreementText {

    private CapitalDiplomaticAgreementText() {
    }

    static String stateDisplay(CapitalDiplomaticState state) {
        return switch (state) {
            case PEACE -> "Peace";
            case NON_AGGRESSION_PACT -> "Non-Aggression Pact";
            case ALLIANCE -> "Alliance";
            case TRUCE -> "Truce";
            case WAR -> "War";
        };
    }

    static String capitalName(ServerLevel level, CapitalRecord capital) {
        return CapitalDiplomaticCorrespondenceService.getCapitalName(level, capital);
    }

    static String withIndefiniteArticle(String value) {
        if (value == null || value.isBlank()) {
            return "a diplomatic agreement";
        }
        char first = Character.toLowerCase(value.trim().charAt(0));
        String article = first == 'a' || first == 'e' || first == 'i'
                || first == 'o' || first == 'u'
                ? "an "
                : "a ";
        return article + value.trim();
    }

    static String capitalizedWithIndefiniteArticle(String value) {
        String phrase = withIndefiniteArticle(value);
        return Character.toUpperCase(phrase.charAt(0)) + phrase.substring(1);
    }
}
