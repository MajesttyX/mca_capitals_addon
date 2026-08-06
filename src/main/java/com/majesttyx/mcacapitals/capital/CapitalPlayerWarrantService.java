package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalPlayerWarrantDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public final class CapitalPlayerWarrantService {
    public static final int WARRANT_FINE_EMERALDS = 16;
    public static final long LEAVE_ORDER_TICKS = 20L * 120L;
    public static final long SENTENCE_TICKS = 20L * 300L;

    private CapitalPlayerWarrantService() {
    }

    public static void orderToLeave(
            ServerPlayer player,
            CapitalRecord issuingCapital,
            String caseKey
    ) {
        if (player == null
                || issuingCapital == null
                || issuingCapital.getCapitalId() == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        CapitalPlayerWarrantDataAccess.setLeaveOrder(
                level,
                player.getUUID(),
                issuingCapital.getCapitalId(),
                LEAVE_ORDER_TICKS,
                caseKey
        );
        player.sendSystemMessage(Component.literal(
                "The court of "
                        + CapitalDiplomaticAgreementText.capitalName(level, issuingCapital)
                        + " has ordered you to leave. You have two real-time minutes to depart its bounds."
        ));
    }

    public static boolean hasWarrant(
            ServerLevel level,
            UUID playerId,
            CapitalRecord capital
    ) {
        return level != null
                && playerId != null
                && capital != null
                && capital.getCapitalId() != null
                && CapitalPlayerWarrantDataAccess.hasWarrant(
                level,
                playerId,
                capital.getCapitalId()
        );
    }

    public static int payFine(ServerPlayer player, CapitalRecord issuingCapital) {
        if (!validateWarrant(player, issuingCapital)) {
            return 0;
        }

        if (countEmeralds(player) < WARRANT_FINE_EMERALDS) {
            player.sendSystemMessage(Component.literal(
                    "You need exactly 16 emerald items to pay this warrant fine. Emerald blocks are not accepted."
            ));
            return 0;
        }
        removeEmeralds(player, WARRANT_FINE_EMERALDS);
        CapitalPlayerWarrantDataAccess.clearWarrant(
                player.serverLevel(),
                player.getUUID(),
                issuingCapital.getCapitalId()
        );
        player.getInventory().setChanged();
        player.sendSystemMessage(Component.literal(
                "You paid 16 emeralds. The warrant issued by "
                        + CapitalDiplomaticAgreementText.capitalName(
                        player.serverLevel(),
                        issuingCapital
                )
                        + " has been cleared."
        ));
        return 1;
    }

    public static int surrender(ServerPlayer player, CapitalRecord issuingCapital) {
        if (!validateWarrant(player, issuingCapital)) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        List<BlockPos> prisonCenters = CapitalBuildingService.getPrisonCenters(
                level,
                issuingCapital
        );
        boolean recognizedPrison = !prisonCenters.isEmpty();
        BlockPos holdingCenter = recognizedPrison
                ? prisonCenters.get(0)
                : MCAIntegrationBridge.getVillageCenter(level, issuingCapital.getVillageId());
        if (holdingCenter == null) {
            player.sendSystemMessage(Component.literal(
                    "This Capital has no available legal holding location."
            ));
            return 0;
        }
        player.teleportTo(
                holdingCenter.getX() + 0.5D,
                holdingCenter.getY() + 1.0D,
                holdingCenter.getZ() + 0.5D
        );
        CapitalPlayerWarrantDataAccess.setSentence(
                level,
                player.getUUID(),
                issuingCapital.getCapitalId(),
                SENTENCE_TICKS
        );
        player.sendSystemMessage(Component.literal(
                "You surrendered to "
                        + CapitalDiplomaticAgreementText.capitalName(level, issuingCapital)
                        + (recognizedPrison
                        ? ". Remain within its recognized Prison for five real-time minutes to complete the sentence."
                        : ". Remain within the court holding location for five real-time minutes to complete the sentence.")
        ));
        return 1;
    }

    public static boolean isInsidePrison(
            ServerPlayer player,
            CapitalRecord capital
    ) {
        if (player == null || capital == null) {
            return false;
        }
        List<AABB> prisonBounds = CapitalBuildingService.getPrisonBounds(
                player.serverLevel(),
                capital
        );
        for (AABB bounds : prisonBounds) {
            if (bounds.contains(player.position())) {
                return true;
            }
        }
        if (!prisonBounds.isEmpty()) {
            return false;
        }

        BlockPos fallbackCenter = MCAIntegrationBridge.getVillageCenter(
                player.serverLevel(),
                capital.getVillageId()
        );
        return fallbackCenter != null
                && new AABB(fallbackCenter).inflate(6.0D, 4.0D, 6.0D).contains(player.position());
    }

    private static boolean validateWarrant(ServerPlayer player, CapitalRecord issuingCapital) {
        if (player == null
                || issuingCapital == null
                || issuingCapital.getCapitalId() == null
                || !CapitalPlayerWarrantDataAccess.hasWarrant(
                player.serverLevel(),
                player.getUUID(),
                issuingCapital.getCapitalId()
        )) {
            if (player != null) {
                player.sendSystemMessage(Component.literal(
                        "This Capital has no active warrant for you."
                ));
            }
            return false;
        }
        return true;
    }

    private static int countEmeralds(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.EMERALD)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(Items.EMERALD)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeEmeralds(ServerPlayer player, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (!stack.is(Items.EMERALD)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (remaining <= 0) {
                break;
            }
            if (!stack.is(Items.EMERALD)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
    }
}
