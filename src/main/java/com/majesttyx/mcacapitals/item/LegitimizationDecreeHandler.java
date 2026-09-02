package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;
import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class LegitimizationDecreeHandler {
    private LegitimizationDecreeHandler() {
    }

    public static InteractionResult handleEntityInteract(Player player, Entity rawTarget, InteractionHand hand) {
        if (player == null || rawTarget == null || hand == null) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.LEGITIMIZATION_DECREE.get()) || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(rawTarget instanceof LivingEntity livingTarget)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player.level() instanceof ServerLevel level)) return InteractionResult.PASS;

        UUID targetId = livingTarget.getUUID();
        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.legitimization_may_only_be_granted_to_an_mca_villager"));
            return InteractionResult.FAIL;
        }
        CapitalRecord capital = resolveCapital(level, targetId);
        if (capital == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.that_villager_has_no_claim_tied_to_any_capital"));
            return InteractionResult.FAIL;
        }
        if (capital.getSovereign() == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.that_capital_has_no_sovereign_to_grant_legitimacy"));
            return InteractionResult.FAIL;
        }
        if (targetId.equals(capital.getSovereign()) || targetId.equals(capital.getConsort()) || targetId.equals(capital.getDowager())) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.that_title_cannot_be_granted_through_legitimization"));
            return InteractionResult.FAIL;
        }
        if (!isEligibleDynasticChild(level, capital, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.legitimization_decree_handler.that_villager_is_not_recognized_as_a_child_of_this_dynasty"));
            return InteractionResult.FAIL;
        }

        boolean female = MCAIntegrationBridge.isFemale(level, targetId);
        capital.addLegitimizedRoyalChild(targetId, female);
        if (!capital.getRoyalSuccessionOrder().contains(targetId)) capital.getRoyalSuccessionOrder().add(targetId);
        CapitalFoundationService.refreshCourt(level, capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String displayName = CapitalChronicleIdentitySnapshot.name(level, capital, targetId);
        Component title = Component.translatable(female
                ? "mcacapitals.dynamic.title.royal_child.female"
                : "mcacapitals.dynamic.title.royal_child.male");
        CapitalChronicleService.addEvent(
                level, capital, CapitalChronicleEventId.LEGITIMIZED,
                displayName, CapitalChronicleIdentitySnapshot.title(level, capital, targetId),
                MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
        );
        player.sendSystemMessage(Component.translatable(
                "mcacapitals.system.legitimization_decree_handler.success",
                displayNameComponent(displayName), title
        ));
        return InteractionResult.SUCCESS;
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID targetId) {
        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, targetId);
        if (villageId != null) {
            CapitalRecord byVillage = CapitalManager.getCapitalByVillageId(level, villageId);
            if (byVillage != null) return byVillage;
        }
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital.isRoyalChild(targetId)
                    || capital.isLegitimizedRoyalChild(targetId)
                    || MCAIntegrationBridge.isChildOf(level, targetId, capital.getSovereign())
                    || (capital.getDowager() != null && MCAIntegrationBridge.isChildOf(level, targetId, capital.getDowager()))) {
                return capital;
            }
        }
        return null;
    }

    private static boolean isEligibleDynasticChild(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (capital == null || capital.getSovereign() == null || targetId == null) return false;
        if (capital.isRoyalChild(targetId) || capital.isLegitimizedRoyalChild(targetId)) return true;
        if (MCAIntegrationBridge.isChildOf(level, targetId, capital.getSovereign())) return true;
        return capital.getDowager() != null && MCAIntegrationBridge.isChildOf(level, targetId, capital.getDowager());
    }

    private static Component displayNameComponent(String displayName) {
        return displayName == null || displayName.isBlank()
                ? Component.translatable("mcacapitals.system.common.unnamed")
                : Component.literal(displayName);
    }
}
