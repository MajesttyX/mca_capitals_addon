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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

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

        Entity refugee =
                MCAIntegrationBridge.findLoadedMCAVillagerByUuid(
                        level,
                        refugeeId
                );

        if (refugee == null
                || !refugee.isAlive()
                || refugee.isRemoved()) {
            return false;
        }

        VillagerIdentityService.ensureAssigned(
                level,
                refugee,
                originCapital
        );

        removeTrustedOffices(
                level,
                refugeeId
        );

        if (!MCAIntegrationBridge.leaveHome(
                level,
                refugeeId
        )) {
            return false;
        }

        String originName =
                CapitalDiplomaticAgreementText.capitalName(
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

        CapitalJusticeDataAccess.markDiscoveredExile(
                level,
                originCapital.getCapitalId(),
                refugeeId
        );

        CapitalResidentScanner.clearCache(
                level
        );

        CapitalDataAccess.markDirty(
                level
        );

        VillagerIdentitySyncService.syncToNearbyPlayers(
                level,
                refugee
        );

        return true;
    }

    public static int grantAsylum(
            ServerPlayer player,
            UUID ambassadorId,
            UUID refugeeId
    ) {
        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateAudience(
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

        Entity entity =
                MCAIntegrationBridge.findLoadedMCAVillagerByUuid(
                        level,
                        refugeeId
                );

        if (!(entity instanceof VillagerEntityMCA villager)
                || !villager.isAlive()
                || villager.isRemoved()
                || !MCAIntegrationBridge.isTeenOrAdultVillager(
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

        if (!targetVillage.isWithinBorder(
                villager
        )) {
            player.sendSystemMessage(
                    Component.literal(
                            "The refugee must be inside the capital before asylum can be granted."
                    )
            );

            return 0;
        }

        Village currentHome =
                villager.getResidency()
                        .getHomeVillage()
                        .orElse(null);

        boolean alreadyResidentOfTarget =
                isTargetVillage(
                        currentHome,
                        targetCapital
                );

        if (currentHome != null
                && !alreadyResidentOfTarget) {
            player.sendSystemMessage(
                    Component.literal(
                            "That refugee currently belongs to a different MCA village."
                    )
            );

            return 0;
        }

        boolean assignedHomeHere = false;

        if (!alreadyResidentOfTarget) {
            if (!targetVillage.hasSpace()) {
                player.sendSystemMessage(
                        Component.literal(
                                "The capital has no free MCA residence capacity for this refugee."
                        )
                );

                return 0;
            }

            if (!MCAIntegrationBridge.forceVillageResidency(
                    level,
                    refugeeId,
                    targetCapital.getVillageId()
            )) {
                MCAIntegrationBridge.leaveHome(
                        level,
                        refugeeId
                );

                player.sendSystemMessage(
                        Component.literal(
                                "The refugee could not be assigned to this capital's MCA village."
                        )
                );

                return 0;
            }

            boolean joinedTarget =
                    villager.getResidency()
                            .getHomeVillage()
                            .map(
                                    home ->
                                            isTargetVillage(
                                                    home,
                                                    targetCapital
                                            )
                            )
                            .orElse(false);

            if (!joinedTarget) {
                MCAIntegrationBridge.leaveHome(
                        level,
                        refugeeId
                );

                player.sendSystemMessage(
                        Component.literal(
                                "The refugee could not be assigned to this capital's MCA village."
                        )
                );

                return 0;
            }

            assignedHomeHere = true;
        }

        removeTrustedOffices(
                level,
                refugeeId
        );

        if (!CapitalRefugeeDataAccess.grantAsylum(
                level,
                refugeeId,
                targetCapital.getCapitalId()
        )) {
            if (assignedHomeHere) {
                MCAIntegrationBridge.leaveHome(
                        level,
                        refugeeId
                );
            }

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

        CapitalResidentScanner.clearCache(
                level
        );

        CapitalDataAccess.markDirty(
                level
        );

        CapitalNameService.refreshCapitalNames(
                level,
                targetCapital,
                CapitalResidentScanner.scanResidents(
                        level,
                        targetCapital.getCapitalId()
                )
        );

        VillagerIdentitySyncService.syncToNearbyPlayers(
                level,
                villager
        );

        String refugeeName =
                villager.getName().getString();

        String targetName =
                CapitalDiplomaticAgreementText.capitalName(
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

        applyOriginCapitalConsequences(
                level,
                targetCapital,
                record,
                refugeeId,
                refugeeName,
                targetName
        );

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

        return record.isAwaitingAsylum()
                ? "Exiled From "
                + record.getOriginCapitalName()
                : "Refugee from "
                + record.getOriginCapitalName();
    }

    private static void applyOriginCapitalConsequences(
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
                CapitalJusticeDataAccess.hasDiscoveredExile(
                        level,
                        originCapital.getCapitalId(),
                        refugeeId
                );

        CapitalPublicCrownStatus originStatus =
                CapitalJusticeDataAccess.getPublicStatus(
                        level,
                        originCapital.getCapitalId(),
                        refugeeId
                );

        if (discoveredExile
                && originStatus
                != CapitalPublicCrownStatus.DISCOVERED_ENEMY) {
            CapitalJusticeDataAccess.setPublicStatus(
                    level,
                    originCapital.getCapitalId(),
                    refugeeId,
                    CapitalPublicCrownStatus.DISCOVERED_ENEMY
            );

            originStatus =
                    CapitalPublicCrownStatus.DISCOVERED_ENEMY;
        }

        boolean recognizedEnemy =
                originStatus
                        == CapitalPublicCrownStatus.DISCOVERED_ENEMY;

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

        if (originCapital.getPlayerSovereignId() != null) {
            CapitalDiplomaticAgreementCorrespondenceService.sendNotice(
                    level,
                    originCapital.getPlayerSovereignId(),
                    "Asylum Granted",
                    targetName
                            + " granted asylum to "
                            + refugeeName
                            + ", an exile from "
                            + record.getOriginCapitalName()
                            + "."
            );
        }
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

    private static boolean isTargetVillage(
            Village village,
            CapitalRecord targetCapital
    ) {
        return village != null
                && targetCapital != null
                && targetCapital.getVillageId() != null
                && village.getId()
                == targetCapital.getVillageId();
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
                CapitalManager.getAllCapitalRecords()) {
            if (capital == null
                    || capital.getCapitalId() == null) {
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
                    CapitalAmbassadorService.getAmbassador(
                            level,
                            capital
                    )
            )) {
                CapitalDiplomacyDataAccess.clearAmbassador(
                        level,
                        capital.getCapitalId()
                );
                changed = true;
            }

            if (changed) {
                CapitalCourtWatcher.clearFingerprint(
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