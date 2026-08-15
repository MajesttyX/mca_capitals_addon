package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
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

public class DeclarationOfAbdicationHandler {

    private static final String[] KNOWN_TITLES = new String[] {
            "High Queen",
            "High King",
            "Dowager Queen",
            "Dowager King",
            "Queen Consort",
            "King Consort",
            "Heir Apparent",
            "Crown Princess",
            "Crown Prince",
            "Princess Consort",
            "Prince Consort",
            "Princess",
            "Prince",
            "Duchess",
            "Duke",
            "Lady",
            "Lord",
            "Commander",
            "Dame",
            "Sir",
            "Queen",
            "King"
    };

    public static InteractionResult handleEntityInteract(Player player, Entity rawTarget, InteractionHand hand) {
        if (player == null || rawTarget == null || hand == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.DECLARATION_OF_ABDICATION.get())) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!(rawTarget instanceof LivingEntity livingTarget)) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        UUID targetId = livingTarget.getUUID();

        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_abdication_handler.the_declaration_of_abdication_can_only_be_used_on_an_mca_sovereign"));
            return InteractionResult.FAIL;
        }

        CapitalRecord capital = resolveCapital(level, targetId);
        if (capital == null) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_abdication_handler.that_villager_is_not_part_of_a_capital"));
            return InteractionResult.FAIL;
        }

        if (!targetId.equals(capital.getSovereign())) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_abdication_handler.only_the_current_sovereign_may_abdicate_the_throne"));
            return InteractionResult.FAIL;
        }

        boolean changed = CapitalFoundationService.abdicateSovereign(level, capital);
        if (!changed) {
            player.sendSystemMessage(Component.translatable("mcacapitals.system.declaration_of_abdication_handler.there_is_no_valid_successor_to_receive_the_throne"));
            return InteractionResult.FAIL;
        }

        String displayName = stripKnownTitles(livingTarget.getName().getString());
        player.sendSystemMessage(Component.translatable(
                "mcacapitals.system.declaration_of_abdication_handler.success",
                displayNameComponent(displayName)
        ));

        return InteractionResult.SUCCESS;
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID targetId) {
        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, targetId);
        if (villageId != null) {
            CapitalRecord byVillage = CapitalManager.getCapitalByVillageId(villageId);
            if (byVillage != null) {
                return byVillage;
            }
        }

        CapitalRecord bySovereign = CapitalManager.getCapitalBySovereign(targetId);
        if (bySovereign != null) {
            return bySovereign;
        }

        return null;
    }

    private static Component displayNameComponent(String displayName) {
        if (displayName == null || displayName.isBlank() || "Unnamed".equals(displayName)) {
            return Component.translatable("mcacapitals.system.common.unnamed");
        }
        return Component.literal(displayName);
    }

    private static String stripKnownTitles(String name) {
        if (name == null || name.isBlank()) {
            return "Unnamed";
        }

        String result = name.trim();

        for (String title : KNOWN_TITLES) {
            String prefix = title + " ";
            if (result.startsWith(prefix)) {
                return result.substring(prefix.length()).trim();
            }
        }

        return result;
    }
}