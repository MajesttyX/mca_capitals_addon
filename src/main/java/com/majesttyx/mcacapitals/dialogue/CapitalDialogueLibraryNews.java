package com.majesttyx.mcacapitals.dialogue;

import java.util.Map;

final class CapitalDialogueLibraryNews {

    private CapitalDialogueLibraryNews() {
    }

    static void register(Map<CapitalDialogueKey, CapitalDialogueLibrary.Definition> definitions) {
        definitions.put(CapitalDialogueKey.NEWS_MARRIAGE, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
        definitions.put(CapitalDialogueKey.NEWS_DEATH_OR_SUCCESSION, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
        definitions.put(CapitalDialogueKey.NEWS_MOURNING_DECLARED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
        definitions.put(CapitalDialogueKey.NEWS_MOURNING_ENDED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
        definitions.put(CapitalDialogueKey.NEWS_HEIR_NAMED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
        definitions.put(CapitalDialogueKey.NEWS_DISINHERITED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
        definitions.put(CapitalDialogueKey.NEWS_LEGITIMIZED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
        definitions.put(CapitalDialogueKey.NEWS_THRONE_CHANGE, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
        definitions.put(CapitalDialogueKey.NEWS_CAPITAL_FOUNDED, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
        definitions.put(CapitalDialogueKey.NEWS_GENERIC_NOTABLE, CapitalDialogueLibrary.definition(CapitalDialogueLibrary.Category.CHAT_CHATTER));
    }
}
