package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
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

                                                            Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(player.serverLevel(), capital.getVillageId());
                                                            if (!MCAReputationBridge.canClaimThrone(player.serverLevel(), residents, player.getUUID(), CLAIM_HEARTS_REQUIRED)) {
                                                                player.sendSystemMessage(Component.translatable(
                                                                        "mcacapitals.system.capital_founding_commands.claim_hearts_required",
                                                                        CLAIM_HEARTS_REQUIRED,
                                                                        MCAReputationBridge.getCapitalHeartsScore(player.serverLevel(), residents, player.getUUID())
                                                                ));
                                                                return 0;
                                                            }

                                                            capital.setMonarchyRejected(false);
                                                            CapitalFoundationService.appointPlayerSovereign(
                                                                    player.serverLevel(),
                                                                    capital,
                                                                    player.getUUID(),
                                                                    MCAIntegrationBridge.isPlayerFemale(player.serverLevel(), player)
                                                            );
                                                            consumeCharter(player, capitalId);

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

                                                                            Set<UUID> residents = MCAIntegrationBridge.getVillageResidents(player.serverLevel(), capital.getVillageId());
                                                                            if (!residents.contains(villagerId)) {
                                                                                player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.that_villager_does_not_belong_to_this_village"));
                                                                                return 0;
                                                                            }

                                                                            capital.setMonarchyRejected(false);
                                                                            CapitalFoundationService.appointVillagerSovereign(
                                                                                    player.serverLevel(),
                                                                                    capital,
                                                                                    villagerId,
                                                                                    MCAIntegrationBridge.isFemale(player.serverLevel(), villagerId)
                                                                            );
                                                                            consumeCharter(player, capitalId);

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

                                                            capital.setMonarchyRejected(true);
                                                            capital.setState(CapitalState.PENDING);
                                                            CapitalChronicleService.addEventWithoutHerald(
                                                                    player.serverLevel(),
                                                                    capital,
                                                                    CapitalChronicleEventId.MONARCHY_REJECTED,
                                                                    MCAIntegrationBridge.getVillageName(player.serverLevel(), capital.getVillageId())
                                                            );
                                                            CapitalDataAccess.markDirty(player.serverLevel());
                                                            consumeCharter(player, capitalId);

                                                            player.sendSystemMessage(Component.translatable("mcacapitals.system.capital_founding_commands.the_people_will_remain_without_a_crown"));
                                                            return 1;
                                                        })
                                        )
                        )
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
                && capitalId.toString().equals(stack.getTag().getString(ModDataKeys.CAPITAL_ID));
    }
}