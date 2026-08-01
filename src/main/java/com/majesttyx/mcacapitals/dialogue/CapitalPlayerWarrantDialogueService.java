package com.majesttyx.mcacapitals.capital;

import forge.net.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class CapitalPlayerWarrantDialogueService {

    public static final String PAY_FINE_COMMAND = "mcacapitals_pay_warrant_fine";
    public static final String SURRENDER_COMMAND = "mcacapitals_surrender_warrant";

    private CapitalPlayerWarrantDialogueService() {
    }

    public static boolean canShowPayFine(
            ServerPlayer player,
            VillagerEntityMCA villager
    ) {
        CapitalRecord capital = resolveIssuingCapital(player, villager);
        return capital != null
                && CapitalPlayerWarrantService.hasWarrant(
                player.serverLevel(),
                player.getUUID(),
                capital
        );
    }

    public static boolean canShowSurrender(
            ServerPlayer player,
            VillagerEntityMCA villager
    ) {
        CapitalRecord capital = resolveIssuingCapital(player, villager);
        return capital != null
                && CapitalBuildingService.hasPrison(player.serverLevel(), capital)
                && CapitalPlayerWarrantService.hasWarrant(
                player.serverLevel(),
                player.getUUID(),
                capital
        );
    }

    public static boolean handleCommand(
            ServerPlayer player,
            Entity entity,
            String command
    ) {
        if (!(entity instanceof VillagerEntityMCA villager)) {
            return false;
        }
        CapitalRecord capital = resolveIssuingCapital(player, villager);
        if (capital == null) {
            return false;
        }
        if (PAY_FINE_COMMAND.equals(command)) {
            CapitalPlayerWarrantService.payFine(player, capital);
            return true;
        }
        if (SURRENDER_COMMAND.equals(command)) {
            CapitalPlayerWarrantService.surrender(player, capital);
            return true;
        }
        return false;
    }

    private static CapitalRecord resolveIssuingCapital(
            ServerPlayer player,
            VillagerEntityMCA villager
    ) {
        if (player == null || villager == null) {
            return null;
        }
        CapitalRecord capital = CapitalSovereignDeclarationPromptService.resolveCapital(
                player.serverLevel(),
                villager
        );
        return capital != null
                && villager.getUUID().equals(capital.getMasterOfLaws())
                ? capital
                : null;
    }
}