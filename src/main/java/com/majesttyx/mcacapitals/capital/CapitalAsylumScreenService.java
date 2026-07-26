package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalRefugeeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRefugeeRecord;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CapitalAsylumScreenService {

    private CapitalAsylumScreenService() {
    }

    public static boolean hasReviewableRequests(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        CapitalDiplomaticAgreementValidation
                .AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        if (!audience.valid()) {
            return false;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord capital =
                audience.sourceCapital();

        return CapitalBuildingService.hasInn(
                level,
                capital
        )
                && !findCandidates(
                level,
                capital
        ).isEmpty();
    }

    public static int openRequests(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        CapitalDiplomaticAgreementValidation
                .AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        if (!audience.valid()) {
            if (player != null) {
                sendMessage(
                        player,
                        "Asylum Requests",
                        audience.failureMessage(),
                        "/capitaldiplomacy targets "
                                + ambassadorId
                );
            }

            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord targetCapital =
                audience.sourceCapital();

        if (!CapitalBuildingService.hasInn(
                level,
                targetCapital
        )) {
            sendMessage(
                    player,
                    "Asylum Requests",
                    "The capital requires an operational Inn before refugees can request asylum.",
                    "/capitaldiplomacy targets "
                            + ambassadorId
            );

            return 0;
        }

        Village targetVillage =
                getVillage(
                        level,
                        targetCapital
                );

        if (targetVillage == null) {
            sendMessage(
                    player,
                    "Asylum Requests",
                    "The capital's MCA village record is unavailable.",
                    "/capitaldiplomacy targets "
                            + ambassadorId
            );

            return 0;
        }

        List<CapitalRefugeeRecord> candidates =
                findCandidates(
                        level,
                        targetCapital
                );

        if (candidates.isEmpty()) {
            sendMessage(
                    player,
                    "Asylum Requests",
                    "No refugees are currently seeking asylum inside the capital.",
                    "/capitaldiplomacy targets "
                            + ambassadorId
            );

            return 0;
        }

        if (!targetVillage.hasSpace()) {
            sendMessage(
                    player,
                    "Asylum Requests",
                    "The capital has no free MCA residence capacity for a refugee.",
                    "/capitaldiplomacy targets "
                            + ambassadorId
            );

            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (CapitalRefugeeRecord record :
                candidates) {
            Entity refugee =
                    MCAIntegrationBridge
                            .findLoadedMCAVillagerByUuid(
                                    level,
                                    record.getRefugeeId()
                            );

            if (refugee == null) {
                continue;
            }

            String refugeeName =
                    refugee.getName().getString();

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            refugeeName,
                            "Exiled From: "
                                    + record.getOriginCapitalName(),
                            "Status: Seeking Asylum",
                            "",
                            "Grant Asylum to "
                                    + refugeeName,
                            "/capitalasylum grant "
                                    + ambassadorId
                                    + " "
                                    + record.getRefugeeId(),
                            true,
                            ""
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.ASYLUM_REQUESTS,
                        "Asylum Requests",
                        CapitalDiplomaticAgreementText
                                .capitalName(
                                        level,
                                        targetCapital
                                ),
                        "Choose a refugee to admit as a resident of the capital.",
                        "/capitaldiplomacy targets "
                                + ambassadorId,
                        entries,
                        List.of()
                )
        );

        return 1;
    }

    private static void sendMessage(
            ServerPlayer player,
            String title,
            String message,
            String backCommand
    ) {
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

    private static List<CapitalRefugeeRecord>
    findCandidates(
            ServerLevel level,
            CapitalRecord targetCapital
    ) {
        if (level == null
                || targetCapital == null
                || targetCapital.getCapitalId()
                == null) {
            return List.of();
        }

        Village village =
                getVillage(
                        level,
                        targetCapital
                );

        if (village == null) {
            return List.of();
        }

        return CapitalRefugeeDataAccess
                .getAwaitingAsylum(level)
                .stream()
                .filter(record ->
                        record != null
                )
                .filter(record ->
                        !record.getOriginCapitalId()
                                .equals(
                                        targetCapital
                                                .getCapitalId()
                                )
                )
                .filter(record ->
                        isPresentCandidate(
                                level,
                                village,
                                record
                        )
                )
                .sorted(
                        Comparator.comparing(
                                record ->
                                        candidateName(
                                                level,
                                                record
                                        ),
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .toList();
    }

    private static boolean isPresentCandidate(
            ServerLevel level,
            Village village,
            CapitalRefugeeRecord record
    ) {
        Entity entity =
                MCAIntegrationBridge
                        .findLoadedMCAVillagerByUuid(
                                level,
                                record.getRefugeeId()
                        );

        if (!(entity
                instanceof VillagerEntityMCA villager)
                || !villager.isAlive()
                || !MCAIntegrationBridge
                .isTeenOrAdultVillager(
                        level,
                        record.getRefugeeId()
                )) {
            return false;
        }

        return village.isWithinBorder(villager)
                && villager.getResidency()
                .getHomeVillage()
                .isEmpty();
    }

    private static String candidateName(
            ServerLevel level,
            CapitalRefugeeRecord record
    ) {
        Entity entity =
                MCAIntegrationBridge
                        .findLoadedMCAVillagerByUuid(
                                level,
                                record.getRefugeeId()
                        );

        return entity == null
                ? record.getRefugeeId().toString()
                : entity.getName().getString();
    }

    private static Village getVillage(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getVillageId() == null) {
            return null;
        }

        return VillageManager.get(level)
                .getOrEmpty(
                        capital.getVillageId()
                )
                .orElse(null);
    }
}