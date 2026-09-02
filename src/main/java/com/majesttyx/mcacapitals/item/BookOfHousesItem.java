package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.house.BookOfHousesService;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenCapitalChroniclePacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BookOfHousesItem extends Item {

    private static final double MAX_BIND_DISTANCE_SQR = 128.0D * 128.0D;

    public BookOfHousesItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.mcacapitals.book_of_houses");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(heldStack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(heldStack);
        }

        CapitalRecord capital = resolveCapital(serverPlayer, heldStack);
        if (capital == null) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("mcacapitals.book_of_houses.item.not_found")
            );
            return InteractionResultHolder.fail(heldStack);
        }

        ServerLevel capitalLevel = CapitalManager.resolveCapitalLevel(serverPlayer.serverLevel(), capital);
        if (capitalLevel == null) {
            capitalLevel = serverPlayer.serverLevel();
        }

        BookOfHousesService.bindBookItem(capitalLevel, capital, heldStack);

        ItemStack previewBook = new ItemStack(Items.WRITTEN_BOOK);
        BookOfHousesService.writeBook(capitalLevel, capital, previewBook);

        ModNetwork.sendToPlayer(serverPlayer, new OpenCapitalChroniclePacket(previewBook));
        return InteractionResultHolder.consume(heldStack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        tooltipComponents.add(
                Component.translatable("mcacapitals.book_of_houses.item.tooltip.record")
                        .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
                Component.translatable("mcacapitals.book_of_houses.item.tooltip.use")
                        .withStyle(ChatFormatting.GRAY)
        );
    }

    private CapitalRecord resolveCapital(ServerPlayer player, ItemStack stack) {
        CapitalRecord bound = resolveBoundCapital(stack);
        if (bound != null) {
            return bound;
        }

        CapitalRecord current = resolveCurrentCapital(player);
        if (current != null) {
            return current;
        }

        CapitalRecord nearest = resolveNearestCapital(player);
        if (nearest != null) {
            return nearest;
        }

        return CapitalManager.getCapitalBySovereign(player.getUUID());
    }

    private CapitalRecord resolveBoundCapital(ItemStack stack) {
        if (!ModItemStackData.hasCustomData(stack)) {
            return null;
        }

        String raw = ModItemStackData.getCustomData(stack).getString(ModDataKeys.CAPITAL_ID);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return CapitalManager.getCapital(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private CapitalRecord resolveCurrentCapital(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        Optional<Integer> lastSeenVillageId = MCAIntegrationBridge.getLastSeenVillageId(level, player);
        if (lastSeenVillageId.isPresent()) {
            int villageId = lastSeenVillageId.get();
            CapitalRecord capital = CapitalManager.getCapitalByVillageId(level, villageId);
            if (capital != null && MCAIntegrationBridge.isPlayerInVillage(level, player, villageId)) {
                return capital;
            }
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            Integer villageId = capital.getVillageId();
            if (villageId == null || !CapitalManager.isCapitalInLevel(capital, level)) {
                continue;
            }
            if (MCAIntegrationBridge.isPlayerInVillage(level, player, villageId)) {
                return capital;
            }
        }

        return null;
    }

    private CapitalRecord resolveNearestCapital(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        CapitalRecord nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            Integer villageId = capital.getVillageId();
            if (villageId == null || !CapitalManager.isCapitalInLevel(capital, level)) {
                continue;
            }

            BlockPos center = MCAIntegrationBridge.getVillageCenter(level, villageId);
            if (center == null) {
                continue;
            }

            double distance = player.distanceToSqr(
                    center.getX() + 0.5D,
                    center.getY() + 0.5D,
                    center.getZ() + 0.5D
            );

            if (distance <= MAX_BIND_DISTANCE_SQR && distance < nearestDistance) {
                nearest = capital;
                nearestDistance = distance;
            }
        }

        return nearest;
    }
}
