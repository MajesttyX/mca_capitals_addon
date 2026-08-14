package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.item.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
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
                        .then(
                                Commands.literal("claimself")
                                        .then(
                                                Commands.argument("capitalId", StringArgumentType.string())
                                                        .executes(ctx -> {
                                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                                return 0;
                                                            }

                                                            UUID capitalId = UUID.fromString(StringArgumentType.getString(ctx, "capitalId"));
                                                            CapitalRecord capital = CapitalManager.getCapital(capitalId);
                                                            if (capital == null) {
                                                                player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.that_capital_no_longer_exists"));
                                                                return 0;
                                                            }

                                                            if (capital.getSovereign() != null) {
                                                                player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.that_capital_already_has_a_sovereign"));
                                                                return 0;
                                                            }

                                                            int hearts = countCapitalHearts(player, capital);
                                                            if (hearts < CLAIM_HEARTS_REQUIRED) {
                                                                player.sendSystemMessage(Component.translatable(
                                                                        "mcacapitals.system.capital_founding_commands.claim_hearts_required",
                                                                        CLAIM_HEARTS_REQUIRED,
                                                                        hearts
                                                                ));
                                                                return 0;
                                                            }

                                                            CapitalFoundationService.appointPlayerSovereign(
                                                                    player.serverLevel(),
                                                                    capital,
                                                                    player.getUUID(),
                                                                    MCAIntegrationBridge.isPlayerFemale(player.serverLevel(), player)
                                                            );

                                                            consumeMatchingCharters(player, capitalId);
                                                            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.you_have_claimed_the_throne"));
                                                            return 1;
                                                        })
                                        )
                        )
                        .then(
                                Commands.literal("appoint")
                                        .then(
                                                Commands.argument("capitalId", StringArgumentType.string())
                                                        .then(
                                                                Commands.argument("villagerId", StringArgumentType.string())
                                                                        .executes(ctx -> {
                                                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                                                return 0;
                                                                            }

                                                                            UUID capitalId = UUID.fromString(StringArgumentType.getString(ctx, "capitalId"));
                                                                            UUID villagerId = UUID.fromString(StringArgumentType.getString(ctx, "villagerId"));

                                                                            CapitalRecord capital = CapitalManager.getCapital(capitalId);
                                                                            if (capital == null) {
                                                                                player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.that_capital_no_longer_exists"));
                                                                                return 0;
                                                                            }

                                                                            if (capital.getSovereign() != null) {
                                                                                player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.that_capital_already_has_a_sovereign"));
                                                                                return 0;
                                                                            }

                                                                            if (!MCAIntegrationBridge.isMCAVillager(player.serverLevel(), villagerId)) {
                                                                                player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.that_villager_is_no_longer_available"));
                                                                                return 0;
                                                                            }

                                                                            CapitalFoundationService.appointVillagerSovereign(
                                                                                    player.serverLevel(),
                                                                                    capital,
                                                                                    villagerId,
                                                                                    MCAIntegrationBridge.isFemale(player.serverLevel(), villagerId)
                                                                            );

                                                                            consumeMatchingCharters(player, capitalId);
                                                                            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.the_sovereign_has_been_appointed"));
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("reject")
                                        .then(
                                                Commands.argument("capitalId", StringArgumentType.string())
                                                        .executes(ctx -> {
                                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                                return 0;
                                                            }

                                                            UUID capitalId = UUID.fromString(StringArgumentType.getString(ctx, "capitalId"));
                                                            CapitalRecord capital = CapitalManager.getCapital(capitalId);
                                                            if (capital == null) {
                                                                player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.that_capital_no_longer_exists"));
                                                                return 0;
                                                            }

                                                            if (capital.getSovereign() != null) {
                                                                player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.that_capital_already_has_a_sovereign"));
                                                                return 0;
                                                            }

                                                            capital.setMonarchyRejected(true);
                                                            capital.setState(CapitalState.ACTIVE);
                                                            CapitalDataAccess.markDirty(player.serverLevel());
                                                            consumeMatchingCharters(player, capitalId);

                                                            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.the_people_will_remain_without_a_crown"));
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }

    private static int countCapitalHearts(ServerPlayer player, CapitalRecord capital) {
        Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(player.serverLevel(), capital.getVillageId());
        int total = 0;

        for (UUID resident : residents) {
            total += Math.max(0, MCAIntegrationBridge.getHeartsWithPlayer(player.serverLevel(), resident, player.getUUID()));
        }

        return total;
    }

    private static void consumeMatchingCharters(ServerPlayer player, UUID capitalId) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (isMatchingCharter(stack, capitalId)) {
                stack.shrink(1);
                return;
            }
        }

        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            ItemStack stack = player.getInventory().offhand.get(i);
            if (isMatchingCharter(stack, capitalId)) {
                stack.shrink(1);
                return;
            }
        }
    }

    private static boolean isMatchingCharter(ItemStack stack, UUID capitalId) {
        if (stack == null || !stack.is(ModItems.ROYAL_CHARTER.get()) || !ModItemStackData.hasCustomData(stack)) {
            return false;
        }

        CompoundTag tag = ModItemStackData.getCustomData(stack);
        return capitalId.toString().equals(tag.getString(ModDataKeys.CAPITAL_ID));
    }
}