package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCrownJusticeService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalPlayerNotificationService;
import com.majesttyx.mcacapitals.capital.CapitalPlayerWarrantService;
import com.majesttyx.mcacapitals.capital.PlayerCapitalAllegianceService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalPlayerWarrantDataAccess;
import com.majesttyx.mcacapitals.dialogue.CapitalDialogueSpeaker;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenSealedPurseCaseSelectionPacket;
import com.majesttyx.mcacapitals.util.CapitalJusticeText;
import com.majesttyx.mcacapitals.util.MCAExecutionBridge;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SealedPurseHandler {

    private static final double BASE_SUCCESS_CHANCE = 25.0D;
    private static final double MAX_SUCCESS_CHANCE = 98.0D;
    private static final int MAX_SUCCESS_HEARTS = 200;
    private static final long PENDING_SELECTION_TICKS = 20L * 120L;

    private static final Map<UUID, PendingPurseGift> PENDING_GIFTS = new HashMap<>();

    public static boolean tryGiftSealedPurse(ServerPlayer player, Entity target, ItemStack giftedStack) {
        if (player == null || target == null || giftedStack == null || !giftedStack.is(ModItems.SEALED_PURSE.get())) {
            return false;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            return false;
        }

        return openGiftCaseSelection(level, player, target);
    }

    private static boolean openGiftCaseSelection(ServerLevel level, ServerPlayer player, Entity target) {
        CapitalRecord capital = resolveMasterOfLawsCapital(target.getUUID());
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.a_sealed_purse_must_be_gifted_to_the_master_of_laws"));
            MCAIntegrationBridge.stopInteracting(target);
            return true;
        }

        if (!CapitalPlayerNotificationService.isPlayerWithinCapital(level, capital, player)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.a_sealed_purse_must_be_gifted_within_the_capital_s_bounds"));
            MCAIntegrationBridge.stopInteracting(target);
            return true;
        }

        List<OpenSealedPurseCaseSelectionPacket.CaseEntry> cases = buildCases(level, capital);
        if (cases.isEmpty()) {
            player.sendSystemMessage(CapitalDialogueSpeaker.formatVillagerSpeech(
                    target,
                    CapitalJusticeText.sealedPurseNoCases(level, target.getUUID())
            ));
            MCAIntegrationBridge.stopInteracting(target);
            return true;
        }

        if (!consumeSealedPurse(player)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.you_need_a_sealed_purse_to_gift_the_master_of_laws"));
            MCAIntegrationBridge.stopInteracting(target);
            return true;
        }

        if (cases.size() == 1) {
            resolveGiftedCase(player, capital, cases.get(0).id());
            MCAIntegrationBridge.stopInteracting(target);
            return true;
        }

        PENDING_GIFTS.put(player.getUUID(), new PendingPurseGift(capital.getCapitalId(), level.getGameTime() + PENDING_SELECTION_TICKS));

        String villageName = capital.getVillageId() == null ? "" : MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        if (villageName == null) {
            villageName = "";
        }

        ModNetwork.sendToPlayer(player, new OpenSealedPurseCaseSelectionPacket(capital.getCapitalId(), villageName, cases));
        MCAIntegrationBridge.stopInteracting(target);
        return true;
    }

    public static boolean handleSelectedCase(ServerPlayer player, UUID capitalId, UUID targetId) {
        if (player == null || capitalId == null || targetId == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        PendingPurseGift pending = PENDING_GIFTS.get(player.getUUID());
        if (pending == null || !pending.capitalId().equals(capitalId) || level.getGameTime() > pending.expiresAtGameTime()) {
            PENDING_GIFTS.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.you_must_gift_the_sealed_purse_directly_to_the_master_of_laws"));
            return false;
        }

        PENDING_GIFTS.remove(player.getUUID());

        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.that_capital_no_longer_has_a_court_to_influence"));
            return false;
        }

        return resolveGiftedCase(player, capital, targetId);
    }

    private static boolean resolveGiftedCase(ServerPlayer player, CapitalRecord capital, UUID targetId) {
        if (player == null || capital == null || targetId == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();

        if (capital.getMasterOfLaws() == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.this_capital_has_no_master_of_laws_to_receive_the_sealed_purse"));
            return false;
        }

        Entity masterOfLaws = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, capital.getMasterOfLaws());
        if (masterOfLaws == null || masterOfLaws.distanceToSqr(player) > 12.0D * 12.0D) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.you_must_remain_near_the_master_of_laws_while_choosing_the_matter"));
            return false;
        }

        if (!CapitalPlayerNotificationService.isPlayerWithinCapital(level, capital, player)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.a_sealed_purse_must_be_gifted_within_the_capital_s_bounds"));
            return false;
        }

        if (!hasActiveCase(level, capital, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.that_matter_is_no_longer_active"));
            return false;
        }

        if (MCAExecutionBridge.isMarkedForExecution(level, targetId)) {
            player.sendSystemMessage(CapitalJusticeText.sealedPurseFormalExecution(level, targetId));
            return false;
        }

        int hearts = MCAIntegrationBridge.getHeartsWithPlayer(level, capital.getMasterOfLaws(), player.getUUID());
        double chance = calculateSuccessChance(hearts);
        boolean success = level.random.nextDouble() * 100.0D < chance;
        String targetName = CapitalNameService.resolveDisplayName(level, capital, targetId);
        CapitalRecord declaredCapital = PlayerCapitalAllegianceService.getDeclaredCapital(
                level,
                player.getUUID()
        );
        boolean foreign = declaredCapital != null
                && !declaredCapital.getCapitalId().equals(capital.getCapitalId());
        String foreignCaseKey = foreign
                ? buildForeignCaseKey(level, capital, targetId)
                : null;

        if (success) {
            boolean cleared = CapitalJusticeDataAccess.clearJusticeCase(level, capital.getCapitalId(), targetId);
            if (!cleared) {
                player.sendSystemMessage(Component.translatable("mcacapitals.system.sealed_purse_handler.the_sealed_purse_was_accepted_but_the_court_records_were_already_chang"));
                return true;
            }

            CapitalCrownJusticeService.recordPardonResolution(level, capital, targetId);
            Component line = CapitalJusticeText.sealedPurseSuccess(level, targetId, targetName);
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.COURT_RECORD_REMOVED,
                    targetName
            );
            CapitalDataAccess.markDirty(level);
            if (foreign && CapitalPlayerWarrantDataAccess.markCasePenalized(level, foreignCaseKey)) {
                CapitalDiplomacyDataAccess.adjustRelationship(
                        level,
                        declaredCapital.getCapitalId(),
                        capital.getCapitalId(),
                        -15,
                        "mcacapitals.relationship_reason.foreign_legal_bribe_success",
                        declaredCapital.getCapitalId()
                );
            }
            player.sendSystemMessage(line);
            return true;
        }

        if (foreign && CapitalPlayerWarrantDataAccess.markCasePenalized(level, foreignCaseKey)) {
            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    declaredCapital.getCapitalId(),
                    capital.getCapitalId(),
                    -20,
                    "mcacapitals.relationship_reason.foreign_legal_bribe_discovered",
                    declaredCapital.getCapitalId()
            );
            CapitalPlayerWarrantService.orderToLeave(
                    player,
                    capital,
                    foreignCaseKey
            );
        }
        player.sendSystemMessage(CapitalJusticeText.sealedPurseFailure(level, targetId));
        return true;
    }

    private static Component buildCaseStatusComponent(Set<String> statuses) {
        MutableComponent result = Component.empty();
        boolean first = true;

        for (String status : statuses) {
            if (!first) {
                result.append(Component.literal(" / "));
            }

            result.append(
                    Component.translatable(
                            "mcacapitals.ui.sealed_purse.case_status." + status
                    )
            );
            first = false;
        }

        return result;
    }

    private static String buildForeignCaseKey(
            ServerLevel level,
            CapitalRecord capital,
            UUID targetId
    ) {
        return capital.getCapitalId()
                + "|" + targetId
                + "|" + CapitalJusticeDataAccess.getArrestWarrantIssuedGameTime(
                level,
                capital.getCapitalId(),
                targetId
        )
                + "|" + CapitalJusticeDataAccess.getDetentionStartDay(
                level,
                capital.getCapitalId(),
                targetId
        )
                + "|" + CapitalJusticeDataAccess.getConfirmedCaseCount(
                level,
                capital.getCapitalId(),
                targetId
        )
                + "|" + CapitalJusticeDataAccess.getPublicStatus(
                level,
                capital.getCapitalId(),
                targetId
        ).name();
    }

    private static List<OpenSealedPurseCaseSelectionPacket.CaseEntry> buildCases(ServerLevel level, CapitalRecord capital) {
        Map<UUID, Set<String>> statusesByTarget = new LinkedHashMap<>();

        for (UUID targetId : CapitalJusticeDataAccess.getArrestWarrants(level, capital.getCapitalId())) {
            statusesByTarget.computeIfAbsent(targetId, ignored -> new LinkedHashSet<>()).add("arrest_warrant");
        }

        for (UUID targetId : CapitalJusticeDataAccess.getDetainedPrisoners(level, capital.getCapitalId())) {
            statusesByTarget.computeIfAbsent(targetId, ignored -> new LinkedHashSet<>()).add("in_custody");
        }


        List<OpenSealedPurseCaseSelectionPacket.CaseEntry> cases = new ArrayList<>();
        for (Map.Entry<UUID, Set<String>> entry : statusesByTarget.entrySet()) {
            UUID targetId = entry.getKey();
            if (MCAExecutionBridge.isMarkedForExecution(level, targetId)) {
                continue;
            }

            String name = CapitalNameService.resolveDisplayName(level, capital, targetId);
            Component status = buildCaseStatusComponent(entry.getValue());
            cases.add(new OpenSealedPurseCaseSelectionPacket.CaseEntry(targetId, name, status));
        }

        cases.sort((first, second) -> first.name().compareToIgnoreCase(second.name()));
        return cases;
    }

    private static boolean hasActiveCase(ServerLevel level, CapitalRecord capital, UUID targetId) {
        return CapitalJusticeDataAccess.hasArrestWarrant(level, capital.getCapitalId(), targetId)
                || CapitalJusticeDataAccess.isDetainedPrisoner(level, capital.getCapitalId(), targetId);
    }

    private static CapitalRecord resolveMasterOfLawsCapital(UUID masterOfLawsId) {
        if (masterOfLawsId == null) {
            return null;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && masterOfLawsId.equals(capital.getMasterOfLaws())) {
                return capital;
            }
        }

        return null;
    }

    private static double calculateSuccessChance(int hearts) {
        int clampedHearts = Math.max(0, Math.min(MAX_SUCCESS_HEARTS, hearts));
        double progress = clampedHearts / (double) MAX_SUCCESS_HEARTS;
        return BASE_SUCCESS_CHANCE + ((MAX_SUCCESS_CHANCE - BASE_SUCCESS_CHANCE) * progress);
    }

    private static boolean consumeSealedPurse(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.is(ModItems.SEALED_PURSE.get())) {
            mainHand.shrink(1);
            return true;
        }

        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.is(ModItems.SEALED_PURSE.get())) {
            offHand.shrink(1);
            return true;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(ModItems.SEALED_PURSE.get())) {
                stack.shrink(1);
                return true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.is(ModItems.SEALED_PURSE.get())) {
                stack.shrink(1);
                return true;
            }
        }

        return false;
    }

    private record PendingPurseGift(UUID capitalId, long expiresAtGameTime) {
    }
}
