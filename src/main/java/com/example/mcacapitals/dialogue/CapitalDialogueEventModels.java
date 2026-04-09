package com.example.mcacapitals.dialogue;

final class CapitalDialogueEventModels {

    private CapitalDialogueEventModels() {
    }

    enum EventType {
        NONE,
        MARRIAGE,
        DEATH_OR_SUCCESSION,
        MOURNING_DECLARED,
        MOURNING_ENDED,
        HEIR_NAMED,
        DISINHERITED,
        LEGITIMIZED,
        THRONE_CHANGE,
        CAPITAL_FOUNDED,
        GENERIC_NOTABLE
    }

    record ChronicleEvent(long day, String text, EventType type) {
    }
}