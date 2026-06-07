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
            player.sendSystemMessage(Component.literal("The Declaration of Abdication can only be used on an MCA sovereign."));
            return InteractionResult.FAIL;
        }

        CapitalRecord capital = resolveCapital(level, targetId);
        if (capital == null) {
            player.sendSystemMessage(Component.literal("That villager is not part of a capital."));
            return InteractionResult.FAIL;
        }

        if (!targetId.equals(capital.getSovereign())) {
            player.sendSystemMessage(Component.literal("Only the current sovereign may abdicate the throne."));
            return InteractionResult.FAIL;
        }

        boolean changed = CapitalFoundationService.abdicateSovereign(level, capital);
        if (!changed) {
            player.sendSystemMessage(Component.literal("There is no valid successor to receive the throne."));
            return InteractionResult.FAIL;
        }

        String displayName = stripKnownTitles(livingTarget.getName().getString());
        player.sendSystemMessage(Component.literal(
                "By solemn declaration, " + displayName + " has abdicated the throne."
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