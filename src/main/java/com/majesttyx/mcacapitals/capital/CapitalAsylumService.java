package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalPublicCrownStatus;
import com.majesttyx.mcacapitals.data.CapitalRefugeeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRefugeeRecord;
import com.majesttyx.mcacapitals.data.CapitalWarCause;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import com.majesttyx.mcacapitals.identity.VillagerIdentityService;
import com.majesttyx.mcacapitals.identity.VillagerIdentitySyncService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CapitalAsylumService {

    private CapitalAsylumService() {
    }

    public static boolean markExiled(
            ServerLevel level,
            CapitalRecord originCapital,
            UUID refugeeId
    ) {
        if (level == null
                || originCapital == null
                || originCapital.getCapitalId() == null
                || originCapital.getVillageId() == null
                || refugeeId == null) {
            return false;
        }

        Entity entity =
                MCAIntegrationBridge
                        .findLoadedMCAVillagerByUuid(
                                level,
                                refugeeId
                        );

        if (!(entity
                instanceof VillagerEntityMCA villager)
                || !villager.isAlive()) {
            return false;
        }

        VillagerIdentityService.ensureAssigned(
                level,
                villager,
                originCapital
        );

        villager.getResidency().leaveHome();

        String originName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                originCapital
                        );

        CapitalRefugeeRecord record =
                CapitalRefugeeDataAccess.markExiled(
                        level,
                        refugeeId,
                        originCapital.getCapitalId(),
                        originCapital.getVillageId(),
                        originName
                );

        if (record == null) {
            return false;
        }

        CapitalJusticeDataAccess
                .markDiscoveredExile(
                        level,
                        originCapital.getCapitalId(),
                        refugeeId
                );

        VillagerIdentitySyncService
                .syncToNearbyPlayers(
                        level,
                        villager
                );

        return true;
    }

    public static void sendReviewOption(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        if (player == null
                || ambassadorEntity == null) {
            return;
        }

        CapitalDiplomaticAgreementValidation
                .AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorEntity.getUUID()
                        );

        if (!audience.valid()) {
            return;
        }

        if (findCandidates(
                player.serverLevel(),
                audience.sourceCapital()
        ).isEmpty()) {
            return;
        }

        String command =
                "/capitalasylum review "
                        + ambassadorEntity.getUUID();

        player.sendSystemMessage(
                Component.literal(
                                "[Review Asylum Requests]"
                        )
                        .setStyle(
                                Style.EMPTY
                                        .withColor(
                                                ChatFormatting.AQUA
                                        )
                                        .withBold(true)
                                        .withClickEvent(
                                                new ClickEvent(
                                                        ClickEvent.Action
                                                                .RUN_COMMAND,
                                                        command
                                                )
                                        )
                                        .withHoverEvent(
                                                new HoverEvent(
                                                        HoverEvent.Action
                                                                .SHOW_TEXT,
                                                        Component.literal(
                                                                "Review refugees currently seeking asylum in this capital."
                                                        )
                                                )
                                        )
                        )
        );
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
                player.sendSystemMessage(
                        Component.literal(
                                audience.failureMessage()
                        )
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
            player.sendSystemMessage(
                    Component.literal(
                            "The capital requires an operational Inn before refugees can request asylum."
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
            player.sendSystemMessage(
                    Component.literal(
                            "The capital's MCA village record is unavailable."
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
            player.sendSystemMessage(
                    Component.literal(
                            "No refugees are currently seeking asylum inside the capital."
                    )
            );

            return 0;
        }

        if (!targetVillage.hasSpace()) {
            player.sendSystemMessage(
                    Component.literal(
                            "The capital has no free MCA residence capacity for a refugee."
                    )
            );

            return 0;
        }

        player.sendSystemMessage(
                Component.literal(
                        "Refugees currently seeking asylum:"
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );

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

            String command =
                    "/capitalasylum grant "
                            + ambassadorId
                            + " "
                            + record.getRefugeeId();

            player.sendSystemMessage(
                    Component.literal(
                                    "[Grant Asylum] "
                            )
                            .setStyle(
                                    Style.EMPTY
                                            .withColor(
                                                    ChatFormatting
                                                            .GREEN
                                            )
                                            .withBold(true)
                                            .withClickEvent(
                                                    new ClickEvent(
                                                            ClickEvent.Action
                                                                    .RUN_COMMAND,
                                                            command
                                                    )
                                            )
                                            .withHoverEvent(
                                                    new HoverEvent(
                                                            HoverEvent.Action
                                                                    .SHOW_TEXT,
                                                            Component.literal(
                                                                    "Admit this refugee as a resident of the capital."
                                                            )
                                                    )
                                            )
                            )
                            .append(
                                    Component.literal(
                                            refugee.getName()
                                                    .getString()
                                                    + " — Exiled From "
                                                    + record
                                                    .getOriginCapitalName()
                                    ).withStyle(
                                            ChatFormatting.GRAY
                                    )
                            )
            );
        }

        return 1;
    }

    public static int grantAsylum(
            ServerPlayer player,
            UUID ambassadorId,
            UUID refugeeId
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
                player.sendSystemMessage(
                        Component.literal(
                                audience.failureMessage()
                        )
                );
            }

            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        CapitalRecord targetCapital =
                audience.sourceCapital();

        CapitalRefugeeRecord record =
                CapitalRefugeeDataAccess.getRecord(
                        level,
                        refugeeId
                );

        if (record == null
                || !record.isAwaitingAsylum()) {
            player.sendSystemMessage(
                    Component.literal(
                            "That villager is not awaiting asylum."
                    )
            );

            return 0;
        }

        if (record.getOriginCapitalId().equals(
                targetCapital.getCapitalId()
        )) {
            player.sendSystemMessage(
                    Component.literal(
                            "A capital cannot grant foreign asylum to its own exile."
                    )
            );

            return 0;
        }

        if (!CapitalBuildingService.hasInn(
                level,
                targetCapital
        )) {
            player.sendSystemMessage(
                    Component.literal(
                            "The capital requires an operational Inn before asylum can be granted."
                    )
            );

            return 0;
        }

        Village village =
                getVillage(
                        level,
                        targetCapital
                );

        if (village == null
                || !village.hasSpace()) {
            player.sendSystemMessage(
                    Component.literal(
                            "The capital has no free MCA residence capacity for this refugee."
                    )
            );

            return 0;
        }

        Entity entity =
                MCAIntegrationBridge
                        .findLoadedMCAVillagerByUuid(
                                level,
                                refugeeId
                        );

        if (!(entity
                instanceof VillagerEntityMCA villager)
                || !villager.isAlive()
                || !MCAIntegrationBridge
                .isTeenOrAdultVillager(
                        level,
                        refugeeId
                )) {
            player.sendSystemMessage(
                    Component.literal(
                            "The refugee must be present and able to enter the capital."
                    )
            );

            return 0;
        }

        if (!village.isWithinBorder(villager)) {
            player.sendSystemMessage(
                    Component.literal(
                            "The refugee must be inside the capital before asylum can be granted."
                    )
            );

            return 0;
        }

        if (villager.getResidency()
                .getHomeVillage()
                .isPresent()) {
            player.sendSystemMessage(
                    Component.literal(
                            "That villager already belongs to an MCA village."
                    )
            );

            return 0;
        }

        villager.getResidency().seekHome();

        boolean joinedTarget =
                villager.getResidency()
                        .getHomeVillage()
                        .map(home ->
                                home.getId()
                                        == targetCapital
                                        .getVillageId()
                        )
                        .orElse(false);

        if (!joinedTarget) {
            villager.getResidency().leaveHome();

            player.sendSystemMessage(
                    Component.literal(
                            "The refugee could not be assigned to this capital's MCA village."
                    )
            );

            return 0;
        }

        if (!CapitalRefugeeDataAccess
                .grantAsylum(
                        level,
                        refugeeId,
                        targetCapital.getCapitalId()
                )) {
            villager.getResidency().leaveHome();

            player.sendSystemMessage(
                    Component.literal(
                            "The asylum record could not be saved."
                    )
            );

            return 0;
        }

        targetCapital.setCrownStanding(
                refugeeId,
                CrownStanding.FRIEND_OF_CROWN
        );
        CapitalJusticeDataAccess.setPublicStatus(
                level,
                targetCapital.getCapitalId(),
                refugeeId,
                CapitalPublicCrownStatus.RECOGNIZED_FRIEND
        );

        CapitalDataAccess.markDirty(level);

        VillagerIdentitySyncService
                .syncToNearbyPlayers(
                        level,
                        villager
                );

        String refugeeName =
                villager.getName().getString();

        String targetName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                targetCapital
                        );

        CapitalChronicleService.addEntry(
                level,
                targetCapital,
                refugeeName
                        + ", exiled from "
                        + record.getOriginCapitalName()
                        + ", was granted asylum in "
                        + targetName
                        + "."
        );

        CapitalRecord originCapital =
                CapitalManager.getCapital(
                        record.getOriginCapitalId()
                );

        if (originCapital != null) {
            boolean recognizedEnemy = CapitalJusticeDataAccess.getPublicStatus(
                    level,
                    originCapital.getCapitalId(),
                    refugeeId
            ) == CapitalPublicCrownStatus.DISCOVERED_ENEMY;
            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    originCapital.getCapitalId(),
                    targetCapital.getCapitalId(),
                    recognizedEnemy ? -45 : -30,
                    recognizedEnemy
                            ? "Asylum granted to a recognized Enemy of the Crown"
                            : "Asylum granted to a foreign exile",
                    targetCapital.getCapitalId()
            );
            CapitalWarDataAccess.recordGrievance(
                    level,
                    originCapital.getCapitalId(),
                    targetCapital.getCapitalId(),
                    recognizedEnemy
                            ? CapitalWarCause.SERIOUS_ASYLUM_DISPUTE
                            : CapitalWarCause.ASYLUM_DISPUTE,
                    10L
            );

            CapitalChronicleService.addEntry(
                    level,
                    originCapital,
                    targetName
                            + " granted asylum to the exile "
                            + refugeeName
                            + "."
            );

            if (originCapital
                    .getPlayerSovereignId()
                    != null) {
                CapitalDiplomaticAgreementCorrespondenceService
                        .sendNotice(
                                level,
                                originCapital
                                        .getPlayerSovereignId(),
                                "Asylum Granted",
                                targetName
                                        + " granted asylum to "
                                        + refugeeName
                                        + ", an exile from "
                                        + record
                                        .getOriginCapitalName()
                                        + "."
                        );
            }
        }

        player.sendSystemMessage(
                Component.literal(
                        refugeeName
                                + " has been granted asylum and is now an MCA resident of "
                                + targetName
                                + "."
                )
        );

        return 1;
    }

    public static String getStatusLine(
            ServerLevel level,
            UUID villagerId
    ) {
        CapitalRefugeeRecord record =
                CapitalRefugeeDataAccess.getRecord(
                        level,
                        villagerId
                );

        if (record == null) {
            return "";
        }

        if (record.isAwaitingAsylum()) {
            return "Exiled From "
                    + record.getOriginCapitalName();
        }

        return "Refugee from "
                + record.getOriginCapitalName();
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
                .filter(record -> record != null)
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