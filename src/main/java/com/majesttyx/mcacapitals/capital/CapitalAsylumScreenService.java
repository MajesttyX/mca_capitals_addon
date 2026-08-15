package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalRefugeeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRefugeeRecord;
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
import java.util.List;
import java.util.UUID;

public final class CapitalAsylumScreenService {

    private CapitalAsylumScreenService() {
    }

    public static boolean hasReviewableRequests(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateMenuAudience(
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

        if (!CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        capital,
                        player.getUUID()
                )) {
            return false;
        }

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
        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateMenuAudience(
                                player,
                                ambassadorId
                        );

        if (!audience.valid()) {
            if (player != null) {
                sendMessage(
                        player,
                        Component.translatable("mcacapitals.ui.asylum.title"),
                        audience.failureMessage(),
                        continuationCommand(
                                ambassadorId
                        )
                );
            }

            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord targetCapital =
                audience.sourceCapital();

        if (!CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        targetCapital,
                        player.getUUID()
                )) {
            sendMessage(
                    player,
                    Component.translatable("mcacapitals.ui.asylum.title"),
                    Component.translatable("mcacapitals.ui.asylum.only_sovereign_or_hand"),
                    continuationCommand(
                            ambassadorId
                    )
            );

            return 0;
        }

        if (!CapitalBuildingService.hasInn(
                level,
                targetCapital
        )) {
            sendMessage(
                    player,
                    Component.translatable("mcacapitals.ui.asylum.title"),
                    Component.translatable("mcacapitals.ui.asylum.requires_inn"),
                    continuationCommand(
                            ambassadorId
                    )
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
                    Component.translatable("mcacapitals.ui.asylum.title"),
                    Component.translatable("mcacapitals.ui.asylum.village_unavailable"),
                    continuationCommand(
                            ambassadorId
                    )
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
                    Component.translatable("mcacapitals.ui.asylum.title"),
                    Component.translatable("mcacapitals.ui.asylum.none"),
                    continuationCommand(
                            ambassadorId
                    )
            );

            return 0;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        for (CapitalRefugeeRecord record : candidates) {
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
                            Component.literal(refugeeName),
                            Component.translatable(
                                    "mcacapitals.ui.asylum.exiled_from",
                                    storedCapitalNameComponent(record.getOriginCapitalName())
                            ),
                            Component.translatable("mcacapitals.ui.asylum.status_seeking"),
                            Component.empty(),
                            Component.translatable(
                                    "mcacapitals.ui.asylum.grant_to",
                                    Component.literal(refugeeName)
                            ),
                            "/capitalasylum grant "
                                    + ambassadorId
                                    + " "
                                    + record.getRefugeeId(),
                            true,
                            Component.empty()
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .ASYLUM_REQUESTS,
                        Component.translatable("mcacapitals.ui.asylum.title"),
                        CapitalDiplomaticAgreementText.capitalNameComponent(
                                level,
                                targetCapital
                        ),
                        Component.translatable("mcacapitals.ui.asylum.choose_refugee"),
                        continuationCommand(
                                ambassadorId
                        ),
                        entries,
                        List.of()
                )
        );

        return 1;
    }

    private static void sendMessage(
            ServerPlayer player,
            Component title,
            Component message,
            String backCommand
    ) {
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode
                                .MESSAGE,
                        title,
                        Component.empty(),
                        message,
                        backCommand,
                        List.of(),
                        List.of()
                )
        );
    }

    private static List<CapitalRefugeeRecord> findCandidates(
            ServerLevel level,
            CapitalRecord targetCapital
    ) {
        if (level == null
                || targetCapital == null
                || targetCapital.getCapitalId() == null) {
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
                                targetCapital,
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
            CapitalRecord targetCapital,
            CapitalRefugeeRecord record
    ) {
        Entity entity =
                MCAIntegrationBridge
                        .findLoadedMCAVillagerByUuid(
                                level,
                                record.getRefugeeId()
                        );

        if (!(entity instanceof VillagerEntityMCA villager)
                || !villager.isAlive()
                || !MCAIntegrationBridge
                .isTeenOrAdultVillager(
                        level,
                        record.getRefugeeId()
                )
                || !village.isWithinBorder(
                villager
        )) {
            return false;
        }

        Village currentHome =
                villager.getResidency()
                        .getHomeVillage()
                        .orElse(null);

        return currentHome == null
                || targetCapital.getVillageId() != null
                && currentHome.getId()
                == targetCapital.getVillageId();
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

    private static Component storedCapitalNameComponent(String name) {
        return name == null
                || name.isBlank()
                || "Unknown Capital".equals(name)
                ? Component.translatable("mcacapitals.system.common.unknown_capital")
                : Component.literal(name);
    }

    private static String continuationCommand(
            UUID ambassadorId
    ) {
        return ambassadorId == null
                ? ""
                : "/capitalurgent continue "
                + ambassadorId;
    }
}