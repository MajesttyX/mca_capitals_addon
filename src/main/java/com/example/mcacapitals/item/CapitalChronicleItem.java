package com.example.mcacapitals.item;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CapitalChronicleItem extends WrittenBookItem {

    private static final double MAX_BIND_DISTANCE_SQR = 128.0D * 128.0D;

    public CapitalChronicleItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        CapitalRecord capital = resolveCapital(serverPlayer, stack);
        if (capital == null) {
            serverPlayer.sendSystemMessage(Component.literal("No capital chronicle can be found from here."));
            return InteractionResultHolder.fail(stack);
        }

        CapitalChronicleService.bindChronicleItem(serverPlayer.serverLevel(), capital, stack);
        writeChroniclePages(serverPlayer, stack, capital);

        return super.use(level, player, hand);
    }

    private void writeChroniclePages(ServerPlayer player, ItemStack stack, CapitalRecord capital) {
        List<String> pages = CapitalChronicleService.createPages(player.serverLevel(), capital);

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", "Chronicle of " + MCAIntegrationBridge.getVillageName(player.serverLevel(), capital.getVillageId()));
        tag.putString("author", "The Royal Chancery");
        tag.putBoolean("resolved", true);

        ListTag pageList = new ListTag();
        for (String page : pages) {
            pageList.add(StringTag.valueOf(page));
        }
        tag.put("pages", pageList);
    }

    private CapitalRecord resolveCapital(ServerPlayer player, ItemStack stack) {
        if (stack.hasTag()) {
            String raw = stack.getTag().getString("CapitalId");
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

        return CapitalManager.getAllCapitals().values().stream()
                .filter(capital -> capital.getVillageId() != null)
                .min(Comparator.comparingDouble(capital -> player.distanceToSqr(
                        MCAIntegrationBridge.getVillageCenter(player.serverLevel(), capital.getVillageId()).getX() + 0.5D,
                        MCAIntegrationBridge.getVillageCenter(player.serverLevel(), capital.getVillageId()).getY() + 0.5D,
                        MCAIntegrationBridge.getVillageCenter(player.serverLevel(), capital.getVillageId()).getZ() + 0.5D
                )))
                .filter(capital -> {
                    double distance = player.distanceToSqr(
                            MCAIntegrationBridge.getVillageCenter(player.serverLevel(), capital.getVillageId()).getX() + 0.5D,
                            MCAIntegrationBridge.getVillageCenter(player.serverLevel(), capital.getVillageId()).getY() + 0.5D,
                            MCAIntegrationBridge.getVillageCenter(player.serverLevel(), capital.getVillageId()).getZ() + 0.5D
                    );
                    return distance <= MAX_BIND_DISTANCE_SQR;
                })
                .orElse(null);
    }
}