package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCrownJusticeService;
import com.majesttyx.mcacapitals.capital.CapitalCrownStandingService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalPlayerNotificationService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CrownStanding;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalPublicCrownStatus;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAccusationSelectionPacket;
import com.majesttyx.mcacapitals.util.CapitalJusticeText;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CapitalJusticeService {

    private static final long ACCUSATION_COOLDOWN_DAYS = 2L;

    private CapitalJusticeService() {
    }

    public static boolean openAccusationSelection(ServerPlayer player, Entity masterOfLawsEntity) {
        if (player == null || masterOfLawsEntity == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, masterOfLawsEntity.getUUID());
        if (!isMasterOfLaws(capital, masterOfLawsEntity.getUUID())) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_justice_service.only_the_master_of_laws_may_receive_accusations_against_the_crown"));
            MCAIntegrationBridge.stopInteracting(masterOfLawsEntity);
            return true;
        }

        long remainingDays = getRemainingCooldownDays(level, capital, player.getUUID());
        if (remainingDays > 0L) {
            player.sendSystemMessage(CapitalDialogueSpeaker.formatVillagerSpeech(
                    masterOfLawsEntity,
                    CapitalJusticeText.accusationCooldown()
            ));
            MCAIntegrationBridge.stopInteracting(masterOfLawsEntity);
            return true;
        }

        List<OpenAccusationSelectionPacket.Candidate> candidates = buildCandidates(level, capital);
        if (candidates.isEmpty()) {
            player.sendSystemMessage(CapitalDialogueSpeaker.formatVillagerSpeech(
                    masterOfLawsEntity,
                    Component.translatable("mcacapitals.justice.accusation.no_candidates")
            ));
            MCAIntegrationBridge.stopInteracting(masterOfLawsEntity);
            return true;
        }

        player.sendSystemMessage(CapitalDialogueSpeaker.formatVillagerSpeech(
                masterOfLawsEntity,
                CapitalJusticeText.accusationIntro()
        ));

        String villageName = capital.getVillageId() == null ? "" : MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        if (villageName == null) {
            villageName = "";
        }

        ModNetwork.sendToPlayer(player, new OpenAccusationSelectionPacket(capital.getCapitalId(), villageName, candidates));
        MCAIntegrationBridge.stopInteracting(masterOfLawsEntity);
        return true;
    }

    public static boolean handleAccusation(ServerPlayer player, UUID capitalId, UUID targetId) {
        if (player == null || capitalId == null || targetId == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_justice_service.that_capital_no_longer_exists"));
            return false;
        }

        if (capital.getMasterOfLaws() == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_justice_service.this_capital_has_no_master_of_laws"));
            return false;
        }

        Entity master = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, capital.getMasterOfLaws());
        if (master == null || player.distanceToSqr(master) > 12.0D * 12.0D) {
            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.system.capital_justice_service.you_must_remain_near_the_master_of_laws_to_make_this_accusation"
            ));
            return false;
        }

        if (!CapitalPlayerNotificationService.isPlayerWithinCapital(level, capital, player)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_justice_service.you_must_be_within_the_capital_to_make_this_accusation"));
            return false;
        }

        long remainingDays = getRemainingCooldownDays(level, capital, player.getUUID());
        if (remainingDays > 0L) {
            player.sendSystemMessage(CapitalJusticeText.accusationCooldown());
            return false;
        }

        if (!isValidAccusationTarget(level, capital, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_justice_service.that_person_cannot_be_accused_before_the_master_of_laws"));
            return false;
        }

        long currentDay = currentDay(level);
        CapitalJusticeDataAccess.setLastAccusationDay(level, capital.getCapitalId(), player.getUUID(), currentDay);

        CrownStanding standing = CapitalCrownStandingService.getStanding(level, capital, targetId);
        String targetName = CapitalNameService.resolveDisplayName(level, capital, targetId);
        String playerName = player.getName().getString();

        if (standing == CrownStanding.ENEMY_OF_CROWN) {
            rewardCorrectAccusation(player);
            CapitalCrownJusticeService.onCorrectAccusation(level, capital, targetId);

            boolean newWarrant = CapitalJusticeDataAccess.issueArrestWarrant(level, capital.getCapitalId(), targetId);
            Component warrantLine = CapitalJusticeText.arrestWarrantIssued(targetName);

            if (newWarrant) {
                CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.ARREST_WARRANT_ISSUED, targetName);
                player.sendSystemMessage(CapitalJusticeText.correctAccusation());
                player.sendSystemMessage(warrantLine);
            } else {
                CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.ACCUSATION_RENEWED, playerName, targetName);
                player.sendSystemMessage(CapitalJusticeText.correctAccusation());
                player.sendSystemMessage(Component.translatable("mcacapitals.justice.arrest_warrant.already_active", targetName));
            }

            CapitalDataAccess.markDirty(level);
            return true;
        }

        applyWrongAccusationPenalties(level, capital, targetId, player);
        CapitalChronicleService.addEvent(level, capital, CapitalChronicleEventId.FALSE_ACCUSATION, playerName, targetName);
        CapitalDataAccess.markDirty(level);

        player.sendSystemMessage(CapitalJusticeText.falseAccusation());
        return true;
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID masterOfLawsId) {
        if (masterOfLawsId == null) {
            return null;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && masterOfLawsId.equals(capital.getMasterOfLaws())) {
                return capital;
            }
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, masterOfLawsId);
        return CapitalManager.getCapitalByVillageId(villageId);
    }

    private static boolean isMasterOfLaws(CapitalRecord capital, UUID villagerId) {
        return capital != null && villagerId != null && villagerId.equals(capital.getMasterOfLaws());
    }

    private static List<OpenAccusationSelectionPacket.Candidate> buildCandidates(ServerLevel level, CapitalRecord capital) {
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        List<OpenAccusationSelectionPacket.Candidate> candidates = new ArrayList<>();

        for (UUID resident : residents) {
            if (!isValidAccusationTarget(level, capital, resident)) {
                continue;
            }

            candidates.add(new OpenAccusationSelectionPacket.Candidate(
                    resident,
                    CapitalNameService.resolveDisplayName(level, capital, resident)
            ));
        }

        candidates.sort(Comparator.comparing(OpenAccusationSelectionPacket.Candidate::name, String.CASE_INSENSITIVE_ORDER));
        return candidates;
    }

    private static boolean isValidAccusationTarget(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level == null || capital == null || targetId == null) {
            return false;
        }

        if (targetId.equals(capital.getMasterOfLaws())) {
            return false;
        }

        if (targetId.equals(capital.getSovereign()) || targetId.equals(capital.getPlayerSovereignId())) {
            return false;
        }

        CapitalPublicCrownStatus publicStatus = CapitalJusticeDataAccess.getPublicStatus(
                level,
                capital.getCapitalId(),
                targetId
        );
        if (publicStatus == CapitalPublicCrownStatus.DISCOVERED_ENEMY
                || publicStatus == CapitalPublicCrownStatus.RESTORED_TO_PEACE) {
            return false;
        }

        if (!MCAIntegrationBridge.isLoadedAndAlive(level, targetId)) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            return false;
        }

        String ageState = MCAIntegrationBridge.getAgeState(level, targetId);
        return !"BABY".equalsIgnoreCase(ageState) && !"TODDLER".equalsIgnoreCase(ageState);
    }

    private static void rewardCorrectAccusation(ServerPlayer player) {
        giveOrDrop(player, new ItemStack(Items.EMERALD_BLOCK, 2));
        giveOrDrop(player, new ItemStack(Items.DIAMOND, 3));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static void applyWrongAccusationPenalties(ServerLevel level, CapitalRecord capital, UUID targetId, ServerPlayer player) {
        boolean recognizedFriend = CapitalCrownJusticeService.isRecognizedFriend(level, capital, targetId);
        MCAIntegrationBridge.adjustHearts(level, targetId, player.getUUID(), recognizedFriend ? -75 : -50);

        UUID sovereign = capital.getSovereign();
        if (sovereign != null && !sovereign.equals(targetId) && MCAIntegrationBridge.isMCAVillager(level, sovereign)) {
            MCAIntegrationBridge.adjustHearts(level, sovereign, player.getUUID(), recognizedFriend ? -50 : -30);
        }
    }

    private static long getRemainingCooldownDays(ServerLevel level, CapitalRecord capital, UUID playerId) {
        long lastDay = CapitalJusticeDataAccess.getLastAccusationDay(level, capital.getCapitalId(), playerId);
        if (lastDay == Long.MIN_VALUE) {
            return 0L;
        }

        long elapsed = currentDay(level) - lastDay;
        if (elapsed >= ACCUSATION_COOLDOWN_DAYS) {
            return 0L;
        }

        return ACCUSATION_COOLDOWN_DAYS - elapsed;
    }

    private static long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }
}