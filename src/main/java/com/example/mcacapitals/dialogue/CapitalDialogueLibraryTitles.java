package com.example.mcacapitals.dialogue;

import java.util.List;
import java.util.Map;

final class CapitalDialogueLibraryTitles {

    private CapitalDialogueLibraryTitles() {
    }

    static void register(Map<CapitalDialogueKey, List<String>> lines) {
        lines.put(
                CapitalDialogueKey.COMMANDER_POPULATION_TOO_LOW,
                List.of(
                        "The office of Commander of the Royal Army will not be granted until this capital reaches a population of %s.",
                        "This capital must grow to a population of %s before a Commander of the Royal Army may be named.",
                        "The army will not take a Commander until the capital reaches a population of %s.",
                        "When the capital reaches a population of %s, then this office may be granted."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_LOW_STANDING,
                List.of(
                        "You do not yet have the standing to be entrusted with command in %s.",
                        "The people of %s do not yet trust you with command.",
                        "You have not yet earned the standing required to lead the royal army of %s.",
                        "Command in %s is not given without stronger standing."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_ALREADY_GRANTED,
                List.of(
                        "The office of Commander of the Royal Army has already been granted and cannot be taken from another sworn player.",
                        "That office is already held by another sworn commander.",
                        "Another player has already been entrusted with command of the royal army.",
                        "The office has already been granted elsewhere and cannot be stripped by petition."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_ALREADY_HELD,
                List.of(
                        "You already hold the office of Commander of the Royal Army.",
                        "You are already my Commander of the Royal Army.",
                        "That office is already yours.",
                        "You already command the royal army."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_REASSIGN_FAILED,
                List.of(
                        "The office of Commander of the Royal Army could not be reassigned.",
                        "Something prevented the command from being granted.",
                        "This appointment could not be completed.",
                        "The office could not be transferred into your hands."
                )
        );

        lines.put(
                CapitalDialogueKey.COMMANDER_SUCCESS,
                List.of(
                        "Your petition is accepted. You are now Commander of the Royal Army of %s.",
                        "So be it. You shall command the royal army of %s.",
                        "The office is yours. You are now Commander of the Royal Army of %s.",
                        "I grant it. You are now Commander of the Royal Army of %s."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_ALREADY_HIGHER,
                List.of(
                        "You already hold a higher noble dignity than Lord or Lady.",
                        "You already stand above the lesser nobility.",
                        "Why would I lower your rank? You already hold a higher dignity.",
                        "You already possess a greater noble title than Lord or Lady."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_ALREADY_HELD,
                List.of(
                        "You already hold the dignity of Lord or Lady.",
                        "You are already one of my lesser nobles.",
                        "That title is already yours.",
                        "You already stand as Lord or Lady of this capital."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_LOW_STANDING,
                List.of(
                        "You have not yet earned the standing required to be raised to the lesser nobility of %s.",
                        "The lesser nobility of %s is not granted without stronger standing.",
                        "You have not yet earned enough favor to be named among the lesser nobles of %s.",
                        "Your standing in %s is not yet high enough for such a title."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_NOT_ENOUGH_MASTERS,
                List.of(
                        "I will not grant this petition until at least %s master villagers strengthen the standing of the capital.",
                        "This capital must have at least %s master villagers before I grant such a title.",
                        "When at least %s master villagers serve this capital, then this petition may be heard.",
                        "The capital is not yet ready. It must first have at least %s master villagers."
                )
        );

        lines.put(
                CapitalDialogueKey.LORD_SUCCESS,
                List.of(
                        "Your petition is accepted. You are now %s of %s.",
                        "So be it. You are now %s of %s.",
                        "I grant it. You shall be known as %s of %s.",
                        "The matter is settled. You are now %s of %s."
                )
        );

        lines.put(
                CapitalDialogueKey.DUKE_ALREADY_HELD,
                List.of(
                        "You already hold the dignity of Duke or Duchess.",
                        "You already stand among my highest nobles.",
                        "That ducal dignity is already yours.",
                        "You already hold ducal rank."
                )
        );

        lines.put(
                CapitalDialogueKey.DUKE_POPULATION_TOO_LOW,
                List.of(
                        "I will not grant ducal rank until this capital reaches a population of %s.",
                        "This capital must grow to a population of %s before ducal rank may be granted.",
                        "Ducal rank will be granted only when the capital reaches a population of %s.",
                        "When this capital reaches a population of %s, then such a title may be granted."
                )
        );

        lines.put(
                CapitalDialogueKey.DUKE_LOW_STANDING,
                List.of(
                        "You have not yet earned the standing required to be raised to ducal rank in %s.",
                        "Ducal rank in %s is not granted without stronger standing.",
                        "Your standing in %s is not yet high enough for ducal rank.",
                        "You have not yet earned enough favor to be raised as a duke or duchess of %s."
                )
        );

        lines.put(
                CapitalDialogueKey.DUKE_SUCCESS,
                List.of(
                        "Your petition is accepted. You are now %s of %s.",
                        "So be it. You are now %s of %s.",
                        "I grant it. You shall be known as %s of %s.",
                        "The matter is settled. You are now %s of %s."
                )
        );
    }
}