package com.example.mcacapitals.dialogue;

import java.util.List;
import java.util.Map;

final class CapitalDialogueLibrarySovereign {

    private CapitalDialogueLibrarySovereign() {
    }

    static void register(Map<CapitalDialogueKey, List<String>> lines) {
        lines.put(
                CapitalDialogueKey.THRONE_NO_SOVEREIGN,
                List.of(
                        "This capital has no reigning sovereign.",
                        "There is no crowned ruler here to answer such a petition.",
                        "No sovereign holds this throne at present.",
                        "There is no reigning sovereign to hear your claim."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_NOT_REIGNING,
                List.of(
                        "That villager is no longer the reigning sovereign.",
                        "You are speaking to the wrong ruler. That one no longer holds the throne.",
                        "That crown no longer belongs to this villager.",
                        "That villager no longer reigns here."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_PLAYER_HELD,
                List.of(
                        "That throne is already held by a player sovereign.",
                        "A player already sits that throne.",
                        "That crown has already passed into player hands.",
                        "This capital is already ruled by a player sovereign."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_POPULATION_TOO_LOW,
                List.of(
                        "I will not surrender the throne until %s stands stronger. Let the capital reach a population of %s first.",
                        "The throne will not change hands by petition until %s has grown to a population of %s.",
                        "This capital is not yet strong enough for such a transfer. Let it reach a population of %s first.",
                        "When %s reaches a population of %s, then such a petition may be heard."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_LOW_STANDING,
                List.of(
                        "You do not yet have the standing to claim the throne of %s.",
                        "The people of %s do not favor your claim strongly enough.",
                        "You have not yet earned the standing needed to rule %s.",
                        "The throne of %s is not given to one without stronger standing."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_ALREADY_RULES_OTHER,
                List.of(
                        "You already rule another capital and cannot petition for this throne.",
                        "One ruler cannot peacefully claim two crowns in this way.",
                        "You already sit another throne. This petition cannot be granted.",
                        "You cannot petition for this crown while ruling another capital."
                )
        );

        lines.put(
                CapitalDialogueKey.THRONE_SUCCESS,
                List.of(
                        "Your petition is accepted. The throne of %s passes peacefully to you.",
                        "So be it. By petition, the throne of %s passes into your hands.",
                        "The matter is settled. You shall rule %s in peace.",
                        "Your claim is accepted. The crown of %s is yours now."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_NO_SOVEREIGN,
                List.of(
                        "You would challenge the throne? Then speak to the one who wears the crown.",
                        "Why are you asking me? If it is the throne you want, ask the king or queen.",
                        "That is not for me to hear. Bring such words before the reigning sovereign.",
                        "If you mean to contest the crown, you must stand before the sovereign."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_MISSING_VILLAGE,
                List.of(
                        "Something is amiss with the records of this capital. I cannot hear this matter now.",
                        "The capital's records are in disarray. This petition cannot be answered.",
                        "There is a problem with the village record. Ask again when the court is in order.",
                        "The records of this capital are not in proper order. I cannot judge this request."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_NOT_IN_AUDIENCE,
                List.of(
                        "If you would speak of taking the throne, stand before the sovereign and say it plainly.",
                        "Do not mutter treason from afar. Stand before the sovereign if you dare.",
                        "If you mean to seize the crown, come before the throne and speak plainly.",
                        "Such words belong in the royal presence, not shouted from a distance."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_LOW_REPUTATION,
                List.of(
                        "You would take the throne? The capital does not love you enough for that.",
                        "The people would never follow you in this. Mind your station.",
                        "You have not earned the standing to make a bid for the crown.",
                        "The capital turns from your claim. You have not won its favor."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_NO_ADVANCEMENT,
                List.of(
                        "Bold words alone do not make a ruler. You have not yet proven your worth.",
                        "You speak of taking the throne, yet you have not shown the might such a deed demands.",
                        "The crown is not won by talk. Prove your strength before you make such a claim.",
                        "A usurper must inspire fear or awe. You have done neither."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_NO_COMMANDER_SUPPORT,
                List.of(
                        "The Commander and the army do not stand with you. Without steel at your back, this ends here.",
                        "You would seize the throne without the army's support? That is folly.",
                        "The Commander has not declared for you, and the guard will not move at your word.",
                        "Without the Commander or the army behind you, your claim dies where it stands."
                )
        );

        lines.put(
                CapitalDialogueKey.SEIZE_THRONE_SUCCESS,
                List.of(
                        "So be it. The throne of %s is yours now.",
                        "Then let it be known: the crown of %s passes to you by force.",
                        "The matter is decided. You have taken the throne of %s.",
                        "By strength and boldness, you have claimed the throne of %s."
                )
        );
    }
}