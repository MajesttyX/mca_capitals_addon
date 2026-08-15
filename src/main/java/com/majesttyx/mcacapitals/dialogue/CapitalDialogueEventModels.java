package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventType;
import net.minecraft.network.chat.Component;

final class CapitalDialogueEventModels {

    private CapitalDialogueEventModels() {
    }

    record ChronicleEvent(
            long day,
            Component text,
            CapitalChronicleEventType type,
            boolean semantic
    ) {
    }
}
