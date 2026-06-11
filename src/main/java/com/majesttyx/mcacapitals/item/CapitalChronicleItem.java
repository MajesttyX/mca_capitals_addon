package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenCapitalChroniclePacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
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
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class CapitalChronicleItem extends Item {

    private static final double MAX_BIND_DISTANCE_SQR = 128.0D * 128.0D;

    public CapitalChronicleItem() {
        super(new Properties().stacksTo(1));
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
            serverPlayer.sendSystemMessage(Component.literal("No capital chronicle can be found from here."));
            return InteractionResultHolder.fail(heldStack);
        }

        CapitalChronicleService.bindChronicleItem(serverPlayer.serverLevel(), capital, heldStack);

        ItemStack previewBook = new ItemStack(Items.WRITTEN_BOOK);
        CapitalChronicleService.bindChronicleItem(serverPlayer.serverLevel(), capital, previewBook);
        CapitalChronicleService.writeChronicleBook(serverPlayer.serverLevel(), capital, previewBook);

        MCACapitals.LOGGER.info(
                "[CapitalChronicle] Sending preview book to client for village '{}' with {} page entries.",
                previewBook.getOrCreateTag().getString(ModDataKeys.VILLAGE_NAME),
                previewBook.getOrCreateTag().contains(ModDataKeys.BOOK_PAGES)
                        ? previewBook.getOrCreateTag().getList(ModDataKeys.BOOK_PAGES, 8).size()
                        : 0
        );

        ModNetwork.sendToPlayer(serverPlayer, new OpenCapitalChroniclePacket(previewBook));
        return InteractionResultHolder.consume(heldStack);
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
        if (stack == null || !stack.hasTag()) {
            return null;
        }

        String raw = stack.getTag().getString(ModDataKeys.CAPITAL_ID);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            UUID capitalId = UUID.fromString(raw);
            return CapitalManager.getCapital(capitalId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private CapitalRecord resolveCurrentCapital(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        Optional<Integer> lastSeenVillageId = MCAIntegrationBridge.getLastSeenVillageId(level, player);
        if (lastSeenVillageId.isPresent()) {
            Integer villageId = lastSeenVillageId.get();
            CapitalRecord capital = CapitalManager.getCapitalByVillageId(villageId);

            if (capital != null && MCAIntegrationBridge.isPlayerInVillage(level, player, villageId)) {
                return capital;
            }
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            Integer villageId = capital.getVillageId();
            if (villageId == null) {
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
            if (villageId == null) {
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

            if (distance > MAX_BIND_DISTANCE_SQR) {
                continue;
            }

            if (distance < nearestDistance) {
                nearest = capital;
                nearestDistance = distance;
            }
        }

        return nearest;
    }
}