package com.majesttyx.mcacapitals.capital;

import fabric.net.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class CapitalPlayerWarrantDialogueService {
    public static final String PAY_FINE_COMMAND = "mcacapitals_pay_warrant_fine";
    public static final String SURRENDER_COMMAND = "mcacapitals_surrender_warrant";
    private static final double MAX_AUDIENCE_DISTANCE_SQR = 12.0D * 12.0D;

    private CapitalPlayerWarrantDialogueService() {
    }

    public static boolean canShowPayFine(ServerPlayer player, VillagerEntityMCA villager) {
        CapitalRecord capital = resolveLegalCapital(player, villager);
        return capital != null
                && player.distanceToSqr(villager) <= MAX_AUDIENCE_DISTANCE_SQR
                && CapitalPlayerWarrantService.hasWarrant(player.serverLevel(), player.getUUID(), capital);
    }

    public static boolean canShowSurrender(ServerPlayer player, VillagerEntityMCA villager) {
        return canShowPayFine(player, villager);
    }

    public static boolean handleCommand(ServerPlayer player, Entity entity, String command) {
        if (player == null || !(entity instanceof VillagerEntityMCA villager) || command == null) {
            return false;
        }
        if (!PAY_FINE_COMMAND.equals(command) && !SURRENDER_COMMAND.equals(command)) {
            return false;
        }
        CapitalRecord capital = resolveLegalCapital(player, villager);
        if (capital == null || player.distanceToSqr(villager) > MAX_AUDIENCE_DISTANCE_SQR) {
            return false;
        }
        int result = PAY_FINE_COMMAND.equals(command)
                ? CapitalPlayerWarrantService.payFine(player, capital)
                : CapitalPlayerWarrantService.surrender(player, capital);
        if (result > 0) {
            com.majesttyx.mcacapitals.util.MCAIntegrationBridge.stopInteracting(villager);
        }
        return true;
    }

    private static CapitalRecord resolveLegalCapital(ServerPlayer player, Entity villager) {
        if (player == null || villager == null) {
            return null;
        }
        CapitalRecord capital = CapitalSovereignDeclarationPromptService.resolveCapital(player.serverLevel(), villager);
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            return null;
        }
        java.util.UUID id = villager.getUUID();
        return id.equals(capital.getSovereign())
                || id.equals(capital.getHand())
                || id.equals(capital.getMasterOfLaws())
                ? capital
                : null;
    }
}
