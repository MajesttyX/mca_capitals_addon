package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import fabric.net.mca.entity.VillagerEntityMCA;
import fabric.net.mca.cobalt.network.NetworkHandler;
import fabric.net.mca.network.s2c.InteractionDialogueResponse;
import fabric.net.mca.resources.Dialogues;
import fabric.net.mca.resources.data.dialogue.Answer;
import fabric.net.mca.resources.data.dialogue.Question;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class CapitalSovereignDeclarationPromptService {
    public static final String ACCEPT_COMMAND = "mcacapitals_declare_capital_accept";
    public static final String DECLINE_COMMAND = "mcacapitals_declare_capital_decline";
    public static final String DECLARE_COMMAND = "mcacapitals_declare_for_capital";
    public static final String QUESTION_ID = "mcacapitals_declaration_prompt";
    private static final String QUESTION_TEXT_KEY = "dialogue.mcacapitals_declaration_prompt.question";

    private CapitalSovereignDeclarationPromptService() {
    }

    public static boolean shouldPrompt(ServerPlayer player, VillagerEntityMCA villager) {
        if (player == null || villager == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        PlayerCapitalAllegianceService.synchronizePlayerSovereignDeclaration(player);
        CapitalRecord capital = resolveCapital(level, villager);

        return capital != null
                && capital.getState() == CapitalState.ACTIVE
                && villager.getUUID().equals(capital.getSovereign())
                && PlayerCapitalAllegianceService.getDeclaredCapitalId(
                level,
                player.getUUID()
        ) == null;
    }

    public static void openPrompt(ServerPlayer player, VillagerEntityMCA villager) {
        CapitalRecord capital = resolveCapital(player.serverLevel(), villager);
        if (capital == null) {
            return;
        }

        Question question = Dialogues.getInstance().getQuestion(QUESTION_ID);
        if (question == null
                || question.getAnswer(ACCEPT_COMMAND) == null
                || question.getAnswer(DECLINE_COMMAND) == null) {
            MCACapitals.LOGGER.error(
                    "[MCACapitals] Declaration dialogue '{}' is missing or incomplete; normal MCA dialogue remains available.",
                    QUESTION_ID
            );
            return;
        }

        player.sendSystemMessage(
                Component.translatable(
                        QUESTION_TEXT_KEY,
                        CapitalDiplomaticAgreementText.capitalName(
                                player.serverLevel(),
                                capital
                        )
                )
        );

        // MCA 7.6.x only exposes the Question-based response constructor.
        // Clone the two declaration answers without constraints so the 1.21.1
        // explicit-answer behavior is preserved and MCA cannot filter the panel empty.
        Answer accept = question.getAnswer(ACCEPT_COMMAND);
        Answer decline = question.getAnswer(DECLINE_COMMAND);
        Question declarationQuestion = new Question(
                question.getName(),
                List.of(
                        new Answer(accept.getName(), List.of(), accept.getResults(), accept.getPriority()),
                        new Answer(decline.getName(), List.of(), decline.getResults(), decline.getPriority())
                ),
                false,
                question.isSilent()
        );
        NetworkHandler.sendToPlayer(
                new InteractionDialogueResponse(declarationQuestion, player, villager),
                player
        );
    }

    public static boolean handleCommand(ServerPlayer player, Entity entity, String command) {
        if (player == null || !(entity instanceof VillagerEntityMCA villager) || command == null) {
            return false;
        }

        CapitalRecord capital = resolveCapital(player.serverLevel(), villager);
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            return false;
        }

        if (ACCEPT_COMMAND.equals(command)) {
            if (!villager.getUUID().equals(capital.getSovereign())) {
                return false;
            }

            PlayerCapitalAllegianceService.DeclarationResult result =
                    PlayerCapitalAllegianceService.declare(player, capital);
            player.sendSystemMessage(result.message());
            continueNormalConversation(player, villager);
            return true;
        }

        if (DECLINE_COMMAND.equals(command)) {
            if (!villager.getUUID().equals(capital.getSovereign())) {
                return false;
            }

            continueNormalConversation(player, villager);
            return true;
        }

        if (DECLARE_COMMAND.equals(command)) {
            if (!villager.getUUID().equals(capital.getSovereign())
                    && !villager.getUUID().equals(capital.getHand())) {
                return false;
            }

            PlayerCapitalAllegianceService.DeclarationResult result =
                    PlayerCapitalAllegianceService.declare(player, capital);
            player.sendSystemMessage(result.message());
            MCAIntegrationBridge.stopInteracting(villager);
            return true;
        }

        return false;
    }

    public static boolean canShowDeclarationAnswer(ServerPlayer player, VillagerEntityMCA villager) {
        CapitalRecord capital = resolveCapital(
                player == null ? null : player.serverLevel(),
                villager
        );

        return player != null
                && villager != null
                && capital != null
                && (villager.getUUID().equals(capital.getSovereign())
                || villager.getUUID().equals(capital.getHand()))
                && PlayerCapitalAllegianceService.canOfferDeclaration(player, capital);
    }

    public static CapitalRecord resolveCapital(ServerLevel level, Entity villager) {
        if (level == null || villager == null) {
            return null;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && capital.getState() == CapitalState.ACTIVE
                    && (villager.getUUID().equals(capital.getSovereign())
                    || villager.getUUID().equals(capital.getHand())
                    || villager.getUUID().equals(capital.getMasterOfLaws()))) {
                return capital;
            }
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(
                level,
                villager.getUUID()
        );
        CapitalRecord capital = CapitalManager.getCapitalByVillageId(villageId);
        return capital != null && capital.getState() == CapitalState.ACTIVE
                ? capital
                : null;
    }

    public static void continueNormalConversation(ServerPlayer player, VillagerEntityMCA villager) {
        Question root = Dialogues.getInstance().getQuestion("root");
        if (root == null) {
            MCACapitals.LOGGER.error(
                    "[MCACapitals] MCA root dialogue is unavailable while continuing after declaration prompt."
            );
            return;
        }

        if (root.isAuto()) {
            Dialogues.getInstance().selectAnswer(
                    villager,
                    player,
                    root.getName(),
                    root.getRandomAnswer().getName()
            );
            return;
        }

        NetworkHandler.sendToPlayer(
                new InteractionDialogueResponse(root, player, villager),
                player
        );
    }
}
