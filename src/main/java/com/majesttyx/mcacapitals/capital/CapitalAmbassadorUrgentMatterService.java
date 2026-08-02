package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CapitalAmbassadorUrgentMatterService {

    private CapitalAmbassadorUrgentMatterService() {
    }

    public static boolean openIfPresent(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        if (player == null || ambassadorEntity == null) {
            return false;
        }

        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateMenuAudience(
                        player,
                        ambassadorEntity.getUUID()
                );

        if (!audience.valid()) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = audience.sourceCapital();

        if (!CapitalDiplomaticAuthorityService.mayExerciseSovereignAuthority(
                level,
                capital,
                player.getUUID()
        )) {
            return false;
        }

        List<DiplomaticProposal> proposals =
                findProposals(level, capital);

        List<DiplomaticShipment> shipments =
                CapitalDiplomacyDataAccess
                        .getPendingPlayerShipments(
                                level,
                                capital.getCapitalId()
                        )
                        .stream()
                        .sorted(
                                Comparator.comparingLong(
                                        DiplomaticShipment::getCreatedAt
                                )
                        )
                        .toList();

        boolean asylumRequests =
                CapitalAsylumScreenService.hasReviewableRequests(
                        player,
                        ambassadorEntity.getUUID()
                );

        boolean escortRequests =
                CapitalRoyalBetrothalService.hasOpenEscortRequests(
                        level,
                        capital
                );

        if (proposals.isEmpty()
                && shipments.isEmpty()
                && !asylumRequests
                && !escortRequests) {
            return false;
        }

        MCAIntegrationBridge.stopInteracting(
                ambassadorEntity
        );

        List<OpenAmbassadorCommunicationPacket.Action> actions =
                new ArrayList<>();

        UUID ambassadorId =
                ambassadorEntity.getUUID();

        for (DiplomaticProposal proposal : proposals) {
            CapitalRecord source =
                    CapitalManager.getCapital(
                            proposal.getSourceCapitalId()
                    );

            String sourceName =
                    CapitalDiplomaticAgreementText.capitalName(
                            level,
                            source
                    );

            String typeName =
                    proposal.getType().getDisplayName();

            String detail =
                    sourceName
                            + " has proposed "
                            + CapitalDiplomaticAgreementText
                            .withIndefiniteArticle(typeName)
                            + ".";

            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            "Accept "
                                    + typeName
                                    + " from "
                                    + sourceName,
                            detail,
                            "/capitalurgent proposal "
                                    + ambassadorId
                                    + " "
                                    + proposal.getProposalId()
                                    + " accept",
                            true
                    )
            );

            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            "Reject "
                                    + typeName
                                    + " from "
                                    + sourceName,
                            detail,
                            "/capitalurgent proposal "
                                    + ambassadorId
                                    + " "
                                    + proposal.getProposalId()
                                    + " reject",
                            true
                    )
            );
        }

        int packageNumber = 1;

        for (DiplomaticShipment shipment : shipments) {
            CapitalRecord source =
                    CapitalManager.getCapital(
                            shipment.getSourceCapitalId()
                    );

            String sourceName =
                    CapitalDiplomaticAgreementText.capitalName(
                            level,
                            source
                    );

            String contents =
                    CapitalDiplomaticCorrespondenceService
                            .formatContents(
                                    shipment.getContents()
                            )
                            .replace("\n", ", ");

            String detail =
                    "From "
                            + sourceName
                            + " — "
                            + shipment.getAppraisal()
                            + " — Contents: "
                            + contents;

            String packageLabel =
                    shipments.size() > 1
                            ? "Package "
                            + packageNumber
                            + " from "
                            + sourceName
                            : "Package from "
                            + sourceName;

            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            "Accept " + packageLabel,
                            detail,
                            "/capitalurgent shipment "
                                    + ambassadorId
                                    + " "
                                    + shipment.getShipmentId()
                                    + " accept",
                            true
                    )
            );

            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            "Return " + packageLabel,
                            detail,
                            "/capitalurgent shipment "
                                    + ambassadorId
                                    + " "
                                    + shipment.getShipmentId()
                                    + " return",
                            true
                    )
            );

            packageNumber++;
        }

        if (asylumRequests) {
            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            "Review Asylum Requests",
                            "Refugees inside the capital are awaiting a decision on asylum.",
                            "/capitalasylum review "
                                    + ambassadorId,
                            true
                    )
            );
        }

        if (escortRequests) {
            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            "Review Royal Escorts",
                            "An accepted Royal Betrothal has an escort still in progress.",
                            "/capitalroyalescort review "
                                    + ambassadorId,
                            true
                    )
            );
        }

        String title =
                CapitalTitleResolver.getDisplayTitle(
                        level,
                        capital,
                        player.getUUID()
                );

        String formalName =
                title == null
                        || title.isBlank()
                        || "None".equals(title)
                        || "Commoner".equals(title)
                        ? player.getName().getString()
                        : title
                        + " "
                        + player.getName().getString();

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .DIPLOMACY_ACTIONS,
                        "Urgent Diplomatic Matters",
                        ambassadorEntity
                                .getName()
                                .getString(),
                        formalName
                                + ", there is an urgent matter to attend to. How would you like me to proceed?",
                        "/capitalurgent continue "
                                + ambassadorId,
                        List.of(),
                        actions
                )
        );

        return true;
    }

    public static int continueConversation(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        if (player == null
                || ambassadorId == null) {
            return 0;
        }

        Entity ambassador =
                player.serverLevel()
                        .getEntity(ambassadorId);

        if (ambassador == null
                || !ambassador.isAlive()) {
            player.sendSystemMessage(
                    Component.literal(
                            "The Ambassador is unavailable."
                    )
            );

            return 0;
        }

        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateMenuAudience(
                                player,
                                ambassadorId
                        );

        if (!audience.valid()) {
            String failure =
                    audience.failureMessage();

            player.sendSystemMessage(
                    Component.literal(
                            failure == null
                                    || failure.isBlank()
                                    ? "The Ambassador is unavailable."
                                    : failure
                    )
            );

            return 0;
        }

        MCAIntegrationBridge.stopInteracting(
                ambassador
        );

        ambassador.interact(
                player,
                InteractionHand.MAIN_HAND
        );

        return 1;
    }

    private static List<DiplomaticProposal> findProposals(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null) {
            return List.of();
        }

        return CapitalAgreementDataAccess
                .getProposalsSnapshot(level)
                .values()
                .stream()
                .filter(proposal ->
                        proposal != null
                )
                .filter(proposal ->
                        capital.getCapitalId()
                                .equals(
                                        proposal
                                                .getTargetCapitalId()
                                )
                )
                .filter(proposal ->
                        level.getGameTime()
                                >= proposal.getAvailableAt()
                )
                .filter(
                        DiplomaticProposal
                                ::isAwaitingPlayerResponse
                )
                .sorted(
                        Comparator.comparingLong(
                                DiplomaticProposal::getCreatedAt
                        )
                )
                .toList();
    }
}