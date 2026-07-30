package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.network.Network;
import net.conczin.mca.network.s2c.InteractionDialogueResponse;
import net.conczin.mca.resources.Dialogues;
import net.conczin.mca.resources.data.dialogue.Question;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class CapitalSovereignDeclarationPromptService {

    public static final String ACCEPT_COMMAND = "mcacapitals_declare_capital_accept";
    public static final String DECLINE_COMMAND = "mcacapitals_declare_capital_decline";
    public static final String DECLARE_COMMAND = "mcacapitals_declare_for_capital";
    public static final String QUESTION_ID = "mcacapitals_declaration_prompt";

    private CapitalSovereignDeclarationPromptService() {
    }

    public static boolean shouldPrompt(
            ServerPlayer player,
            VillagerEntityMCA villager
    ) {
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

    public static void openPrompt(
            ServerPlayer player,
            VillagerEntityMCA villager
    ) {
        CapitalRecord capital = resolveCapital(player.serverLevel(), villager);
        if (capital == null) {
            continueNormalConversation(player, villager);
            return;
        }

        player.sendSystemMessage(Component.literal(
                villager.getName().getString()
                        + ": Before we continue, will you declare for "
                        + CapitalDiplomaticAgreementText.capitalName(
                        player.serverLevel(),
                        capital
                )
                        + "?"
        ));

        Question question = Dialogues.getInstance().getQuestion(QUESTION_ID);
        Network.sendToPlayer(
                new InteractionDialogueResponse(question, player, villager),
                player
        );
    }

    public static boolean handleCommand(
            ServerPlayer player,
            Entity entity,
            String command
    ) {
        if (player == null
                || !(entity instanceof VillagerEntityMCA villager)
                || command == null) {
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
            player.sendSystemMessage(Component.literal(result.message()));
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
            player.sendSystemMessage(Component.literal(result.message()));
            MCAIntegrationBridge.stopInteracting(villager);
            return true;
        }

        return false;
    }

    public static boolean canShowDeclarationAnswer(
            ServerPlayer player,
            VillagerEntityMCA villager
    ) {
        CapitalRecord capital = resolveCapital(
                player == null ? null : player.serverLevel(),
                villager
        );
        return player != null
                && villager != null
                && capital != null
                && (villager.getUUID().equals(capital.getSovereign())
                || villager.getUUID().equals(capital.getHand()))
                && PlayerCapitalAllegianceService.canOfferDeclaration(
                player,
                capital
        );
    }

    public static CapitalRecord resolveCapital(
            ServerLevel level,
            Entity villager
    ) {
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

    public static void continueNormalConversation(
            ServerPlayer player,
            VillagerEntityMCA villager
    ) {
        Question root = Dialogues.getInstance().getQuestion("root");
        if (root.isAuto()) {
            Dialogues.getInstance().selectAnswer(
                    villager,
                    player,
                    root.getName(),
                    root.getRandomAnswer().getName()
            );
            return;
        }
        Network.sendToPlayer(
                new InteractionDialogueResponse(root, player, villager),
                player
        );
    }
}