package com.majesttyx.mcacapitals.dialogue;

import java.util.Map;

final class CapitalDialogueLibraryBetrothal {

    private CapitalDialogueLibraryBetrothal() {
    }

    static void register(Map<CapitalDialogueKey, CapitalDialogueLibrary.Definition> definitions) {
        definitions.put(CapitalDialogueKey.BETROTHAL_NO_ELIGIBLE_MATCH, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.BETROTHAL_SELECTION_MISSING_VILLAGE, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.BETROTHAL_SELECTION_INVALID_TARGET, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.BETROTHAL_SELECTION_FAILED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.BETROTHAL_SELECTION_SUCCESS, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.BETROTHAL_RECOMMEND_INVALID_TARGET, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.BETROTHAL_RECOMMEND_FAILED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
        definitions.put(CapitalDialogueKey.BETROTHAL_RECOMMEND_SUCCESS, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.RANK_OFFICES));
    }
}
