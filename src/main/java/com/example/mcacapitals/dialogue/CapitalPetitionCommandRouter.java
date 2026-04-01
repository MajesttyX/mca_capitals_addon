package com.example.mcacapitals.dialogue;

public final class CapitalPetitionCommandRouter {

    @FunctionalInterface
    public interface PetitionAction {
        void run();
    }

    private CapitalPetitionCommandRouter() {
    }

    public static boolean route(
            String command,
            PetitionAction throneAction,
            PetitionAction seizeThroneAction,
            PetitionAction commanderAction,
            PetitionAction nobleLordAction,
            PetitionAction nobleDukeAction,
            PetitionAction betrothalAction,
            PetitionAction betrothalRecommendAction
    ) {
        if (command == null) {
            return false;
        }

        if (CapitalPetitionService.PETITION_THRONE.equals(command)) {
            throneAction.run();
            return true;
        }

        if (CapitalPetitionService.PETITION_SEIZE_THRONE.equals(command)) {
            seizeThroneAction.run();
            return true;
        }

        if (CapitalPetitionService.PETITION_COMMANDER.equals(command)) {
            commanderAction.run();
            return true;
        }

        if (CapitalPetitionService.PETITION_NOBLE_LORD.equals(command)) {
            nobleLordAction.run();
            return true;
        }

        if (CapitalPetitionService.PETITION_NOBLE_DUKE.equals(command)) {
            nobleDukeAction.run();
            return true;
        }

        if (CapitalPetitionService.PETITION_BETROTHAL.equals(command)) {
            betrothalAction.run();
            return true;
        }

        if (CapitalPetitionService.PETITION_BETROTHAL_RECOMMEND.equals(command)) {
            betrothalRecommendAction.run();
            return true;
        }

        return false;
    }
}