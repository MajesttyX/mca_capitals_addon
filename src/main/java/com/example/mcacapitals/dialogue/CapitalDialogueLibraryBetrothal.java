package com.example.mcacapitals.dialogue;

import java.util.List;
import java.util.Map;

final class CapitalDialogueLibraryBetrothal {

    private CapitalDialogueLibraryBetrothal() {
    }

    static void register(Map<CapitalDialogueKey, List<String>> lines) {
        lines.put(
                CapitalDialogueKey.BETROTHAL_NO_ELIGIBLE_MATCH,
                List.of(
                        "There is no eligible match in this capital who may presently be named in a betrothal petition.",
                        "No suitable match in this capital may presently be named in such a petition.",
                        "There is no one here who may rightly be named in a betrothal petition at this time.",
                        "No proper match is available in this capital for such a petition."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_SELECTION_MISSING_VILLAGE,
                List.of(
                        "Something is amiss with the records of this capital.",
                        "The capital's records are in disarray.",
                        "There is a fault in the village record. This matter cannot be decided now.",
                        "The records of this capital are not in proper order."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_SELECTION_INVALID_TARGET,
                List.of(
                        "Only an eligible teen or adult noble of this capital may be chosen for betrothal.",
                        "You may only name an eligible teen or adult noble of this capital in such a petition.",
                        "That choice is not valid. Only an eligible teen or adult noble of this capital may be chosen.",
                        "Only a proper noble of age within this capital may be named in this betrothal."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_SELECTION_FAILED,
                List.of(
                        "This betrothal cannot be granted: %s",
                        "That petition fails: %s",
                        "I cannot grant that match: %s",
                        "This matter cannot proceed: %s"
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_SELECTION_SUCCESS,
                List.of(
                        "Your petition is accepted. %s is now betrothed to you.",
                        "So be it. %s is now promised to you.",
                        "The matter is settled. %s is now betrothed to you.",
                        "I grant it. %s is now pledged to you."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_RECOMMEND_INVALID_TARGET,
                List.of(
                        "Only residents of this capital may be named in a betrothal recommendation.",
                        "You may only name residents of this capital in such a recommendation.",
                        "That recommendation is not valid. Only residents of this capital may be named.",
                        "Only those who belong to this capital may be named in this matter."
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_RECOMMEND_FAILED,
                List.of(
                        "This recommendation cannot be granted: %s",
                        "That recommendation fails: %s",
                        "I cannot approve that match: %s",
                        "This matter cannot proceed: %s"
                )
        );

        lines.put(
                CapitalDialogueKey.BETROTHAL_RECOMMEND_SUCCESS,
                List.of(
                        "Your recommendation is accepted. %s and %s are now betrothed.",
                        "So be it. %s and %s are now promised to one another.",
                        "The matter is settled. %s and %s are now betrothed.",
                        "I grant it. %s and %s are now pledged to one another."
                )
        );
    }
}