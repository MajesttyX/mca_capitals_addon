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
import fabric.net.conczin.mca.entity.VillagerEntityMCA;
import fabric.net.conczin.mca.server.world.data.Village;
import fabric.net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
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

        removeTrustedOffices(
                level,
                refugeeId
        );

        villager.getResidency().leaveHome();

        CapitalRefugeeRecord record =
                CapitalRefugeeDataAccess.markExiled(
                        level,
                        refugeeId,
                        originCapital.getCapitalId(),
                        originCapital.getVillageId(),
                        CapitalDiplomaticAgreementText
                                .capitalName(
                                        level,
                                        originCapital
                                )
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

        CapitalResidentScanner.clearCache(level);
        CapitalDataAccess.markDirty(level);

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

        if (!audience.valid()
                || findCandidates(
                player.serverLevel(),
                audience.sourceCapital()
        ).isEmpty()) {
            return;
        }

        player.sendSystemMessage(
                clickable(
                        Component.translatable(
                                "mcacapitals.ui.asylum.review_link"
                        ),
                        "/capitalasylum review "
                                + ambassadorEntity.getUUID(),
                        Component.translatable(
                                "mcacapitals.ui.asylum.review_hover"
                        ),
                        ChatFormatting.AQUA
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
                        audience.failureMessage()
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
                    Component.translatable("mcacapitals.system.capital_asylum_service.the_capital_requires_an_operational_inn_before_refugees_can_request_as")
            );

            return 0;
        }

        if (getVillage(
                level,
                targetCapital
        ) == null) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_asylum_service.the_capital_s_mca_village_record_is_unavailable")
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
                    Component.translatable("mcacapitals.system.capital_asylum_service.no_refugees_are_currently_seeking_asylum_inside_the_capital")
            );

            return 0;
        }

        player.sendSystemMessage(
                Component.translatable("mcacapitals.system.capital_asylum_service.refugees_currently_seeking_asylum").withStyle(
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

            MutableComponent line =
                    clickable(
                            Component.translatable(
                                    "mcacapitals.ui.asylum.grant_link"
                            ),
                            "/capitalasylum grant "
                                    + ambassadorId
                                    + " "
                                    + record.getRefugeeId(),
                            Component.translatable(
                                    "mcacapitals.ui.asylum.grant_hover"
                            ),
                            ChatFormatting.GREEN
                    ).append(
                            Component.translatable(
                                    "mcacapitals.ui.asylum.candidate_line",
                                    refugee.getName(),
                                    record.getOriginCapitalName()
                            ).withStyle(
                                    ChatFormatting.GRAY
                            )
                    );

            player.sendSystemMessage(line);
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
                        audience.failureMessage()
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
                    Component.translatable("mcacapitals.system.capital_asylum_service.that_villager_is_not_awaiting_asylum")
            );

            return 0;
        }

        if (record.getOriginCapitalId()
                .equals(
                        targetCapital.getCapitalId()
                )) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_asylum_service.a_capital_cannot_grant_foreign_asylum_to_its_own_exile")
            );

            return 0;
        }

        if (!CapitalBuildingService.hasInn(
                level,
                targetCapital
        )) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_asylum_service.the_capital_requires_an_operational_inn_before_asylum_can_be_granted")
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
                    Component.translatable("mcacapitals.system.capital_asylum_service.the_capital_s_mca_village_record_is_unavailable")
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
                    Component.translatable("mcacapitals.system.capital_asylum_service.the_refugee_must_be_present_and_able_to_enter_the_capital")
            );

            return 0;
        }

        if (!targetVillage.isWithinBorder(
                villager
        )) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_asylum_service.the_refugee_must_be_inside_the_capital_before_asylum_can_be_granted")
            );

            return 0;
        }

        Village currentHome =
                villager.getResidency()
                        .getHomeVillage()
                        .orElse(null);

        boolean alreadyResidentOfTarget =
                isTargetVillage(
                        level,
                        currentHome,
                        targetCapital
                );

        if (currentHome != null
                && !alreadyResidentOfTarget) {
            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_asylum_service.that_refugee_currently_belongs_to_a_different_mca_village")
            );

            return 0;
        }

        boolean assignedHomeHere = false;

        if (!alreadyResidentOfTarget) {
            if (!targetVillage.hasSpace()) {
                player.sendSystemMessage(
                        Component.translatable("mcacapitals.system.capital_asylum_service.the_capital_has_no_free_mca_residence_capacity_for_this_refugee")
                );

                return 0;
            }

            villager.getResidency().seekHome();

            boolean joinedTarget =
                    villager.getResidency()
                            .getHomeVillage()
                            .map(
                                    home ->
                                            isTargetVillage(
                                                    level,
                                                    home,
                                                    targetCapital
                                            )
                            )
                            .orElse(false);

            if (!joinedTarget) {
                villager.getResidency().leaveHome();

                player.sendSystemMessage(
                        Component.translatable("mcacapitals.system.capital_asylum_service.the_refugee_could_not_be_assigned_to_this_capital_s_mca_village")
                );

                return 0;
            }

            assignedHomeHere = true;
        }

        removeTrustedOffices(
                level,
                refugeeId
        );

        if (!CapitalRefugeeDataAccess
                .grantAsylum(
                        level,
                        refugeeId,
                        targetCapital.getCapitalId()
                )) {
            if (assignedHomeHere) {
                villager.getResidency()
                        .leaveHome();
            }

            player.sendSystemMessage(
                    Component.translatable("mcacapitals.system.capital_asylum_service.the_asylum_record_could_not_be_saved")
            );

            return 0;
        }

        targetCapital.setCrownStanding(
                refugeeId,
                CrownStanding.FRIEND_OF_CROWN
        );

        CapitalJusticeDataAccess
                .setPublicStatus(
                        level,
                        targetCapital.getCapitalId(),
                        refugeeId,
                        CapitalPublicCrownStatus
                                .RECOGNIZED_FRIEND
                );

        CapitalResidentScanner.clearCache(level);
        CapitalDataAccess.markDirty(level);

        CapitalNameService
                .refreshCapitalNames(
                        level,
                        targetCapital,
                        CapitalResidentScanner
                                .scanResidents(
                                        level,
                                        targetCapital
                                                .getCapitalId()
                                )
                );

        VillagerIdentitySyncService
                .syncToNearbyPlayers(
                        level,
                        villager
                );

        String refugeeName =
                villager.getName()
                        .getString();

        String targetName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                targetCapital
                        );

        CapitalChronicleService.addEvent(
                level,
                targetCapital,
                CapitalChronicleEventId.ASYLUM_GRANTED_DESTINATION,
                refugeeName,
                record.getOriginCapitalName(),
                targetName
        );

        applyOriginCapitalConsequences(
                level,
                targetCapital,
                record,
                refugeeId,
                refugeeName,
                targetName
        );

        player.sendSystemMessage(
                Component.translatable(
                        "mcacapitals.asylum.granted_resident",
                        refugeeName,
                        targetName
                )
        );

        return 1;
    }

    public static String getStatusLine(
            ServerLevel level,
            UUID villagerId
    ) {
        return getStatusComponent(
                level,
                villagerId
        ).getString();
    }

    public static Component getStatusComponent(
            ServerLevel level,
            UUID villagerId
    ) {
        CapitalRefugeeRecord record =
                CapitalRefugeeDataAccess.getRecord(
                        level,
                        villagerId
                );

        if (record == null) {
            return Component.empty();
        }

        return Component.translatable(
                record.isAwaitingAsylum()
                        ? "mcacapitals.ui.asylum.status_exiled_from"
                        : "mcacapitals.ui.asylum.status_refugee_from",
                record.getOriginCapitalName()
        );
    }

    private static void
    applyOriginCapitalConsequences(
            ServerLevel level,
            CapitalRecord targetCapital,
            CapitalRefugeeRecord record,
            UUID refugeeId,
            String refugeeName,
            String targetName
    ) {
        CapitalRecord originCapital =
                CapitalManager.getCapital(
                        record.getOriginCapitalId()
                );

        if (originCapital == null) {
            return;
        }

        boolean discoveredExile =
                CapitalJusticeDataAccess
                        .hasDiscoveredExile(
                                level,
                                originCapital
                                        .getCapitalId(),
                                refugeeId
                        );

        CapitalPublicCrownStatus originStatus =
                CapitalJusticeDataAccess
                        .getPublicStatus(
                                level,
                                originCapital
                                        .getCapitalId(),
                                refugeeId
                        );

        if (discoveredExile
                && originStatus
                != CapitalPublicCrownStatus
                .DISCOVERED_ENEMY) {
            CapitalJusticeDataAccess
                    .setPublicStatus(
                            level,
                            originCapital
                                    .getCapitalId(),
                            refugeeId,
                            CapitalPublicCrownStatus
                                    .DISCOVERED_ENEMY
                    );

            originStatus =
                    CapitalPublicCrownStatus
                            .DISCOVERED_ENEMY;
        }

        boolean recognizedEnemy =
                originStatus
                        == CapitalPublicCrownStatus
                        .DISCOVERED_ENEMY;

        CapitalDiplomacyDataAccess
                .adjustRelationship(
                        level,
                        originCapital.getCapitalId(),
                        targetCapital.getCapitalId(),
                        recognizedEnemy ? -45 : -30,
                        recognizedEnemy
                                ? "mcacapitals.relationship_reason.asylum_recognized_enemy"
                                : "mcacapitals.relationship_reason.asylum_foreign_exile",
                        targetCapital.getCapitalId()
                );

        CapitalWarDataAccess
                .recordGrievance(
                        level,
                        originCapital.getCapitalId(),
                        targetCapital.getCapitalId(),
                        recognizedEnemy
                                ? CapitalWarCause
                                .SERIOUS_ASYLUM_DISPUTE
                                : CapitalWarCause
                                .ASYLUM_DISPUTE,
                        10L
                );

        CapitalChronicleService.addEvent(
                level,
                originCapital,
                CapitalChronicleEventId.ASYLUM_GRANTED_ORIGIN,
                targetName,
                refugeeName
        );

        if (originCapital
                .getPlayerSovereignId()
                != null) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            originCapital
                                    .getPlayerSovereignId(),
                            Component.translatable(
                                    "mcacapitals.asylum.granted_title"
                            ),
                            Component.translatable(
                                    "mcacapitals.asylum.granted_origin_notice",
                                    targetName,
                                    refugeeName,
                                    record.getOriginCapitalName()
                            )
                    );
        }
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
                .filter(
                        record ->
                                record != null
                )
                .filter(
                        record ->
                                !record
                                        .getOriginCapitalId()
                                        .equals(
                                                targetCapital
                                                        .getCapitalId()
                                        )
                )
                .filter(
                        record ->
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

        if (!(entity
                instanceof VillagerEntityMCA villager)
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
                || isTargetVillage(
                level,
                currentHome,
                targetCapital
        );
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
                ? record.getRefugeeId()
                .toString()
                : entity.getName()
                .getString();
    }

    private static Village getVillage(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getVillageId()
                == null) {
            return null;
        }

        ServerLevel capitalLevel = CapitalManager.resolveCapitalLevel(level, capital);
        return VillageManager.get(capitalLevel)
                .getOrEmpty(
                        capital.getVillageId()
                )
                .orElse(null);
    }

    private static boolean isTargetVillage(
            ServerLevel level,
            Village village,
            CapitalRecord targetCapital
    ) {
        return village != null
                && targetCapital != null
                && targetCapital.getVillageId() != null
                && CapitalManager.isCapitalInLevel(targetCapital, level)
                && village.getId() == targetCapital.getVillageId();
    }

    private static MutableComponent clickable(
            Component label,
            String command,
            Component hover,
            ChatFormatting color
    ) {
        return label.copy()
                .setStyle(
                        Style.EMPTY
                                .withColor(color)
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
                                                hover
                                        )
                                )
                );
    }

    private static void removeTrustedOffices(
            ServerLevel level,
            UUID refugeeId
    ) {
        if (level == null
                || refugeeId == null) {
            return;
        }

        boolean anyChanged = false;

        for (CapitalRecord capital :
                CapitalManager
                        .getAllCapitalRecords()) {
            if (capital == null
                    || capital.getCapitalId()
                    == null) {
                continue;
            }

            boolean changed = false;

            if (refugeeId.equals(
                    capital.getHand()
            )) {
                capital.setHand(null);
                changed = true;
            }

            if (refugeeId.equals(
                    capital.getCommander()
            )) {
                capital.setCommander(null);
                capital.setCommanderFemale(false);
                changed = true;
            }

            if (refugeeId.equals(
                    capital.getHerald()
            )) {
                capital.setHerald(null);
                capital.setHeraldDisplayName("");
                changed = true;
            }

            if (refugeeId.equals(
                    capital.getGrandMaester()
            )) {
                capital.setGrandMaester(null);
                changed = true;
            }

            if (refugeeId.equals(
                    capital.getMasterOfLaws()
            )) {
                capital.setMasterOfLaws(null);
                changed = true;
            }

            if (capital.isRoyalGuard(
                    refugeeId
            )) {
                capital.removeRoyalGuard(
                        refugeeId
                );

                changed = true;
            }

            if (refugeeId.equals(
                    CapitalAmbassadorService
                            .getAmbassador(
                                    level,
                                    capital
                            )
            )) {
                CapitalDiplomacyDataAccess
                        .clearAmbassador(
                                level,
                                capital.getCapitalId()
                        );

                changed = true;
            }

            if (changed) {
                CapitalCourtWatcher
                        .clearFingerprint(
                                capital.getCapitalId()
                        );

                anyChanged = true;
            }
        }

        if (anyChanged) {
            CapitalResidentScanner.clearCache(
                    level
            );

            CapitalDataAccess.markDirty(
                    level
            );
        }
    }
}