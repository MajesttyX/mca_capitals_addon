package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalTradeAgreement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class CapitalTradeExchangeService {

    static final long TRADE_INTERVAL_TICKS = 48000L;
    private static final int RELATIONSHIP_BONUS = 2;

    private CapitalTradeExchangeService() {
    }

    static void processDueTrade(ServerLevel level, CapitalTradeAgreement agreement) {
        if (level == null || agreement == null) {
            return;
        }

        CapitalRecord first = CapitalManager.getCapital(agreement.getFirstCapitalId());
        CapitalRecord second = CapitalManager.getCapital(agreement.getSecondCapitalId());
        if (first == null || second == null) {
            CapitalAgreementDataAccess.endTradeAgreement(
                    level,
                    agreement.getFirstCapitalId(),
                    agreement.getSecondCapitalId()
            );
            return;
        }

        if (first.getState() != CapitalState.ACTIVE || second.getState() != CapitalState.ACTIVE) {
            return;
        }

        CapitalDiplomaticState state = CapitalDiplomacyDataAccess.getDiplomaticState(
                level,
                first.getCapitalId(),
                second.getCapitalId()
        );
        if (state == CapitalDiplomaticState.WAR || state == CapitalDiplomaticState.TRUCE) {
            CapitalDiplomaticTradeAgreementService.end(
                    level,
                    first,
                    second,
                    "because peaceful trade was no longer possible."
            );
            return;
        }

        if (!CapitalBuildingService.hasStorage(level, first)
                || !CapitalBuildingService.hasStorage(level, second)) {
            return;
        }

        long lastReferenceTime = agreement.getLastTradeAt() > 0L
                ? agreement.getLastTradeAt()
                : agreement.getEstablishedAt();
        if (level.getGameTime() - lastReferenceTime < TRADE_INTERVAL_TICKS) {
            return;
        }

        long tradeCycle = Math.max(1L, level.getGameTime() / TRADE_INTERVAL_TICKS);
        List<ItemStack> firstExports = CapitalTradeProfileService.createShipment(
                level,
                first,
                tradeCycle
        );
        List<ItemStack> secondExports = CapitalTradeProfileService.createShipment(
                level,
                second,
                tradeCycle
        );
        if (firstExports.isEmpty() || secondExports.isEmpty()) {
            return;
        }

        if (!CapitalDiplomaticStorageService.deposit(level, second, firstExports)) {
            return;
        }
        if (!CapitalDiplomaticStorageService.deposit(level, first, secondExports)) {
            return;
        }

        CapitalAgreementDataAccess.markTradeCompleted(
                level,
                first.getCapitalId(),
                second.getCapitalId()
        );
        int currentRelationship = CapitalDiplomacyDataAccess.getRelationshipScore(
                level,
                first.getCapitalId(),
                second.getCapitalId()
        );
        if (currentRelationship < 270) {
            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    first.getCapitalId(),
                    second.getCapitalId(),
                    Math.min(RELATIONSHIP_BONUS, 270 - currentRelationship),
                    "Trade exchange completed",
                    null
            );
        }
        recordTrade(level, first, second, firstExports, secondExports);
    }

    private static void recordTrade(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second,
            List<ItemStack> firstExports,
            List<ItemStack> secondExports
    ) {
        String firstName = CapitalDiplomaticAgreementText.capitalName(level, first);
        String secondName = CapitalDiplomaticAgreementText.capitalName(level, second);
        String entry = "A trade caravan delivered "
                + describe(firstExports)
                + " from " + firstName
                + " and " + describe(secondExports)
                + " from " + secondName + ".";
        CapitalChronicleService.addEntry(level, first, entry);
        CapitalChronicleService.addEntry(level, second, entry);
    }

    private static String describe(List<ItemStack> stacks) {
        return stacks.stream()
                .map(stack -> stack.getCount() + " × " + stack.getHoverName().getString())
                .toList()
                .stream()
                .reduce((first, second) -> first + ", " + second)
                .orElse("no goods");
    }
}