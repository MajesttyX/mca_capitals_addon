package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticShipment;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CapitalAmbassadorUrgentMatterService {

    private CapitalAmbassadorUrgentMatterService() {
    }

    public static boolean openIfNeeded(ServerPlayer player, Entity ambassador) {
        if (player == null || ambassador == null) {
            return false;
        }
        UUID ambassadorId = ambassador.getUUID();
        Context context = resolveContext(player, ambassadorId);
        if (!context.valid()) {
            return false;
        }
        MatterSummary summary = collectMatters(context);
        if (!summary.hasAny()) {
            return false;
        }
        openHub(context, summary);
        return true;
    }

    public static int open(ServerPlayer player, UUID ambassadorId) {
        Context context = resolveContext(player, ambassadorId);
        if (!context.valid()) {
            sendMessage(player, "Urgent Matters", context.failureMessage(), "");
            return 0;
        }
        MatterSummary summary = collectMatters(context);
        if (!summary.hasAny()) {
            sendMessage(
                    player,
                    "Urgent Matters",
                    "There are no diplomatic matters presently awaiting your decision.",
                    ""
            );
            return 0;
        }
        openHub(context, summary);
        return 1;
    }

    public static int openProposals(ServerPlayer player, UUID ambassadorId) {
        Context context = resolveContext(player, ambassadorId);
        if (!context.valid()) {
            sendMessage(player, "Diplomatic Proposals", context.failureMessage(), "");
            return 0;
        }
        List<DiplomaticProposal> proposals = pendingProposals(context);
        if (proposals.isEmpty()) {
            return open(player, ambassadorId);
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries = new ArrayList<>();
        for (DiplomaticProposal proposal : proposals) {
            CapitalRecord source = CapitalManager.getCapital(proposal.getSourceCapitalId());
            String sourceName = source == null
                    ? "Unknown Capital"
                    : CapitalDiplomaticAgreementText.capitalName(context.level(), source);
            int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                    context.level(),
                    proposal.getSourceCapitalId(),
                    proposal.getTargetCapitalId()
            );
            CapitalDiplomaticState state = CapitalDiplomacyDataAccess.getDiplomaticState(
                    context.level(),
                    proposal.getSourceCapitalId(),
                    proposal.getTargetCapitalId()
            );
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    proposal.getType().getDisplayName(),
                    "From: " + sourceName,
                    "Relations: " + CapitalRelationshipBand.fromScore(score).getDisplayName() + " (" + score + ")",
                    "Current State: " + CapitalDiplomaticAgreementText.stateDisplay(state),
                    "Review Proposal",
                    "/capitalurgent proposal " + ambassadorId + " " + proposal.getProposalId(),
                    true,
                    ""
            ));
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.FOREIGN_AFFAIRS,
                        "Urgent Diplomatic Proposals",
                        titledPlayerName(context),
                        "Choose a proposal to inspect before answering the foreign court.",
                        "/capitalurgent open " + ambassadorId,
                        entries,
                        List.of()
                )
        );
        return 1;
    }

    public static int openProposal(
            ServerPlayer player,
            UUID ambassadorId,
            UUID proposalId
    ) {
        Context context = resolveContext(player, ambassadorId);
        if (!context.valid()) {
            sendMessage(player, "Diplomatic Proposal", context.failureMessage(), "");
            return 0;
        }
        DiplomaticProposal proposal = pendingProposals(context).stream()
                .filter(candidate -> proposalId.equals(candidate.getProposalId()))
                .findFirst()
                .orElse(null);
        if (proposal == null) {
            sendMessage(
                    player,
                    "Diplomatic Proposal",
                    "That proposal is no longer awaiting your decision.",
                    "/capitalurgent proposals " + ambassadorId
            );
            return 0;
        }

        CapitalRecord source = CapitalManager.getCapital(proposal.getSourceCapitalId());
        String sourceName = source == null
                ? "Unknown Capital"
                : CapitalDiplomaticAgreementText.capitalName(context.level(), source);
        int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                context.level(),
                proposal.getSourceCapitalId(),
                proposal.getTargetCapitalId()
        );
        String message = sourceName
                + " formally proposes a "
                + proposal.getType().getDisplayName()
                + ".\n\nRelations: "
                + CapitalRelationshipBand.fromScore(score).getDisplayName()
                + " ("
                + score
                + ").";

        List<OpenAmbassadorCommunicationPacket.Action> actions = List.of(
                new OpenAmbassadorCommunicationPacket.Action(
                        "Accept Proposal",
                        "Accept the terms and send the court's formal reply.",
                        "/capitalsacceptproposal " + proposal.getProposalId(),
                        true
                ),
                new OpenAmbassadorCommunicationPacket.Action(
                        "Reject Proposal",
                        "Decline the terms and return the court's formal reply.",
                        "/capitalsrejectproposal " + proposal.getProposalId(),
                        true
                )
        );

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS,
                        proposal.getType().getDisplayName(),
                        "Proposal from " + sourceName,
                        message,
                        "/capitalurgent proposals " + ambassadorId,
                        List.of(),
                        actions
                )
        );
        return 1;
    }

    public static int openPackages(ServerPlayer player, UUID ambassadorId) {
        Context context = resolveContext(player, ambassadorId);
        if (!context.valid()) {
            sendMessage(player, "Diplomatic Packages", context.failureMessage(), "");
            return 0;
        }
        List<DiplomaticShipment> shipments = pendingShipments(context);
        if (shipments.isEmpty()) {
            return open(player, ambassadorId);
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries = new ArrayList<>();
        for (DiplomaticShipment shipment : shipments) {
            CapitalRecord source = CapitalManager.getCapital(shipment.getSourceCapitalId());
            String sourceName = source == null
                    ? "Unknown Capital"
                    : CapitalDiplomaticAgreementText.capitalName(context.level(), source);
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    "Package from " + sourceName,
                    "Court Appraisal: " + shipment.getAppraisal(),
                    "Relationship Effect: " + signed(shipment.getRelationshipDelta()),
                    firstContentsLine(shipment),
                    "Inspect Package",
                    "/capitalurgent package " + ambassadorId + " " + shipment.getShipmentId(),
                    true,
                    ""
            ));
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.FOREIGN_AFFAIRS,
                        "Urgent Diplomatic Packages",
                        titledPlayerName(context),
                        "Choose a package to inspect before answering the sending court.",
                        "/capitalurgent open " + ambassadorId,
                        entries,
                        List.of()
                )
        );
        return 1;
    }

    public static int openPackage(
            ServerPlayer player,
            UUID ambassadorId,
            UUID shipmentId
    ) {
        Context context = resolveContext(player, ambassadorId);
        if (!context.valid()) {
            sendMessage(player, "Diplomatic Package", context.failureMessage(), "");
            return 0;
        }
        DiplomaticShipment shipment = pendingShipments(context).stream()
                .filter(candidate -> shipmentId.equals(candidate.getShipmentId()))
                .findFirst()
                .orElse(null);
        if (shipment == null) {
            sendMessage(
                    player,
                    "Diplomatic Package",
                    "That package is no longer awaiting your decision.",
                    "/capitalurgent packages " + ambassadorId
            );
            return 0;
        }

        CapitalRecord source = CapitalManager.getCapital(shipment.getSourceCapitalId());
        String sourceName = source == null
                ? "Unknown Capital"
                : CapitalDiplomaticAgreementText.capitalName(context.level(), source);
        String message = "The court of "
                + sourceName
                + " has sent:\n\n"
                + CapitalDiplomaticCorrespondenceService.formatContents(shipment.getContents())
                + "\n\nCourt appraisal: "
                + shipment.getAppraisal()
                + ".\nRelationship effect if accepted: "
                + signed(shipment.getRelationshipDelta())
                + ".";

        List<OpenAmbassadorCommunicationPacket.Action> actions = List.of(
                new OpenAmbassadorCommunicationPacket.Action(
                        "Accept Package",
                        "Admit the package to the capital's recognized Storage.",
                        "/capitalsaccept " + shipment.getShipmentId(),
                        true
                ),
                new OpenAmbassadorCommunicationPacket.Action(
                        "Return Package",
                        "Return the package to the sending court without accepting it.",
                        "/capitalsreturn " + shipment.getShipmentId(),
                        true
                )
        );

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.DIPLOMACY_ACTIONS,
                        "Diplomatic Package",
                        "From " + sourceName,
                        message,
                        "/capitalurgent packages " + ambassadorId,
                        List.of(),
                        actions
                )
        );
        return 1;
    }

    private static Context resolveContext(ServerPlayer player, UUID ambassadorId) {
        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateMenuAudience(player, ambassadorId);
        if (!audience.valid()) {
            return Context.failure(player, ambassadorId, audience.failureMessage());
        }
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = audience.sourceCapital();
        if (!CapitalDiplomaticAuthorityService.mayExerciseSovereignAuthority(
                level,
                capital,
                player.getUUID()
        )) {
            return Context.failure(
                    player,
                    ambassadorId,
                    "Only the sovereign, or the Hand serving a villager sovereign, may answer these matters."
            );
        }
        return Context.success(player, ambassadorId, level, capital);
    }

    private static MatterSummary collectMatters(Context context) {
        return new MatterSummary(
                pendingProposals(context),
                pendingShipments(context),
                CapitalAsylumScreenService.hasReviewableRequests(
                        context.player(),
                        context.ambassadorId()
                ),
                CapitalRoyalBetrothalService.hasOpenEscortRequests(
                        context.level(),
                        context.capital()
                )
        );
    }

    private static List<DiplomaticProposal> pendingProposals(Context context) {
        return CapitalDiplomaticAgreementService.getPendingForPlayer(
                        context.level(),
                        context.player().getUUID()
                )
                .stream()
                .filter(proposal -> context.capital().getCapitalId().equals(
                        proposal.getTargetCapitalId()
                ))
                .sorted(Comparator.comparingLong(DiplomaticProposal::getCreatedAt))
                .toList();
    }

    private static List<DiplomaticShipment> pendingShipments(Context context) {
        return CapitalDiplomaticResolutionService.getPendingForPlayer(
                        context.level(),
                        context.player().getUUID()
                )
                .stream()
                .filter(shipment -> context.capital().getCapitalId().equals(
                        shipment.getTargetCapitalId()
                ))
                .sorted(Comparator.comparingLong(DiplomaticShipment::getCreatedAt))
                .toList();
    }

    private static void openHub(Context context, MatterSummary summary) {
        List<OpenAmbassadorCommunicationPacket.Entry> entries = new ArrayList<>();
        if (!summary.proposals().isEmpty()) {
            int count = summary.proposals().size();
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    count == 1 ? "Diplomatic Proposal" : "Diplomatic Proposals",
                    count == 1
                            ? "A foreign court awaits your formal answer."
                            : count + " foreign courts await formal answers.",
                    "",
                    "",
                    "Review " + (count == 1 ? "Proposal" : "Proposals"),
                    "/capitalurgent proposals " + context.ambassadorId(),
                    true,
                    ""
            ));
        }
        if (!summary.shipments().isEmpty()) {
            int count = summary.shipments().size();
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    count == 1 ? "Diplomatic Package" : "Diplomatic Packages",
                    count == 1
                            ? "A package has arrived for the court's judgment."
                            : count + " packages await the court's judgment.",
                    "",
                    "",
                    "Review " + (count == 1 ? "Package" : "Packages"),
                    "/capitalurgent packages " + context.ambassadorId(),
                    true,
                    ""
            ));
        }
        if (summary.asylum()) {
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    "Asylum Request",
                    "A refugee within the capital seeks the protection of the Crown.",
                    "",
                    "",
                    "Review Asylum Request",
                    "/capitalasylum review " + context.ambassadorId(),
                    true,
                    ""
            ));
        }
        if (summary.escort()) {
            entries.add(new OpenAmbassadorCommunicationPacket.Entry(
                    "Royal Escort",
                    "An accepted Royal Betrothal awaits completion of its escort.",
                    "",
                    "",
                    "Review Royal Escort",
                    "/capitalroyalescort review " + context.ambassadorId(),
                    true,
                    ""
            ));
        }

        String message = entries.size() == 1
                ? "There is a matter requiring the court's attention. How shall we proceed?"
                : "There are matters requiring the court's attention. How shall we proceed?";
        ModNetwork.sendToPlayer(
                context.player(),
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.FOREIGN_AFFAIRS,
                        entries.size() == 1 ? "An Urgent Matter" : "Urgent Matters",
                        titledPlayerName(context),
                        message,
                        "",
                        entries,
                        List.of()
                )
        );
    }

    private static String titledPlayerName(Context context) {
        String title = CapitalTitleResolver.getDisplayTitle(
                context.level(),
                context.capital(),
                context.player().getUUID()
        );
        String name = context.player().getName().getString();
        if (title == null
                || title.isBlank()
                || "Commoner".equalsIgnoreCase(title)
                || "None".equalsIgnoreCase(title)) {
            return name;
        }
        return title + " " + name;
    }

    private static String firstContentsLine(DiplomaticShipment shipment) {
        String contents = CapitalDiplomaticCorrespondenceService.formatContents(
                shipment.getContents()
        );
        int newline = contents.indexOf('\n');
        return newline < 0 ? contents : contents.substring(0, newline) + " …";
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static void sendMessage(
            ServerPlayer player,
            String title,
            String message,
            String backCommand
    ) {
        if (player == null) {
            return;
        }
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.MESSAGE,
                        title,
                        "",
                        message,
                        backCommand,
                        List.of(),
                        List.of()
                )
        );
    }

    private record Context(
            boolean valid,
            ServerPlayer player,
            UUID ambassadorId,
            ServerLevel level,
            CapitalRecord capital,
            String failureMessage
    ) {
        static Context success(
                ServerPlayer player,
                UUID ambassadorId,
                ServerLevel level,
                CapitalRecord capital
        ) {
            return new Context(true, player, ambassadorId, level, capital, "");
        }

        static Context failure(
                ServerPlayer player,
                UUID ambassadorId,
                String failureMessage
        ) {
            return new Context(false, player, ambassadorId, null, null, failureMessage);
        }
    }

    private record MatterSummary(
            List<DiplomaticProposal> proposals,
            List<DiplomaticShipment> shipments,
            boolean asylum,
            boolean escort
    ) {
        boolean hasAny() {
            return !proposals.isEmpty() || !shipments.isEmpty() || asylum || escort;
        }
    }
}
