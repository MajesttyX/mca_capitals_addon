package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalAccess;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalSavedData;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
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

    private CapitalRoyalBetrothalService() {
    }

    static String validateProposal(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            CapitalDiplomaticState state,
            int score
    ) {
        if (level == null
                || source == null
                || target == null) {
            return "That Royal Betrothal proposal is invalid.";
        }

        if (state == CapitalDiplomaticState.WAR
                || state == CapitalDiplomaticState.TRUCE) {
            return "A Royal Betrothal cannot be proposed during war or a truce.";
        }

        if (score < MINIMUM_RELATIONSHIP) {
            return "Relations must be at least Cordial before proposing a Royal Betrothal.";
        }

        return findMatch(level, source, target) == null
                ? "These capitals do not currently have two eligible unbetrothed royals."
                : null;
    }

    static Match findMatch(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (level == null
                || source == null
                || target == null
                || source.getCapitalId() == null
                || target.getCapitalId() == null) {
            return null;
        }

        List<UUID> sourceRoyals =
                eligibleRoyals(level, source);

        List<UUID> targetRoyals =
                eligibleRoyals(level, target);

        for (UUID sourceRoyalId : sourceRoyals) {
            for (UUID targetRoyalId : targetRoyals) {
                if (MCAIntegrationBridge
                        .areCloselyRelatedForMarriage(
                                level,
                                sourceRoyalId,
                                targetRoyalId
                        )) {
                    continue;
                }

                int sourceRank = royalRank(
                        source,
                        sourceRoyalId
                );

                int targetRank = royalRank(
                        target,
                        targetRoyalId
                );

                UUID relocatingRoyalId;
                UUID originCapitalId;
                UUID destinationCapitalId;

                if (sourceRank > targetRank) {
                    relocatingRoyalId = targetRoyalId;
                    originCapitalId = target.getCapitalId();
                    destinationCapitalId =
                            source.getCapitalId();
                } else {
                    relocatingRoyalId = sourceRoyalId;
                    originCapitalId = source.getCapitalId();
                    destinationCapitalId =
                            target.getCapitalId();
                }

                return new Match(
                        sourceRoyalId,
                        targetRoyalId,
                        relocatingRoyalId,
                        originCapitalId,
                        destinationCapitalId
                );
            }
        }

        return null;
    }

    static boolean establish(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (level == null
                || proposal == null
                || source == null
                || target == null
                || !proposal.hasRoyalBetrothalDetails()) {
            return false;
        }

        UUID sourceRoyalId = proposal.getSourceRoyalId();
        UUID targetRoyalId = proposal.getTargetRoyalId();

        if (!isEligibleRoyal(
                level,
                source,
                sourceRoyalId
        )
                || !isEligibleRoyal(
                level,
                target,
                targetRoyalId
        )
                || MCAIntegrationBridge
                .areCloselyRelatedForMarriage(
                        level,
                        sourceRoyalId,
                        targetRoyalId
                )) {
            return false;
        }

        UUID destinationCapitalId =
                proposal.getDestinationCapitalId();
        UUID relocatingRoyalId =
                proposal.getRelocatingRoyalId();

        boolean destinationIsSource =
                destinationCapitalId.equals(
                        source.getCapitalId()
                );
        boolean destinationIsTarget =
                destinationCapitalId.equals(
                        target.getCapitalId()
                );

        if (!destinationIsSource
                && !destinationIsTarget
                || destinationIsSource
                && !targetRoyalId.equals(relocatingRoyalId)
                || destinationIsTarget
                && !sourceRoyalId.equals(relocatingRoyalId)) {
            return false;
        }

        UUID originCapitalId = destinationIsSource
                ? target.getCapitalId()
                : source.getCapitalId();

        PendingVillagerBetrothalAccess.setRoyalEscort(
                level,
                sourceRoyalId,
                targetRoyalId,
                originCapitalId,
                destinationCapitalId,
                proposal.getRelocatingRoyalId()
        );

        String sourceName =
                CapitalDiplomaticAgreementText
                        .capitalName(level, source);

        String targetName =
                CapitalDiplomaticAgreementText
                        .capitalName(level, target);

        String firstName =
                royalName(
                        level,
                        source,
                        sourceRoyalId
                );

        String secondName =
                royalName(
                        level,
                        target,
                        targetRoyalId
                );

        CapitalChronicleService.addEntry(
                level,
                source,
                firstName
                        + " and "
                        + secondName
                        + " were betrothed by agreement between "
                        + sourceName
                        + " and "
                        + targetName
                        + "."
        );

        CapitalChronicleService.addEntry(
                level,
                target,
                firstName
                        + " and "
                        + secondName
                        + " were betrothed by agreement between "
                        + sourceName
                        + " and "
                        + targetName
                        + "."
        );

        notifyEscortRequest(
                level,
                source,
                target,
                proposal
        );

        return true;
    }

    public static boolean hasOpenEscortRequests(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null) {
            return false;
        }

        for (PendingVillagerBetrothalSavedData.RoyalEscortRecord record :
                PendingVillagerBetrothalAccess
                        .getRoyalEscorts(level)) {
            if (!record.isCompleted()
                    && (capital.getCapitalId().equals(
                    record.originCapitalId()
            )
                    || capital.getCapitalId().equals(
                    record.destinationCapitalId()
            ))) {
                return true;
            }
        }

        return false;
    }

    public static int openEscortRequests(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        if (player == null || ambassadorId == null) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        Entity ambassador = level.getEntity(ambassadorId);

        CapitalRecord audience =
                resolveAmbassadorCapital(
                        level,
                        player,
                        ambassador
                );

        if (audience == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "The Ambassador is unavailable."
                    )
            );

            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (PendingVillagerBetrothalSavedData.RoyalEscortRecord record :
                PendingVillagerBetrothalAccess
                        .getRoyalEscorts(level)) {
            if (record.isCompleted()
                    || !audience.getCapitalId().equals(
                    record.originCapitalId()
            )
                    && !audience.getCapitalId().equals(
                    record.destinationCapitalId()
            )) {
                continue;
            }

            CapitalRecord origin = CapitalManager.getCapital(
                    record.originCapitalId()
            );

            CapitalRecord destination = CapitalManager.getCapital(
                    record.destinationCapitalId()
            );

            UUID partnerId = record.pair().first().equals(
                    record.relocatingRoyalId()
            )
                    ? record.pair().second()
                    : record.pair().first();

            String relocatingName = royalName(
                    level,
                    origin,
                    record.relocatingRoyalId()
            );

            String partnerName = royalName(
                    level,
                    destination,
                    partnerId
            );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            relocatingName
                                    + " and "
                                    + partnerName,
                            "Escort "
                                    + relocatingName
                                    + " from "
                                    + CapitalDiplomaticAgreementText
                                    .capitalName(level, origin)
                                    + ".",
                            "Destination: "
                                    + CapitalDiplomaticAgreementText
                                    .capitalName(
                                            level,
                                            destination
                                    ),
                            "No time limit. Marriage waits until both royals live in the same capital.",
                            "Escort Pending",
                            "",
                            false,
                            "Lead the relocating royal into the destination capital."
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.ROYAL_ESCORT_REQUESTS,
                        "Royal Escort Requests",
                        ambassador.getName().getString(),
                        entries.isEmpty()
                                ? "There are no pending royal escorts connected to this capital."
                                : "These accepted betrothals are waiting for a royal to be escorted to their future spouse's capital.",
                        "",
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
                PendingVillagerBetrothalAccess
                        .getRoyalEscorts(level)) {
            if (record.isCompleted()) {
                continue;
            }

            CapitalRecord destination =
                    CapitalManager.getCapital(
                            record.destinationCapitalId()
                    );

            if (destination == null
                    || destination.getState()
                    != CapitalState.ACTIVE
                    || destination.getVillageId() == null) {
                continue;
            }

            if (CapitalDiplomacyDataAccess.getDiplomaticState(
                    level,
                    record.originCapitalId(),
                    record.destinationCapitalId()
            ) == CapitalDiplomaticState.WAR) {
                continue;
            }

            Entity entity = MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            record.relocatingRoyalId()
                    );

            if (!(entity instanceof VillagerEntityMCA villager)
                    || !villager.isAlive()) {
                continue;
            }

            Village destinationVillage =
                    VillageManager.get(level)
                            .getOrEmpty(
                                    destination.getVillageId()
                            )
                            .orElse(null);

            if (destinationVillage == null
                    || !destinationVillage
                    .isWithinBorder(villager)) {
                continue;
            }

            boolean alreadyResident =
                    villager.getResidency()
                            .getHomeVillage()
                            .map(home ->
                                    home.getId()
                                            == destination
                                            .getVillageId()
                            )
                            .orElse(false);

            if (!alreadyResident) {
                if (!destinationVillage.hasSpace()) {
                    continue;
                }

                villager.getResidency().leaveHome();
                villager.getResidency().seekHome();

                alreadyResident = villager.getResidency()
                        .getHomeVillage()
                        .map(home ->
                                home.getId()
                                        == destination
                                        .getVillageId()
                        )
                        .orElse(false);
            }

            if (!alreadyResident) {
                continue;
            }

            if (PendingVillagerBetrothalAccess
                    .completeRoyalEscort(
                            level,
                            record.pair().first(),
                            record.pair().second()
                    )) {
                CapitalRecord origin =
                        CapitalManager.getCapital(
                                record.originCapitalId()
                        );

                String royalName = royalName(
                        level,
                        origin,
                        record.relocatingRoyalId()
                );

                String destinationName =
                        CapitalDiplomaticAgreementText
                                .capitalName(
                                        level,
                                        destination
                                );

                if (origin != null) {
                    CapitalChronicleService.addEntry(
                            level,
                            origin,
                            royalName
                                    + " departed for "
                                    + destinationName
                                    + " under a royal betrothal agreement."
                    );
                }

                CapitalChronicleService.addEntry(
                        level,
                        destination,
                        royalName
                                + " arrived in "
                                + destinationName
                                + " under a royal betrothal agreement."
                );
            }
        }
    }

    public static void completeRoyalMarriage(
            ServerLevel level,
            PendingVillagerBetrothalSavedData.RoyalEscortRecord escort,
            Entity firstVillager,
            Entity secondVillager
    ) {
        if (level == null
                || escort == null
                || firstVillager == null
                || secondVillager == null) {
            return;
        }

        CapitalRecord origin = CapitalManager.getCapital(
                escort.originCapitalId()
        );

        CapitalRecord destination = CapitalManager.getCapital(
                escort.destinationCapitalId()
        );

        if (origin == null || destination == null) {
            return;
        }

        CapitalDiplomacyDataAccess.adjustRelationship(
                level,
                origin.getCapitalId(),
                destination.getCapitalId(),
                10,
                "Royal marriage completed",
                destination.getCapitalId()
        );

        String entry = firstVillager.getName().getString()
                + " and "
                + secondVillager.getName().getString()
                + " completed the royal marriage joining "
                + CapitalDiplomaticAgreementText
                .capitalName(level, origin)
                + " and "
                + CapitalDiplomaticAgreementText
                .capitalName(level, destination)
                + ".";

        CapitalChronicleService.addEntry(
                level,
                origin,
                entry
        );

        CapitalChronicleService.addEntry(
                level,
                destination,
                entry
        );
    }

    static String proposalDescription(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (proposal == null
                || !proposal.hasRoyalBetrothalDetails()) {
            return "a Royal Betrothal";
        }

        return "the betrothal of "
                + royalName(
                level,
                source,
                proposal.getSourceRoyalId()
        )
                + " and "
                + royalName(
                level,
                target,
                proposal.getTargetRoyalId()
        );
    }

    private static List<UUID> eligibleRoyals(
            ServerLevel level,
            CapitalRecord capital
    ) {
        Set<UUID> candidates = new LinkedHashSet<>();

        if (capital.getHeir() != null) {
            candidates.add(capital.getHeir());
        }

        candidates.addAll(capital.getRoyalChildren());
        candidates.addAll(
                capital.getLegitimizedRoyalChildren()
        );

        List<UUID> result = new ArrayList<>();

        for (UUID candidate : candidates) {
            if (isEligibleRoyal(
                    level,
                    capital,
                    candidate
            )) {
                result.add(candidate);
            }
        }

        result.sort(
                Comparator
                        .comparingInt(
                                (UUID candidate) ->
                                        royalRank(
                                                capital,
                                                candidate
                                        )
                        )
                        .reversed()
                        .thenComparing(UUID::toString)
        );

        return result;
    }

    private static boolean isEligibleRoyal(
            ServerLevel level,
            CapitalRecord capital,
            UUID royalId
    ) {
        if (level == null
                || capital == null
                || royalId == null) {
            return false;
        }

        boolean recognizedRoyal =
                royalId.equals(capital.getHeir())
                        || capital.isRoyalChild(royalId)
                        || capital.isLegitimizedRoyalChild(royalId);

        if (!recognizedRoyal
                || capital.isDisinheritedRoyalChild(royalId)
                || PendingVillagerBetrothalAccess
                .hasPendingBetrothal(level, royalId)
                || MCAIntegrationBridge.getSpouse(
                level,
                royalId
        ) != null
                || MCAIntegrationBridge.isFamilyNodeDeceased(
                level,
                royalId
        )) {
            return false;
        }

        String age = MCAIntegrationBridge
                .getAgeState(level, royalId);

        return "BABY".equalsIgnoreCase(age)
                || "TODDLER".equalsIgnoreCase(age)
                || "CHILD".equalsIgnoreCase(age)
                || "TEEN".equalsIgnoreCase(age)
                || "ADULT".equalsIgnoreCase(age);
    }

    private static int royalRank(
            CapitalRecord capital,
            UUID royalId
    ) {
        if (capital != null
                && royalId != null
                && royalId.equals(capital.getHeir())) {
            return 2;
        }

        return 1;
    }

    private static String royalName(
            ServerLevel level,
            CapitalRecord capital,
            UUID royalId
    ) {
        if (royalId == null) {
            return "Unknown Royal";
        }

        if (capital != null) {
            return CapitalNameService.resolveDisplayName(
                    level,
                    capital,
                    royalId
            );
        }

        Entity entity = MCAIntegrationBridge
                .getEntityByUuid(level, royalId);

        return entity == null
                ? royalId.toString()
                : entity.getName().getString();
    }

    private static CapitalRecord resolveAmbassadorCapital(
            ServerLevel level,
            ServerPlayer player,
            Entity ambassador
    ) {
        if (level == null
                || player == null
                || ambassador == null
                || !ambassador.isAlive()
                || player.level() != ambassador.level()
                || player.distanceToSqr(ambassador)
                > 144.0D) {
            return null;
        }

        for (CapitalRecord capital :
                CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && capital.getState()
                    == CapitalState.ACTIVE
                    && CapitalAmbassadorService
                    .isAmbassador(
                            level,
                            capital,
                            ambassador.getUUID()
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
        CapitalRecord origin = CapitalManager.getCapital(
                proposal.getDestinationCapitalId().equals(
                        source.getCapitalId()
                )
                        ? target.getCapitalId()
                        : source.getCapitalId()
        );

        CapitalRecord destination = CapitalManager.getCapital(
                proposal.getDestinationCapitalId()
        );

        String relocatingName = royalName(
                level,
                origin,
                proposal.getRelocatingRoyalId()
        );

        String message = "The Royal Betrothal has been accepted. Escort "
                + relocatingName
                + " from "
                + CapitalDiplomaticAgreementText
                .capitalName(level, origin)
                + " to "
                + CapitalDiplomaticAgreementText
                .capitalName(level, destination)
                + ". The request has no time limit, and the marriage will not occur until both betrothed royals live in the same capital.";

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

        if (sourcePlayer != null) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            sourcePlayer,
                            "Royal Escort Request",
                            message
                    );
        }

        if (targetPlayer != null
                && !targetPlayer.equals(sourcePlayer)) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            targetPlayer,
                            "Royal Escort Request",
                            message
                    );
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
}