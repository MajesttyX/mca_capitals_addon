package com.majesttyx.mcacapitals.dialogue;

import java.util.Map;

final class CapitalDialogueLibraryTitles {

    private CapitalDialogueLibraryTitles() {
    }

    static void register(Map<CapitalDialogueKey, CapitalDialogueLibrary.Definition> definitions) {
        definitions.put(CapitalDialogueKey.COMMANDER_POPULATION_TOO_LOW, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.COMMANDER_LOW_STANDING, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.COMMANDER_ALREADY_GRANTED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.COMMANDER_ALREADY_HELD, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.COMMANDER_REASSIGN_FAILED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.COMMANDER_SUCCESS, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.HAND_LOW_STANDING, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.HAND_LOW_SOVEREIGN_STANDING, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.HAND_ALREADY_GRANTED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.HAND_ALREADY_HELD, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.HAND_SUCCESS, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.LORD_ALREADY_HIGHER, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.LORD_ALREADY_HELD, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.LORD_LOW_STANDING, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.LORD_NOT_ENOUGH_MASTERS, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.LORD_SUCCESS, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.DUKE_ALREADY_HELD, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.DUKE_POPULATION_TOO_LOW, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.DUKE_LOW_STANDING, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.DUKE_SUCCESS, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
    }
}
