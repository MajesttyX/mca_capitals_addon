package com.majesttyx.mcacapitals.dialogue;

import java.util.Map;

final class CapitalDialogueLibrarySovereign {

    private CapitalDialogueLibrarySovereign() {
    }

    static void register(Map<CapitalDialogueKey, CapitalDialogueLibrary.Definition> definitions) {
        definitions.put(CapitalDialogueKey.THRONE_NO_SOVEREIGN, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.THRONE_NOT_REIGNING, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.THRONE_PLAYER_HELD, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.THRONE_POPULATION_TOO_LOW, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.THRONE_LOW_STANDING, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.THRONE_ALREADY_RULES_OTHER, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.THRONE_SUCCESS, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.SEIZE_THRONE_NO_SOVEREIGN, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.SEIZE_THRONE_MISSING_VILLAGE, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.SEIZE_THRONE_NOT_IN_AUDIENCE, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.SEIZE_THRONE_LOW_REPUTATION, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.SEIZE_THRONE_NO_ADVANCEMENT, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.SEIZE_THRONE_NO_COMMANDER_SUPPORT, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.SEIZE_THRONE_SUCCESS, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
    }
}
