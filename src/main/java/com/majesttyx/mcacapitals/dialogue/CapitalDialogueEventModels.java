package com.majesttyx.mcacapitals.dialogue;

final class CapitalDialogueEventModels {

    private CapitalDialogueEventModels() {
    }

    enum EventType {
        NONE,
        HEIR_NAMED,
        CAPITAL_FOUNDED,
        ROYAL_MARRIAGE,
        SOVEREIGN_DEATH,
        THRONE_SEIZED,
        DISINHERITED,
        LEGITIMIZED,
        ABDICATION,
        NEW_DUKE_OR_DUCHESS,
        LORD_COMMANDER_APPOINTED,
        HAND_APPOINTED,
        GRAND_MAESTER_APPOINTED,
        ROYAL_GUARD_APPOINTED,
        PEACEFUL_TRANSFER,
        ROYAL_BIRTH,
        COURT_HERALD_APPOINTED,
        MOURNING_ENDED,
        GENERIC_NOTABLE
    }

    record ChronicleEvent(long day, String text, EventType type) {
    }
}