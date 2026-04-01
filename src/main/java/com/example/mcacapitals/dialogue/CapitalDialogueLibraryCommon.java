package com.example.mcacapitals.dialogue;

import java.util.List;
import java.util.Map;

final class CapitalDialogueLibraryCommon {

    private CapitalDialogueLibraryCommon() {
    }

    static void register(Map<CapitalDialogueKey, List<String>> lines) {
        lines.put(
                CapitalDialogueKey.PETITION_SOVEREIGN_ONLY,
                List.of(
                        "Only a reigning sovereign can hear this petition.",
                        "If you would make a petition, bring it before the reigning sovereign.",
                        "Such matters are heard only by the one who sits the throne.",
                        "Take that petition to the reigning sovereign, not to me."
                )
        );

        lines.put(
                CapitalDialogueKey.PETITION_MISSING_VILLAGE,
                List.of(
                        "Something is amiss with the records of this capital.",
                        "The records of this capital are not in proper order.",
                        "There is a fault in the village record. This matter cannot be heard now.",
                        "The capital's records are in disarray. Ask again later."
                )
        );

        lines.put(
                CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED,
                List.of(
                        "You must stand before the sovereign to present this petition.",
                        "If you mean to make your case, then stand before the sovereign and say it plainly.",
                        "Such petitions are heard face to face before the throne.",
                        "Come before the sovereign properly if you wish to be heard."
                )
        );
    }
}