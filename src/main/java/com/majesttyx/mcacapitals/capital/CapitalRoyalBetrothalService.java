package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalAccess;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalSavedData;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import fabric.net.mca.entity.VillagerEntityMCA;
import fabric.net.mca.server.world.data.Village;
import fabric.net.mca.server.world.data.VillageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CapitalRoyalBetrothalService {

    private static final int MINIMUM_RELATIONSHIP = 20;
    private static final int MARRIAGE_RELATIONSHIP_BONUS = 25;
    private static final double MAX_AMBASSADOR_DISTANCE_SQR = 144.0D;

    private CapitalRoyalBetrothalService() {
    }

    static Component validateProposal(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            CapitalDiplomaticState state,
            int score
    ) {
        if (level == null || source == null || target == null) {
            return Component.translatable("mcacapitals.diplomacy.royal_betrothal.validation.invalid");
        }
        if (state == CapitalDiplomaticState.WAR || state == CapitalDiplomaticState.TRUCE) {
            return Component.translatable("mcacapitals.diplomacy.royal_betrothal.validation.war_or_truce");
        }
        if (score < MINIMUM_RELATIONSHIP) {
            return Component.translatable("mcacapitals.diplomacy.royal_betrothal.validation.cordial_required");
        }
        return findMatch(level, source, target) == null
                ? Component.translatable("mcacapitals.diplomacy.royal_betrothal.validation.no_match")
                : null;
    }

    static Match findMatch(ServerLevel level, CapitalRecord source, CapitalRecord target) {
        if (level == null || source == null || target == null
                || source.getCapitalId() == null || target.getCapitalId() == null) {
            return null;
        }
        for (UUID sourceRoyalId : eligibleRoyals(level, source)) {
            for (UUID targetRoyalId : eligibleRoyals(level, target)) {
                if (isCrownHeir(source, sourceRoyalId) && isCrownHeir(target, targetRoyalId)) {
                    continue;
                }
                UUID forcedDestination = forcedDestination(source, target, sourceRoyalId, targetRoyalId);
                UUID destination = forcedDestination == null
                        ? target.getCapitalId()
                        : forcedDestination;
                Match match = createMatch(
                        level,
                        source,
                        target,
                        sourceRoyalId,
                        targetRoyalId,
                        destination
                );
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    public static int openSourceRoyalSelection(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        SelectionContext context = validateSelectionContext(
                player,
                ambassadorId,
                targetCapitalId
        );

        if (!context.valid()) {
            sendFailure(player, context.failureMessage());
            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (UUID royalId :
                eligibleRoyals(
                        context.level(),
                        context.source()
                )) {
            Component name = royalNameComponent(
                    context.level(),
                    context.source(),
                    royalId
            );

            Component title = royalTitleComponent(
                    context.level(),
                    context.source(),
                    royalId
            );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            Component.translatable(
                                    "mcacapitals.ui.royal_betrothal.titled_name",
                                    title,
                                    name
                            ),
                            isCrownHeir(
                                    context.source(),
                                    royalId
                            )
                                    ? Component.translatable(
                                            "mcacapitals.ui.royal_betrothal.crown_heir_must_remain",
                                            CapitalDiplomaticAgreementText.capitalNameComponent(
                                                    context.level(),
                                                    context.source()
                                            )
                                    )
                                    : Component.translatable(
                                            "mcacapitals.ui.royal_betrothal.eligible_own_royal"
                                    ),
                            Component.empty(),
                            Component.empty(),
                            Component.translatable(
                                    "mcacapitals.ui.royal_betrothal.choose",
                                    name
                            ),
                            "/capitaldiplomacy betrothal_target "
                                    + ambassadorId
                                    + " "
                                    + targetCapitalId
                                    + " "
                                    + royalId,
                            true,
                            Component.empty()
                    )
            );
        }

        openSelectionScreen(
                player,
                context.ambassador(),
                Component.translatable(
                        "mcacapitals.ui.royal_betrothal.choose_your_royal"
                ),
                entries,
                entries.isEmpty()
                        ? Component.translatable(
                                "mcacapitals.ui.royal_betrothal.no_eligible_own_royal"
                        )
                        : Component.translatable(
                                "mcacapitals.ui.royal_betrothal.choose_own_royal_description"
                        ),
                "/capitaldiplomacy options "
                        + ambassadorId
                        + " "
                        + targetCapitalId
        );

        return 1;
    }

    public static int openTargetRoyalSelection(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            UUID sourceRoyalId
    ) {
        SelectionContext context =
                validateSelectionContext(
                        player,
                        ambassadorId,
                        targetCapitalId
                );

        if (!context.valid()) {
            sendFailure(
                    player,
                    context.failureMessage()
            );

            return 0;
        }

        if (!isEligibleRoyal(
                context.level(),
                context.source(),
                sourceRoyalId
        )) {
            sendFailure(
                    player,
                    Component.translatable(
                            "mcacapitals.ui.royal_betrothal.no_longer_eligible"
                    )
            );

            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (UUID targetRoyalId :
                eligibleRoyals(
                        context.level(),
                        context.target()
                )) {
            if (isCrownHeir(
                    context.source(),
                    sourceRoyalId
            )
                    && isCrownHeir(
                    context.target(),
                    targetRoyalId
            )) {
                continue;
            }

            if (MCAIntegrationBridge
                    .areCloselyRelatedForMarriage(
                            context.level(),
                            sourceRoyalId,
                            targetRoyalId
                    )) {
                continue;
            }

            Component name = royalNameComponent(
                    context.level(),
                    context.target(),
                    targetRoyalId
            );

            Component title = royalTitleComponent(
                    context.level(),
                    context.target(),
                    targetRoyalId
            );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            Component.translatable(
                                    "mcacapitals.ui.royal_betrothal.titled_name",
                                    title,
                                    name
                            ),
                            isCrownHeir(
                                    context.target(),
                                    targetRoyalId
                            )
                                    ? Component.translatable(
                                            "mcacapitals.ui.royal_betrothal.crown_heir_must_remain",
                                            CapitalDiplomaticAgreementText.capitalNameComponent(
                                                    context.level(),
                                                    context.target()
                                            )
                                    )
                                    : Component.translatable(
                                            "mcacapitals.ui.royal_betrothal.eligible_foreign_royal"
                                    ),
                            Component.empty(),
                            Component.empty(),
                            Component.translatable(
                                    "mcacapitals.ui.royal_betrothal.choose",
                                    name
                            ),
                            "/capitaldiplomacy betrothal_settlement "
                                    + ambassadorId
                                    + " "
                                    + targetCapitalId
                                    + " "
                                    + sourceRoyalId
                                    + " "
                                    + targetRoyalId,
                            true,
                            Component.empty()
                    )
            );
        }

        openSelectionScreen(
                player,
                context.ambassador(),
                Component.translatable(
                        "mcacapitals.ui.royal_betrothal.choose_their_royal"
                ),
                entries,
                entries.isEmpty()
                        ? Component.translatable(
                                "mcacapitals.ui.royal_betrothal.no_compatible_foreign_royal"
                        )
                        : Component.translatable(
                                "mcacapitals.ui.royal_betrothal.choose_match_for",
                                royalNameComponent(
                                        context.level(),
                                        context.source(),
                                        sourceRoyalId
                                )
                        ),
                "/capitaldiplomacy betrothal_source "
                        + ambassadorId
                        + " "
                        + targetCapitalId
        );

        return 1;
    }

    public static int openSettlementSelection(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            UUID sourceRoyalId,
            UUID targetRoyalId
    ) {
        SelectionContext context =
                validateSelectionContext(
                        player,
                        ambassadorId,
                        targetCapitalId
                );

        if (!context.valid()) {
            sendFailure(
                    player,
                    context.failureMessage()
            );

            return 0;
        }

        if (!isValidPair(
                context.level(),
                context.source(),
                context.target(),
                sourceRoyalId,
                targetRoyalId
        )) {
            sendFailure(
                    player,
                    Component.translatable(
                            "mcacapitals.ui.royal_betrothal.pair_no_longer_eligible"
                    )
            );

            return 0;
        }

        UUID forcedDestination =
                forcedDestination(
                        context.source(),
                        context.target(),
                        sourceRoyalId,
                        targetRoyalId
                );

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        if (forcedDestination != null) {
            CapitalRecord destination =
                    CapitalManager.getCapital(
                            forcedDestination
                    );

            Component destinationName =
                    CapitalDiplomaticAgreementText.capitalNameComponent(
                            context.level(),
                            destination
                    );

            entries.add(
                    settlementEntry(
                            ambassadorId,
                            targetCapitalId,
                            sourceRoyalId,
                            targetRoyalId,
                            forcedDestination,
                            destinationName,
                            Component.translatable(
                                    "mcacapitals.ui.royal_betrothal.crown_heir_settlement_explanation"
                            )
                    )
            );
        } else {
            entries.add(
                    settlementEntry(
                            ambassadorId,
                            targetCapitalId,
                            sourceRoyalId,
                            targetRoyalId,
                            context.source()
                                    .getCapitalId(),
                            CapitalDiplomaticAgreementText.capitalNameComponent(
                                    context.level(),
                                    context.source()
                            ),
                            Component.translatable(
                                    "mcacapitals.ui.royal_betrothal.escort_here_after_acceptance",
                                    royalNameComponent(
                                            context.level(),
                                            context.target(),
                                            targetRoyalId
                                    )
                            )
                    )
            );

            entries.add(
                    settlementEntry(
                            ambassadorId,
                            targetCapitalId,
                            sourceRoyalId,
                            targetRoyalId,
                            context.target()
                                    .getCapitalId(),
                            CapitalDiplomaticAgreementText.capitalNameComponent(
                                    context.level(),
                                    context.target()
                            ),
                            Component.translatable(
                                    "mcacapitals.ui.royal_betrothal.escort_there_after_acceptance",
                                    royalNameComponent(
                                            context.level(),
                                            context.source(),
                                            sourceRoyalId
                                    )
                            )
                    )
            );
        }

        openSelectionScreen(
                player,
                context.ambassador(),
                Component.translatable(
                        "mcacapitals.ui.royal_betrothal.choose_couples_capital"
                ),
                entries,
                Component.translatable(
                        "mcacapitals.ui.royal_betrothal.choose_household",
                        royalNameComponent(
                                context.level(),
                                context.source(),
                                sourceRoyalId
                        ),
                        royalNameComponent(
                                context.level(),
                                context.target(),
                                targetRoyalId
                        )
                ),
                "/capitaldiplomacy betrothal_target "
                        + ambassadorId
                        + " "
                        + targetCapitalId
                        + " "
                        + sourceRoyalId
        );

        return 1;
    }

    public static int proposeSelected(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            UUID sourceRoyalId,
            UUID targetRoyalId,
            UUID destinationCapitalId
    ) {
        SelectionContext context =
                validateSelectionContext(
                        player,
                        ambassadorId,
                        targetCapitalId
                );

        if (!context.valid()) {
            sendFailure(
                    player,
                    context.failureMessage()
            );

            return 0;
        }

        Match match =
                createMatch(
                        context.level(),
                        context.source(),
                        context.target(),
                        sourceRoyalId,
                        targetRoyalId,
                        destinationCapitalId
                );

        if (match == null) {
            sendFailure(
                    player,
                    Component.translatable(
                            "mcacapitals.ui.royal_betrothal.pair_or_settlement_no_longer_eligible"
                    )
            );

            return 0;
        }

        return CapitalDiplomaticProposalService
                .proposeRoyalBetrothal(
                        player,
                        ambassadorId,
                        targetCapitalId,
                        match
                );
    }

    static boolean establish(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (level == null || proposal == null || source == null || target == null
                || !proposal.hasRoyalBetrothalDetails()) {
            return false;
        }
        Match match = createMatch(
                level,
                source,
                target,
                proposal.getSourceRoyalId(),
                proposal.getTargetRoyalId(),
                proposal.getDestinationCapitalId()
        );
        if (match == null || !match.relocatingRoyalId().equals(proposal.getRelocatingRoyalId())) {
            return false;
        }

        String firstName = royalName(level, source, match.sourceRoyalId());
        String secondName = royalName(level, target, match.targetRoyalId());
        PendingVillagerBetrothalAccess.setRoyalEscort(
                level,
                match.sourceRoyalId(),
                firstName,
                match.targetRoyalId(),
                secondName,
                match.originCapitalId(),
                match.destinationCapitalId(),
                match.relocatingRoyalId()
        );

        String sourceName = capitalName(level, source);
        String targetName = capitalName(level, target);
        String destinationName = capitalName(
                level, CapitalManager.getCapital(match.destinationCapitalId())
        );
        CapitalChronicleService.addEvent(
                level, source, CapitalChronicleEventId.BETROTHAL_AGREEMENT,
                firstName, secondName, sourceName, targetName, destinationName
        );
        CapitalChronicleService.addEvent(
                level, target, CapitalChronicleEventId.BETROTHAL_AGREEMENT,
                firstName, secondName, sourceName, targetName, destinationName
        );
        notifyEscortRequest(level, source, target, proposal);
        return true;
    }

    public static boolean hasOpenEscortRequests(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null || capital.getCapitalId() == null) {
            return false;
        }
        for (PendingVillagerBetrothalSavedData.RoyalEscortRecord record :
                PendingVillagerBetrothalAccess.getRoyalEscorts(level)) {
            if (!record.isCompleted()
                    && (capital.getCapitalId().equals(record.originCapitalId())
                    || capital.getCapitalId().equals(record.destinationCapitalId()))) {
                return true;
            }
        }
        return false;
    }

    public static int openEscortRequests(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        if (player == null
                || ambassadorId == null) {
            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        Entity ambassador =
                level.getEntity(
                        ambassadorId
                );

        CapitalRecord audience =
                resolveAmbassadorCapital(
                        level,
                        player,
                        ambassador
                );

        if (audience == null) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_royal_betrothal_service.the_ambassador_is_unavailable")
            );

            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (PendingVillagerBetrothalSavedData.RoyalEscortRecord record :
                PendingVillagerBetrothalAccess
                        .getRoyalEscorts(level)) {
            if (record.isCompleted()
                    || !audience.getCapitalId()
                    .equals(
                            record.originCapitalId()
                    )
                    && !audience.getCapitalId()
                    .equals(
                            record.destinationCapitalId()
                    )) {
                continue;
            }

            CapitalRecord origin =
                    CapitalManager.getCapital(
                            record.originCapitalId()
                    );

            CapitalRecord destination =
                    CapitalManager.getCapital(
                            record.destinationCapitalId()
                    );

            String relocatingName =
                    record.nameFor(
                            record.relocatingRoyalId()
                    );

            String partnerName =
                    record.partnerNameFor(
                            record.relocatingRoyalId()
                    );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            Component.translatable(
                                    "mcacapitals.ui.royal_escort.couple",
                                    royalNameComponent(relocatingName),
                                    royalNameComponent(partnerName)
                            ),
                            Component.translatable(
                                    "mcacapitals.ui.royal_escort.escort_from",
                                    royalNameComponent(relocatingName),
                                    CapitalDiplomaticAgreementText.capitalNameComponent(
                                            level,
                                            origin
                                    )
                            ),
                            Component.translatable(
                                    "mcacapitals.ui.royal_escort.destination",
                                    CapitalDiplomaticAgreementText.capitalNameComponent(
                                            level,
                                            destination
                                    )
                            ),
                            Component.translatable(
                                    "mcacapitals.ui.royal_escort.completion_explanation"
                            ),
                            Component.translatable(
                                    "mcacapitals.ui.royal_escort.pending"
                            ),
                            "",
                            false,
                            Component.translatable(
                                    "mcacapitals.ui.royal_escort.pending_reason"
                            )
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .ROYAL_ESCORT_REQUESTS,
                        Component.translatable(
                                "mcacapitals.ui.royal_escort.requests_title"
                        ),
                        ambassador.getName(),
                        entries.isEmpty()
                                ? Component.translatable(
                                        "mcacapitals.ui.royal_escort.none_pending"
                                )
                                : Component.translatable(
                                        "mcacapitals.ui.royal_escort.pending_description"
                                ),
                        "/capitalurgent continue "
                                + ambassadorId,
                        entries,
                        List.of()
                )
        );

        return 1;
    }

    public static void tickEscorts(ServerLevel level) {
        if (level == null) {
            return;
        }
        for (PendingVillagerBetrothalSavedData.RoyalEscortRecord record :
                PendingVillagerBetrothalAccess.getRoyalEscorts(level)) {
            if (record.isCompleted()) {
                continue;
            }
            CapitalRecord destination = CapitalManager.getCapital(record.destinationCapitalId());
            if (destination == null
                    || destination.getState() != CapitalState.ACTIVE
                    || destination.getVillageId() == null) {
                continue;
            }
            if (CapitalDiplomacyDataAccess.getDiplomaticState(
                    level, record.originCapitalId(), record.destinationCapitalId()
            ) == CapitalDiplomaticState.WAR) {
                continue;
            }

            Entity firstEntity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(
                    level, record.pair().first()
            );
            Entity secondEntity = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(
                    level, record.pair().second()
            );
            if (!(firstEntity instanceof VillagerEntityMCA firstVillager)
                    || !(secondEntity instanceof VillagerEntityMCA secondVillager)
                    || !firstVillager.isAlive()
                    || !secondVillager.isAlive()) {
                continue;
            }

            Village destinationVillage = VillageManager.get(level)
                    .getOrEmpty(destination.getVillageId())
                    .orElse(null);
            if (destinationVillage == null
                    || !destinationVillage.isWithinBorder(firstVillager)
                    || !destinationVillage.isWithinBorder(secondVillager)) {
                continue;
            }

            MCAIntegrationBridge.forceVillageResidency(
                    level,
                    record.relocatingRoyalId(),
                    destination.getVillageId()
            );

            if (!PendingVillagerBetrothalAccess.completeRoyalEscort(
                    level, record.pair().first(), record.pair().second()
            )) {
                continue;
            }

            CapitalRecord origin = CapitalManager.getCapital(record.originCapitalId());
            String destinationName = capitalName(level, destination);
            String firstName = record.nameFor(record.pair().first());
            String secondName = record.nameFor(record.pair().second());
            if (origin != null) {
                CapitalChronicleService.addEvent(
                        level, origin, CapitalChronicleEventId.BETROTHAL_ESCORT_COMPLETED,
                        firstName, secondName, destinationName
                );
            }
            CapitalChronicleService.addEvent(
                    level, destination, CapitalChronicleEventId.BETROTHAL_ESCORT_COMPLETED,
                    firstName, secondName, destinationName
            );
        }
    }

    public static void completeRoyalMarriage(
            ServerLevel level,
            PendingVillagerBetrothalSavedData.RoyalEscortRecord escort,
            Entity firstVillager,
            Entity secondVillager
    ) {
        if (level == null || escort == null || firstVillager == null || secondVillager == null) {
            return;
        }
        CapitalRecord origin = CapitalManager.getCapital(escort.originCapitalId());
        CapitalRecord destination = CapitalManager.getCapital(escort.destinationCapitalId());
        if (origin == null || destination == null) {
            return;
        }
        CapitalDiplomacyDataAccess.adjustRelationship(
                level,
                origin.getCapitalId(),
                destination.getCapitalId(),
                MARRIAGE_RELATIONSHIP_BONUS,
                "mcacapitals.relationship_reason.royal_marriage_completed",
                destination.getCapitalId()
        );
        String firstName = CapitalChronicleIdentitySnapshot.name(level, origin, firstVillager.getUUID());
        String secondName = CapitalChronicleIdentitySnapshot.name(level, destination, secondVillager.getUUID());
        String originName = capitalName(level, origin);
        String destinationName = capitalName(level, destination);
        CapitalChronicleService.addEvent(
                level, origin, CapitalChronicleEventId.ROYAL_MARRIAGE_COMPLETED,
                firstName, secondName, originName, destinationName
        );
        CapitalChronicleService.addEvent(
                level, destination, CapitalChronicleEventId.ROYAL_MARRIAGE_COMPLETED,
                firstName, secondName, originName, destinationName
        );
    }

    static Component proposalDescription(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (proposal == null
                || !proposal
                .hasRoyalBetrothalDetails()) {
            return Component.translatable(
                    "mcacapitals.diplomacy.royal_betrothal.description.generic"
            );
        }

        return Component.translatable(
                "mcacapitals.diplomacy.royal_betrothal.description.specific",
                royalNameComponent(
                        level,
                        source,
                        proposal.getSourceRoyalId()
                ),
                royalNameComponent(
                        level,
                        target,
                        proposal.getTargetRoyalId()
                ),
                CapitalDiplomaticAgreementText.capitalNameComponent(
                        level,
                        CapitalManager.getCapital(
                                proposal.getDestinationCapitalId()
                        )
                )
        );
    }

    static List<UUID> eligibleRoyals(ServerLevel level, CapitalRecord capital) {
        if (level == null || capital == null) {
            return List.of();
        }
        Set<UUID> candidates = new LinkedHashSet<>();
        if (capital.getHeir() != null) {
            candidates.add(capital.getHeir());
        }
        candidates.addAll(capital.getRoyalChildren());
        candidates.addAll(capital.getLegitimizedRoyalChildren());
        candidates.addAll(capital.getRoyalHousehold());
        UUID sovereignId = capital.getSovereign();
        if (sovereignId != null) {
            candidates.addAll(MCAIntegrationBridge.getChildren(level, sovereignId));
        }

        List<UUID> result = new ArrayList<>();
        for (UUID candidate : candidates) {
            if (isEligibleRoyal(level, capital, candidate)) {
                result.add(candidate);
            }
        }
        result.sort(Comparator
                .comparingInt((UUID id) -> isCrownHeir(capital, id) ? 0 : 1)
                .thenComparing(id -> royalName(level, capital, id), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(UUID::toString));
        return List.copyOf(result);
    }

    static boolean isEligibleRoyal(ServerLevel level, CapitalRecord capital, UUID royalId) {
        if (level == null || capital == null || royalId == null) {
            return false;
        }
        UUID sovereignId = capital.getSovereign();
        boolean recognizedRoyal = royalId.equals(capital.getHeir())
                || capital.isRoyalChild(royalId)
                || capital.isLegitimizedRoyalChild(royalId)
                || capital.getRoyalHousehold().contains(royalId)
                || sovereignId != null && MCAIntegrationBridge.isChildOf(level, royalId, sovereignId);
        return recognizedRoyal
                && !capital.isDisinheritedRoyalChild(royalId)
                && !PendingVillagerBetrothalAccess.hasPendingBetrothal(level, royalId)
                && MCAIntegrationBridge.getSpouse(level, royalId) == null
                && !MCAIntegrationBridge.isFamilyNodeDeceased(level, royalId);
    }

    private static boolean isValidPair(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            UUID sourceRoyalId,
            UUID targetRoyalId
    ) {
        return isEligibleRoyal(level, source, sourceRoyalId)
                && isEligibleRoyal(level, target, targetRoyalId)
                && !(isCrownHeir(source, sourceRoyalId) && isCrownHeir(target, targetRoyalId))
                && !MCAIntegrationBridge.areCloselyRelatedForMarriage(
                level, sourceRoyalId, targetRoyalId
        );
    }

    private static Match createMatch(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            UUID sourceRoyalId,
            UUID targetRoyalId,
            UUID destinationCapitalId
    ) {
        if (!isValidPair(level, source, target, sourceRoyalId, targetRoyalId)
                || destinationCapitalId == null) {
            return null;
        }
        UUID forced = forcedDestination(source, target, sourceRoyalId, targetRoyalId);
        if (forced != null && !forced.equals(destinationCapitalId)) {
            return null;
        }
        if (!destinationCapitalId.equals(source.getCapitalId())
                && !destinationCapitalId.equals(target.getCapitalId())) {
            return null;
        }
        boolean settlesInSource = destinationCapitalId.equals(source.getCapitalId());
        UUID relocatingRoyalId = settlesInSource ? targetRoyalId : sourceRoyalId;
        UUID originCapitalId = settlesInSource ? target.getCapitalId() : source.getCapitalId();
        return new Match(
                sourceRoyalId,
                targetRoyalId,
                relocatingRoyalId,
                originCapitalId,
                destinationCapitalId
        );
    }

    private static UUID forcedDestination(
            CapitalRecord source,
            CapitalRecord target,
            UUID sourceRoyalId,
            UUID targetRoyalId
    ) {
        if (isCrownHeir(source, sourceRoyalId)) {
            return source.getCapitalId();
        }
        if (isCrownHeir(target, targetRoyalId)) {
            return target.getCapitalId();
        }
        return null;
    }

    private static boolean isCrownHeir(CapitalRecord capital, UUID royalId) {
        return capital != null && royalId != null && royalId.equals(capital.getHeir());
    }

    private static OpenAmbassadorCommunicationPacket.Entry
    settlementEntry(
            UUID ambassadorId,
            UUID targetCapitalId,
            UUID sourceRoyalId,
            UUID targetRoyalId,
            UUID destinationCapitalId,
            Component destinationName,
            Component explanation
    ) {
        return new OpenAmbassadorCommunicationPacket.Entry(
                Component.translatable(
                        "mcacapitals.ui.royal_betrothal.settle_in",
                        destinationName
                ),
                explanation,
                Component.empty(),
                Component.empty(),
                Component.translatable(
                        "mcacapitals.ui.royal_betrothal.choose",
                        destinationName
                ),
                "/capitaldiplomacy betrothal_send "
                        + ambassadorId
                        + " "
                        + targetCapitalId
                        + " "
                        + sourceRoyalId
                        + " "
                        + targetRoyalId
                        + " "
                        + destinationCapitalId,
                true,
                Component.empty()
        );
    }

    private static SelectionContext validateSelectionContext(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        if (player == null || ambassadorId == null || targetCapitalId == null) {
            return SelectionContext.failure(Component.translatable("mcacapitals.diplomacy.royal_betrothal.validation.selection_invalid"));
        }
        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateAudience(player, ambassadorId);
        if (!audience.valid()) {
            return SelectionContext.failure(audience.failureMessage());
        }
        CapitalRecord target = CapitalManager.getCapital(targetCapitalId);
        Component targetFailure = CapitalDiplomaticAgreementValidation.validateTarget(
                audience.sourceCapital(), target
        );
        if (targetFailure != null) {
            return SelectionContext.failure(targetFailure);
        }
        ServerLevel level = player.serverLevel();
        CapitalDiplomaticState state = CapitalDiplomacyDataAccess.getDiplomaticState(
                level, audience.sourceCapital().getCapitalId(), target.getCapitalId()
        );
        int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                level, audience.sourceCapital().getCapitalId(), target.getCapitalId()
        );
        Component failure = validateProposal(level, audience.sourceCapital(), target, state, score);
        if (failure != null) {
            return SelectionContext.failure(failure);
        }
        return SelectionContext.success(
                level,
                audience.sourceCapital(),
                target,
                level.getEntity(ambassadorId)
        );
    }

    private static void openSelectionScreen(
            ServerPlayer player,
            Entity ambassador,
            Component title,
            List<OpenAmbassadorCommunicationPacket.Entry> entries,
            Component description,
            String backCommand
    ) {
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .ROYAL_ESCORT_REQUESTS,
                        title,
                        ambassador == null
                                ? Component.translatable(
                                        "mcacapitals.dynamic.office.ambassador"
                                )
                                : ambassador.getName(),
                        description,
                        backCommand,
                        entries,
                        List.of()
                )
        );
    }

    private static String royalName(ServerLevel level, CapitalRecord capital, UUID royalId) {
        if (royalId == null) {
            return "Unknown Royal";
        }
        if (capital != null) {
            return CapitalNameService.resolveDisplayName(level, capital, royalId);
        }
        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, royalId);
        return entity == null ? royalId.toString() : entity.getName().getString();
    }

    private static String royalTitle(ServerLevel level, CapitalRecord capital, UUID royalId) {
        String title = CapitalTitleResolver.getDisplayTitle(level, capital, royalId);
        return title == null || title.isBlank() || "None".equals(title)
                ? "Royal"
                : title;
    }

    private static Component royalNameComponent(
            ServerLevel level,
            CapitalRecord capital,
            UUID royalId
    ) {
        return royalNameComponent(royalName(level, capital, royalId));
    }
    private static Component royalNameComponent(String name) {
        return name == null
                || name.isBlank()
                || "Unknown Royal".equals(name)
                ? Component.translatable("mcacapitals.ui.royal_betrothal.unknown_royal")
                : Component.literal(name);
    }
    private static Component capitalNameComponent(String name) {
        return name == null
                || name.isBlank()
                || "Unknown Capital".equals(name)
                ? Component.translatable("mcacapitals.diplomacy.unknown_capital")
                : Component.literal(name);
    }
    private static Component royalTitleComponent(
            ServerLevel level,
            CapitalRecord capital,
            UUID royalId
    ) {
        Component title =
                CapitalTitleResolver
                        .getDisplayTitleComponent(
                                level,
                                capital,
                                royalId
                        );

        return title == null
                || title.getString().isBlank()
                || CapitalTitleResolver.getResolvedTitleId(
                        level,
                        capital,
                        royalId
                ) == CapitalTitleResolver.ResolvedTitleId.NONE
                ? Component.translatable(
                        "mcacapitals.ui.royal_betrothal.royal"
                )
                : title;
    }

    private static String capitalName(ServerLevel level, CapitalRecord capital) {
        return CapitalDiplomaticAgreementText.capitalName(level, capital);
    }

    private static CapitalRecord resolveAmbassadorCapital(
            ServerLevel level,
            ServerPlayer player,
            Entity ambassador
    ) {
        if (level == null || player == null || ambassador == null
                || !ambassador.isAlive()
                || player.level() != ambassador.level()
                || player.distanceToSqr(ambassador) > MAX_AMBASSADOR_DISTANCE_SQR) {
            return null;
        }
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && capital.getState() == CapitalState.ACTIVE
                    && CapitalAmbassadorService.isAmbassador(
                    level, capital, ambassador.getUUID()
            )) {
                return capital;
            }
        }
        return null;
    }

    private static void notifyEscortRequest(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            DiplomaticProposal proposal
    ) {
        CapitalRecord origin =
                CapitalManager.getCapital(
                        proposal
                                .getDestinationCapitalId()
                                .equals(
                                        source.getCapitalId()
                                )
                                ? target.getCapitalId()
                                : source.getCapitalId()
                );

        CapitalRecord destination =
                CapitalManager.getCapital(
                        proposal.getDestinationCapitalId()
                );

        String relocatingName =
                royalName(
                        level,
                        origin,
                        proposal.getRelocatingRoyalId()
                );

        String firstName =
                royalName(
                        level,
                        source,
                        proposal.getSourceRoyalId()
                );

        String secondName =
                royalName(
                        level,
                        target,
                        proposal.getTargetRoyalId()
                );

        String destinationName =
                capitalName(
                        level,
                        destination
                );

        Component message =
                Component.translatable(
                        "mcacapitals.ui.royal_escort.accepted_message",
                        royalNameComponent(relocatingName),
                        CapitalDiplomaticAgreementText.capitalNameComponent(
                                level,
                                origin
                        ),
                        capitalNameComponent(destinationName),
                        royalNameComponent(firstName),
                        royalNameComponent(secondName)
                );

        UUID sourcePlayer =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                source
                        );

        UUID targetPlayer =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                target
                        );

        Component title =
                Component.translatable(
                        "mcacapitals.ui.royal_escort.request_title"
                );

        if (sourcePlayer != null) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            sourcePlayer,
                            title,
                            message
                    );
        }

        if (targetPlayer != null
                && !targetPlayer.equals(
                sourcePlayer
        )) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            targetPlayer,
                            title,
                            message
                    );
        }
    }

    private static void sendFailure(
            ServerPlayer player,
            Component message
    ) {
        if (player != null && message != null) {
            player.sendSystemMessage(message);
        }
    }

    record Match(
            UUID sourceRoyalId,
            UUID targetRoyalId,
            UUID relocatingRoyalId,
            UUID originCapitalId,
            UUID destinationCapitalId
    ) {
    }

    private record SelectionContext(
            boolean valid,
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            Entity ambassador,
            Component failureMessage
    ) {
        private static SelectionContext success(
                ServerLevel level,
                CapitalRecord source,
                CapitalRecord target,
                Entity ambassador
        ) {
            return new SelectionContext(true, level, source, target, ambassador, null);
        }

        private static SelectionContext failure(Component message) {
            return new SelectionContext(false, null, null, null, null, message);
        }
    }
}
