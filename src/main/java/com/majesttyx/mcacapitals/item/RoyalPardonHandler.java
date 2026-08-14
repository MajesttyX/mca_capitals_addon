package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCrownJusticeService;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

public class RoyalPardonHandler {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (stack.isEmpty() || !stack.is(ModItems.ROYAL_PARDON.get())) {
            return;
        }

        Entity target = event.getTarget();
        if (target == null || !MCAIntegrationBridge.isMCAVillagerEntity(target)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        handlePardon(level, player, target, stack);
    }

    private void handlePardon(ServerLevel level, ServerPlayer player, Entity target, ItemStack stack) {
        UUID targetId = target.getUUID();
        CapitalRecord capital = resolveCapital(level, targetId);

        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.royal_pardon_handler.this_pardon_has_no_lawful_capital_to_answer_to"));
            return;
        }

        if (!CapitalPlayerNotificationService.isPlayerWithinCapital(level, capital, player)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.royal_pardon_handler.a_royal_pardon_must_be_issued_within_the_capital_s_bounds"));
            return;
        }

        if (!hasPardonAuthority(level, capital, player)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.royal_pardon_handler.only_the_player_sovereign_or_the_player_hand_serving_an_npc_sovereign"));
            return;
        }

        boolean hadArrestWarrant = CapitalJusticeDataAccess.hasArrestWarrant(level, capital.getCapitalId(), targetId);
        boolean hadDetention = CapitalJusticeDataAccess.isDetainedPrisoner(level, capital.getCapitalId(), targetId);
        boolean hadExecutionMark = MCAExecutionBridge.isMarkedForExecution(level, targetId);

        if (!hadArrestWarrant && !hadDetention && !hadExecutionMark) {
            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.justice.royal_pardon.no_active_case",
                    target.getName()
            ));
            return;
        }

        boolean clearedArrest = CapitalJusticeDataAccess.clearArrestWarrant(level, capital.getCapitalId(), targetId);
        boolean clearedDetention = CapitalJusticeDataAccess.clearDetainedPrisoner(level, capital.getCapitalId(), targetId);
        boolean clearedExecution = MCAExecutionBridge.clearExecutionMark(level, targetId);

        if (!clearedArrest && !clearedDetention && !clearedExecution) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.royal_pardon_handler.the_pardon_could_not_be_applied"));
            return;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        String targetName = CapitalNameService.resolveDisplayName(level, capital, targetId);
        String playerName = player.getName().getString();

        CapitalCrownJusticeService.recordPardonResolution(level, capital, targetId);
        CapitalChronicleService.addEvent(
                level,
                capital,
                CapitalChronicleEventId.ROYAL_PARDON,
                targetName,
                playerName
        );
        CapitalDataAccess.markDirty(level);

        player.sendSystemMessage(Component.translatable("mcacapitals.justice.royal_pardon.applied", targetName));
    }

    private boolean hasPardonAuthority(ServerLevel level, CapitalRecord capital, ServerPlayer player) {
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

    private CapitalRecord resolveCapital(ServerLevel level, UUID targetId) {
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && capital.containsEntity(targetId)) {
                return capital;
            }
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, targetId);
        return CapitalManager.getCapitalByVillageId(villageId);
    }
}