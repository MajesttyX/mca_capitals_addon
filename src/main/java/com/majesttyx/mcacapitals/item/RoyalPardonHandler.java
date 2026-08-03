package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalPlayerNotificationService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAExecutionBridge;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class RoyalPardonHandler {

    private RoyalPardonHandler() {
    }

    public static InteractionResult handleEntityInteract(Player player, Entity target, InteractionHand hand) {
        if (player == null || target == null || hand == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.is(ModItems.ROYAL_PARDON.get())) {
            return InteractionResult.PASS;
        }
        if (!MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        handlePardon(level, serverPlayer, target, stack);
        return InteractionResult.SUCCESS;
    }

    private static void handlePardon(ServerLevel level, ServerPlayer player, Entity target, ItemStack stack) {
        UUID targetId = target.getUUID();
        CapitalRecord capital = resolveCapital(level, targetId);

        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            player.sendSystemMessage(Component.literal("This pardon has no lawful capital to answer to."));
            return;
        }
        if (!CapitalPlayerNotificationService.isPlayerWithinCapital(level, capital, player)) {
            player.sendSystemMessage(Component.literal("A Royal Pardon must be issued within the capital's bounds."));
            return;
        }
        if (!hasPardonAuthority(level, capital, player)) {
            player.sendSystemMessage(Component.literal("Only the player Sovereign, or the player Hand serving an NPC Sovereign, may issue a Royal Pardon."));
            return;
        }

        boolean hadArrestWarrant = CapitalJusticeDataAccess.hasArrestWarrant(
                level,
                capital.getCapitalId(),
                targetId
        );
        boolean hadDetention = CapitalJusticeDataAccess.isDetainedPrisoner(
                level,
                capital.getCapitalId(),
                targetId
        );
        boolean hadExecutionMark = MCAExecutionBridge.isMarkedForExecution(level, targetId);
        if (!hadArrestWarrant && !hadDetention && !hadExecutionMark) {
            player.sendSystemMessage(Component.literal(
                    target.getName().getString() + " has no warrant, detention, or execution mark to pardon."
            ));
            return;
        }

        boolean clearedArrest = CapitalJusticeDataAccess.clearArrestWarrant(
                level,
                capital.getCapitalId(),
                targetId
        );
        boolean clearedDetention = CapitalJusticeDataAccess.clearDetainedPrisoner(
                level,
                capital.getCapitalId(),
                targetId
        );
        boolean clearedExecution = MCAExecutionBridge.clearExecutionMark(level, targetId);
        if (!clearedArrest && !clearedDetention && !clearedExecution) {
            player.sendSystemMessage(Component.literal("The pardon could not be applied."));
            return;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        String targetName = CapitalNameService.resolveDisplayName(level, capital, targetId);
        String playerName = player.getName().getString();
        CapitalJusticeDataAccess.setLastResolvedDay(
                level,
                capital.getCapitalId(),
                targetId,
                currentDay(level)
        );
        CapitalChronicleService.addEntry(
                level,
                capital,
                targetName + " was granted a Royal Pardon by " + playerName
                        + ". The discovery remains part of the Crown's record."
        );
        CapitalDataAccess.markDirty(level);

        player.sendSystemMessage(Component.literal(targetName + " has been granted a Royal Pardon."));
    }

    private static boolean hasPardonAuthority(ServerLevel level, CapitalRecord capital, ServerPlayer player) {
        if (level == null || capital == null || player == null) {
            return false;
        }

        UUID playerId = player.getUUID();
        if (playerId.equals(capital.getPlayerSovereignId())) {
            return true;
        }
        return capital.getPlayerSovereignId() == null
                && capital.getSovereign() != null
                && playerId.equals(capital.getHand())
                && PlayerCapitalTitleService.isHand(level, capital, playerId);
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID targetId) {
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && capital.containsEntity(targetId)) {
                return capital;
            }
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, targetId);
        return CapitalManager.getCapitalByVillageId(villageId);
    }

    private static long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }
}
