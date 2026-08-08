package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalPlayerWarrantDataAccess;
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
    public static final int WARRANT_FINE_EMERALDS = 32;
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
                    "You need exactly 32 emerald items to pay this warrant fine. Emerald blocks are not accepted."
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
                "You paid 32 emeralds. The warrant issued by "
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
        if (prisonCenters.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "This Capital has no recognized Prison in which the sentence can be served."
            ));
            return 0;
        }
        BlockPos prisonCenter = prisonCenters.get(0);
        player.teleportTo(
                prisonCenter.getX() + 0.5D,
                prisonCenter.getY() + 1.0D,
                prisonCenter.getZ() + 0.5D
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
                        + ". Remain within its recognized Prison for five real-time minutes to complete the sentence."
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
        for (AABB bounds : CapitalBuildingService.getPrisonBounds(
                player.serverLevel(),
                capital
        )) {
            if (bounds.contains(player.position())) {
                return true;
            }
        }
        return false;
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