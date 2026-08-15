package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenBetrothalSelectionPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.MCARelationshipBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;

final class CapitalPetitionBetrothalActions {

    private CapitalPetitionBetrothalActions() {
    }

    static boolean handleBetrothalSelection(ServerPlayer player, UUID capitalId, UUID targetId) {
        if (player == null || capitalId == null || targetId == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_petition_betrothal_actions.that_capital_can_no_longer_hear_this_petition"));
            return false;
        }

        if (capital.getVillageId() == null) {
            CapitalPetitionDialogueHelper.sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_MISSING_VILLAGE);
            return false;
        }

        if (!CapitalPetitionRequirements.isPlayerBetrothalCandidate(level, capital, targetId)) {
            CapitalPetitionDialogueHelper.sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_INVALID_TARGET);
            return false;
        }

        MCARelationshipBridge.BetrothalResult result =
                MCARelationshipBridge.promise(player, MCAIntegrationBridge.getEntityByUuid(level, targetId));

        if (!result.success()) {
            CapitalPetitionDialogueHelper.sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_FAILED, result.message());
            return false;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        Component targetName = CapitalPetitionRequirements.buildBetrothalCandidateNameComponent(level, capital, targetId);

        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.BETROTHAL_PLAYER_PETITION, CapitalChronicleIdentitySnapshot.name(level, capital, targetId), player.getName().getString(), villageName);

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        CapitalPetitionDialogueHelper.sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_SUCCESS, targetName);
        return true;
    }

    static boolean handleRecommendedBetrothalSelection(ServerPlayer player, UUID capitalId, UUID firstId, UUID secondId) {
        if (player == null || capitalId == null || firstId == null || secondId == null) {
            return false;
        }

        if (firstId.equals(secondId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_petition_betrothal_actions.you_must_choose_two_different_villagers_for_a_betrothal_recommendation"));
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_petition_betrothal_actions.that_capital_can_no_longer_hear_this_petition"));
            return false;
        }

        if (capital.getVillageId() == null) {
            CapitalPetitionDialogueHelper.sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_SELECTION_MISSING_VILLAGE);
            return false;
        }

        if (!CapitalPetitionRequirements.isRecommendedBetrothalCandidate(level, capital, firstId)
                || !CapitalPetitionRequirements.isRecommendedBetrothalCandidate(level, capital, secondId)) {
            CapitalPetitionDialogueHelper.sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_RECOMMEND_INVALID_TARGET);
            return false;
        }

        Entity firstVillager = MCAIntegrationBridge.getEntityByUuid(level, firstId);
        Entity secondVillager = MCAIntegrationBridge.getEntityByUuid(level, secondId);

        MCARelationshipBridge.BetrothalResult result =
                MCARelationshipBridge.promiseVillagerToVillager(firstVillager, secondVillager);

        if (!result.success()) {
            CapitalPetitionDialogueHelper.sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_RECOMMEND_FAILED, result.message());
            return false;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        Component firstName = CapitalPetitionRequirements.buildBetrothalCandidateNameComponent(level, capital, firstId);
        Component secondName = CapitalPetitionRequirements.buildBetrothalCandidateNameComponent(level, capital, secondId);

        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.BETROTHAL_ROYAL_RECOMMENDATION, CapitalChronicleIdentitySnapshot.name(level, capital, firstId), CapitalChronicleIdentitySnapshot.name(level, capital, secondId), villageName);

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        CapitalPetitionDialogueHelper.sendCapitalDialogue(player, level, capital, CapitalDialogueKey.BETROTHAL_RECOMMEND_SUCCESS, firstName, secondName);
        return true;
    }

    static void handleBetrothalPetition(
            ServerPlayer player,
            Entity villagerEntity,
            double maxAudienceDistanceSqr
    ) {
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalPetitionRequirements.resolveSovereignCapital(level, villagerEntity);

        if (capital == null) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_SOVEREIGN_ONLY);
            return;
        }

        if (!CapitalPetitionRequirements.isAudienceValid(player, villagerEntity, maxAudienceDistanceSqr)) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED);
            return;
        }

        List<OpenBetrothalSelectionPacket.Candidate> playerCandidates =
                CapitalPetitionRequirements.collectPlayerBetrothalCandidates(level, capital);
        List<OpenBetrothalSelectionPacket.Candidate> recommendationCandidates =
                CapitalPetitionRequirements.collectRecommendedBetrothalCandidates(level, capital);

        MCACapitals.LOGGER.info(
                "[MCACapitals] Betrothal petition candidates found. playerCandidates={}, recommendationCandidates={}",
                playerCandidates.size(),
                recommendationCandidates.size()
        );

        if (playerCandidates.isEmpty() && recommendationCandidates.size() < 2) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(player, villagerEntity, CapitalDialogueKey.BETROTHAL_NO_ELIGIBLE_MATCH);
            return;
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenBetrothalSelectionPacket(
                        capital.getCapitalId(),
                        MCAIntegrationBridge.getVillageName(level, capital.getVillageId()),
                        playerCandidates,
                        recommendationCandidates
                )
        );

        MCACapitals.LOGGER.info("[MCACapitals] Sent betrothal selection packet to player={}", player.getName().getString());
    }
}