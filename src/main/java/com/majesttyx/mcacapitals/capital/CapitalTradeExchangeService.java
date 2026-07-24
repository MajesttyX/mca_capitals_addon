package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalTradeAgreement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

final class CapitalTradeExchangeService {

    static final long TRADE_INTERVAL_TICKS =
            48000L;

    private static final int RELATIONSHIP_BONUS =
            1;

    private CapitalTradeExchangeService() {
    }

    static void processDueTrade(
            ServerLevel level,
            CapitalTradeAgreement agreement
    ) {
        if (level == null || agreement == null) {
            return;
        }

        CapitalRecord first =
                CapitalManager.getCapital(
                        agreement.getFirstCapitalId()
                );

        CapitalRecord second =
                CapitalManager.getCapital(
                        agreement.getSecondCapitalId()
                );

        if (first == null || second == null) {
            CapitalAgreementDataAccess
                    .endTradeAgreement(
                            level,
                            agreement.getFirstCapitalId(),
                            agreement.getSecondCapitalId()
                    );

            return;
        }

        if (first.getState()
                != CapitalState.ACTIVE
                || second.getState()
                != CapitalState.ACTIVE) {
            return;
        }

        CapitalDiplomaticState state =
                CapitalDiplomacyDataAccess
                        .getDiplomaticState(
                                level,
                                first.getCapitalId(),
                                second.getCapitalId()
                        );

        if (state == CapitalDiplomaticState.WAR
                || state
                == CapitalDiplomaticState.TRUCE) {
            CapitalDiplomaticTradeAgreementService
                    .end(
                            level,
                            first,
                            second,
                            "because peaceful trade was no longer possible."
                    );

            return;
        }

        if (!CapitalBuildingService.hasStorage(
                level,
                first
        )
                || !CapitalBuildingService.hasStorage(
                level,
                second
        )) {
            return;
        }

        long lastReferenceTime =
                agreement.getLastTradeAt() > 0L
                        ? agreement.getLastTradeAt()
                        : agreement.getEstablishedAt();

        if (level.getGameTime()
                - lastReferenceTime
                < TRADE_INTERVAL_TICKS) {
            return;
        }

        long tradeCycle = Math.max(
                1L,
                level.getGameTime()
                        / TRADE_INTERVAL_TICKS
        );

        CapitalDiplomaticStorageService
                .TradeExchangeResult result =
                CapitalDiplomaticStorageService
                        .exchange(
                                level,
                                first,
                                second,
                                tradeCycle
                        );

        if (!result.successful()) {
            return;
        }

        CapitalAgreementDataAccess
                .markTradeCompleted(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId()
                );

        CapitalDiplomacyDataAccess
                .adjustRelationship(
                        level,
                        first.getCapitalId(),
                        second.getCapitalId(),
                        RELATIONSHIP_BONUS,
                        "Trade exchange completed",
                        null
                );

        recordTrade(
                level,
                first,
                second,
                result.firstExport(),
                result.secondExport()
        );
    }

    private static void recordTrade(
            ServerLevel level,
            CapitalRecord first,
            CapitalRecord second,
            ItemStack firstExport,
            ItemStack secondExport
    ) {
        String firstName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                first
                        );

        String secondName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                second
                        );

        String entry =
                "A trade caravan exchanged "
                        + describe(firstExport)
                        + " from "
                        + firstName
                        + " for "
                        + describe(secondExport)
                        + " from "
                        + secondName
                        + ".";

        CapitalChronicleService.addEntry(
                level,
                first,
                entry
        );

        CapitalChronicleService.addEntry(
                level,
                second,
                entry
        );
    }

    private static String describe(
            ItemStack stack
    ) {
        return stack.getCount()
                + " × "
                + stack.getHoverName()
                .getString();
    }
}