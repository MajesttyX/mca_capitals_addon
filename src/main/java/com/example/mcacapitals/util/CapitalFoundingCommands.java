package com.example.mcacapitals.util;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.item.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;

public class CapitalFoundingCommands {

    private static final int CLAIM_HEARTS_REQUIRED = 2500;

    private CapitalFoundingCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("capitalfounding")
                        .then(Commands.literal("claimself")
                                .then(Commands.argument("capitalId", StringArgumentType.string())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                return 0;
                                            }

                                            UUID capitalId = UUID.fromString(StringArgumentType.getString(ctx, "capitalId"));
                                            CapitalRecord capital = CapitalManager.getCapital(capitalId);
                                            if (capital == null) {
                                                player.sendSystemMessage(Component.literal("That capital no longer exists."));
                                                return 0;
                                            }

                                            if (capital.getSovereign() != null) {
                                                player.sendSystemMessage(Component.literal("That capital already has a sovereign."));
                                                return 0;
                                            }

                                            Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(player.serverLevel(), capital.getVillageId());
                                            if (!MCAReputationBridge.canClaimThrone(player.serverLevel(), residents, player.getUUID(), CLAIM_HEARTS_REQUIRED)) {
                                                player.sendSystemMessage(Component.literal(
                                                        "You do not yet have the standing to claim the throne of "
                                                                + MCAIntegrationBridge.getVillageName(player.serverLevel(), capital.getVillageId()) + "."
                                                ));
                                                return 0;
                                            }

                                            capital.setMonarchyRejected(false);
                                            CapitalFoundationService.appointPlayerSovereign(player.serverLevel(), capital, player.getUUID(), false);
                                            consumeCharter(player, capitalId);

                                            player.sendSystemMessage(Component.literal(
                                                    "You have claimed the throne of "
                                                            + MCAIntegrationBridge.getVillageName(player.serverLevel(), capital.getVillageId()) + "."
                                            ));
                                            return 1;
                                        })))

                        .then(Commands.literal("appoint")
                                .then(Commands.argument("capitalId", StringArgumentType.string())
                                        .then(Commands.argument("villagerId", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                        return 0;
                                                    }

                                                    UUID capitalId = UUID.fromString(StringArgumentType.getString(ctx, "capitalId"));
                                                    UUID villagerId = UUID.fromString(StringArgumentType.getString(ctx, "villagerId"));

                                                    CapitalRecord capital = CapitalManager.getCapital(capitalId);
                                                    if (capital == null) {
                                                        player.sendSystemMessage(Component.literal("That capital no longer exists."));
                                                        return 0;
                                                    }

                                                    if (capital.getSovereign() != null) {
                                                        player.sendSystemMessage(Component.literal("That capital already has a sovereign."));
                                                        return 0;
                                                    }

                                                    if (!MCAIntegrationBridge.isMCAVillager(player.serverLevel(), villagerId)) {
                                                        player.sendSystemMessage(Component.literal("That choice is not a valid MCA villager."));
                                                        return 0;
                                                    }

                                                    Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(player.serverLevel(), capital.getVillageId());
                                                    if (!residents.contains(villagerId)) {
                                                        player.sendSystemMessage(Component.literal("That villager does not belong to this village."));
                                                        return 0;
                                                    }

                                                    String displayName = MCAIntegrationBridge.getEntityByUuid(player.serverLevel(), villagerId).getName().getString();

                                                    capital.setMonarchyRejected(false);
                                                    CapitalFoundationService.appointVillagerSovereign(
                                                            player.serverLevel(),
                                                            capital,
                                                            villagerId,
                                                            MCAIntegrationBridge.isFemale(player.serverLevel(), villagerId)
                                                    );
                                                    consumeCharter(player, capitalId);

                                                    player.sendSystemMessage(Component.literal(
                                                            "By acclaim of " + MCAIntegrationBridge.getVillageName(player.serverLevel(), capital.getVillageId())
                                                                    + ", " + displayName + " has been raised to the throne."
                                                    ));
                                                    return 1;
                                                }))))

                        .then(Commands.literal("reject")
                                .then(Commands.argument("capitalId", StringArgumentType.string())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                return 0;
                                            }

                                            UUID capitalId = UUID.fromString(StringArgumentType.getString(ctx, "capitalId"));
                                            CapitalRecord capital = CapitalManager.getCapital(capitalId);
                                            if (capital == null) {
                                                player.sendSystemMessage(Component.literal("That capital no longer exists."));
                                                return 0;
                                            }

                                            capital.setMonarchyRejected(true);
                                            capital.setState(com.example.mcacapitals.capital.CapitalState.PENDING);
                                            CapitalChronicleService.addEntry(player.serverLevel(), capital,
                                                    MCAIntegrationBridge.getVillageName(player.serverLevel(), capital.getVillageId())
                                                            + " rejected monarchy and remained ungoverned.");
                                            CapitalDataAccess.markDirty(player.serverLevel());
                                            consumeCharter(player, capitalId);

                                            player.sendSystemMessage(Component.literal(
                                                    MCAIntegrationBridge.getVillageName(player.serverLevel(), capital.getVillageId())
                                                            + " rejects monarchy and remains ungoverned."
                                            ));
                                            return 1;
                                        })))
        );
    }

    private static void consumeCharter(ServerPlayer player, UUID capitalId) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (isCharterForCapital(stack, capitalId)) {
                stack.shrink(1);
                return;
            }
        }

        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            ItemStack stack = player.getInventory().offhand.get(i);
            if (isCharterForCapital(stack, capitalId)) {
                stack.shrink(1);
                return;
            }
        }
    }

    private static boolean isCharterForCapital(ItemStack stack, UUID capitalId) {
        return stack != null
                && stack.is(ModItems.ROYAL_CHARTER.get())
                && stack.hasTag()
                && capitalId.toString().equals(stack.getTag().getString("CapitalId"));
    }
}