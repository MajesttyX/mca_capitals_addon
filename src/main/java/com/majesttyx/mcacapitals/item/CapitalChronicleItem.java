package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenCapitalChroniclePacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.ModDataKeys;
import com.majesttyx.mcacapitals.util.ModItemStackData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;
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

        CompoundTag previewData = ModItemStackData.getCustomData(previewBook);

        MCACapitals.LOGGER.info(
                "[CapitalChronicle] Sending preview book to client for village '{}' with {} page entries.",
                previewData.getString(ModDataKeys.VILLAGE_NAME),
                previewData.contains(ModDataKeys.BOOK_PAGES)
                        ? previewData.getList(ModDataKeys.BOOK_PAGES, 8).size()
                        : 0
        );

        ModNetwork.sendToPlayer(serverPlayer, new OpenCapitalChroniclePacket(previewBook));
        return InteractionResultHolder.consume(heldStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("A written record of royal events, appointments, and succession.").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Right-click within a capital to read.").withStyle(ChatFormatting.GRAY));
    }

    private CapitalRecord resolveCapital(ServerPlayer player, ItemStack stack) {
        if (ModItemStackData.hasCustomData(stack)) {
            String raw = ModItemStackData.getCustomData(stack).getString(ModDataKeys.CAPITAL_ID);
            if (!raw.isBlank()) {
                try {
                    UUID capitalId = UUID.fromString(raw);
                    CapitalRecord byTag = CapitalManager.getCapital(capitalId);
                    if (byTag != null) {
                        return byTag;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        CapitalRecord bySovereign = CapitalManager.getCapitalBySovereign(player.getUUID());
        if (bySovereign != null) {
            return bySovereign;
        }

        return CapitalManager.getAllCapitalRecords().stream()
                .filter(capital -> capital.getVillageId() != null)
                .filter(capital -> MCAIntegrationBridge.getVillageCenter(player.serverLevel(), capital.getVillageId()) != null)
                .min(Comparator.comparingDouble(capital -> {
                    var center = MCAIntegrationBridge.getVillageCenter(player.serverLevel(), capital.getVillageId());
                    return player.distanceToSqr(
                            center.getX() + 0.5D,
                            center.getY() + 0.5D,
                            center.getZ() + 0.5D
                    );
                }))
                .filter(capital -> {
                    var center = MCAIntegrationBridge.getVillageCenter(player.serverLevel(), capital.getVillageId());
                    double distance = player.distanceToSqr(
                            center.getX() + 0.5D,
                            center.getY() + 0.5D,
                            center.getZ() + 0.5D
                    );
                    return distance <= MAX_BIND_DISTANCE_SQR;
                })
                .orElse(null);
    }
}