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

            Component typeName =
                    proposal.getType().getDisplayComponent();

            Component detail =
                    Component.translatable(
                            "mcacapitals.ui.urgent_diplomacy.proposal_detail",
                            sourceName,
                            proposal.getType().getIndefiniteComponent()
                    );

            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.accept_proposal",
                                    typeName,
                                    sourceName
                            ),
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
                            Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.reject_proposal",
                                    typeName,
                                    sourceName
                            ),
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

            Component contents =
                    CapitalDiplomaticCorrespondenceService
                            .formatContentsInline(
                                    shipment.getContents()
                            );

            Component detail =
                    Component.translatable(
                            "mcacapitals.ui.urgent_diplomacy.package_detail",
                            sourceName,
                            CapitalGiftAppraisalService.appraisalComponent(
                                    shipment.getAppraisal()
                            ),
                            contents
                    );

            Component packageLabel =
                    shipments.size() > 1
                            ? Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.numbered_package_from",
                                    packageNumber,
                                    sourceName
                            )
                            : Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.package_from",
                                    sourceName
                            );

            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.accept_package",
                                    packageLabel
                            ),
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
                            Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.return_package",
                                    packageLabel
                            ),
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
                            Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.review_asylum"
                            ),
                            Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.asylum_detail"
                            ),
                            "/capitalasylum review "
                                    + ambassadorId,
                            true
                    )
            );
        }

        if (escortRequests) {
            actions.add(
                    new OpenAmbassadorCommunicationPacket.Action(
                            Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.review_escorts"
                            ),
                            Component.translatable(
                                    "mcacapitals.ui.urgent_diplomacy.escort_detail"
                            ),
                            "/capitalroyalescort review "
                                    + ambassadorId,
                            true
                    )
            );
        }

        CapitalTitleResolver.ResolvedTitleId titleId =
                CapitalTitleResolver.getResolvedTitleId(
                        level,
                        capital,
                        player.getUUID()
                );

        Component formalName =
                titleId == CapitalTitleResolver.ResolvedTitleId.NONE
                        || titleId == CapitalTitleResolver.ResolvedTitleId.COMMONER
                        ? player.getName()
                        : Component.translatable(
                                "mcacapitals.ui.urgent_diplomacy.titled_name",
                                CapitalTitleResolver.getDisplayTitleComponent(
                                        level,
                                        capital,
                                        player.getUUID()
                                ),
                                player.getName()
                        );

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .DIPLOMACY_ACTIONS,
                        Component.translatable(
                                "mcacapitals.ui.urgent_diplomacy.title"
                        ),
                        ambassadorEntity.getName(),
                        Component.translatable(
                                "mcacapitals.ui.urgent_diplomacy.message",
                                formalName
                        ),
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
                    Component.translatable("mcacapitals.system.capital_ambassador_urgent_matter_service.the_ambassador_is_unavailable")
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
            Component failure =
                    audience.failureMessage();

            player.sendSystemMessage(
                    failure == null
                            ? Component.translatable(
                                    "mcacapitals.system.capital_ambassador_urgent_matter_service.the_ambassador_is_unavailable"
                            )
                            : failure
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