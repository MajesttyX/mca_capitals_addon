package com.majesttyx.mcacapitals.dialogue;

import java.util.Map;

final class CapitalDialogueLibraryCommon {

    private CapitalDialogueLibraryCommon() {
    }

    static void register(Map<CapitalDialogueKey, CapitalDialogueLibrary.Definition> definitions) {
        definitions.put(CapitalDialogueKey.PETITION_SOVEREIGN_ONLY, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.PETITION_MISSING_VILLAGE, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
    }
}
