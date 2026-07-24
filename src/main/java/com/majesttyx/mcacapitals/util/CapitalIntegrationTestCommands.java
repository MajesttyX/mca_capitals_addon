package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalDiplomaticState;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalSystemCleanupService;
import com.majesttyx.mcacapitals.capital.CapitalWartimeSuccessionService;
import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalInterregnumRecord;
import com.majesttyx.mcacapitals.data.CapitalRelationKey;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import com.majesttyx.mcacapitals.data.CapitalRefugeeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalTradeAgreement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;

public final class CapitalIntegrationTestCommands {

    private static final long TRUCE_DURATION_TICKS =
            48000L;

    private static final long TRADE_INTERVAL_TICKS =
            48000L;

    private CapitalIntegrationTestCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("capitalintegration")
                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )
                        .then(
                                Commands.literal("summary")
                                        .executes(
                                                context ->
                                                        summary(
                                                                context
                                                                        .getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("relationship")
                                        .then(
                                                Commands.argument(
                                                                "firstCapitalId",
                                                                StringArgumentType
                                                                        .word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "secondCapitalId",
                                                                                StringArgumentType
                                                                                        .word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "score",
                                                                                                StringArgumentType
                                                                                                        .word()
                                                                                        )
                                                                                        .executes(
                                                                                                context ->
                                                                                                        setRelationship(
                                                                                                                context
                                                                                                                        .getSource(),
                                                                                                                StringArgumentType
                                                                                                                        .getString(
                                                                                                                                context,
                                                                                                                                "firstCapitalId"
                                                                                                                        ),
                                                                                                                StringArgumentType
                                                                                                                        .getString(
                                                                                                                                context,
                                                                                                                                "secondCapitalId"
                                                                                                                        ),
                                                                                                                StringArgumentType
                                                                                                                        .getString(
                                                                                                                                context,
                                                                                                                                "score"
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("state")
                                        .then(
                                                Commands.argument(
                                                                "firstCapitalId",
                                                                StringArgumentType
                                                                        .word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "secondCapitalId",
                                                                                StringArgumentType
                                                                                        .word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "state",
                                                                                                StringArgumentType
                                                                                                        .word()
                                                                                        )
                                                                                        .executes(
                                                                                                context ->
                                                                                                        setState(
                                                                                                                context
                                                                                                                        .getSource(),
                                                                                                                StringArgumentType
                                                                                                                        .getString(
                                                                                                                                context,
                                                                                                                                "firstCapitalId"
                                                                                                                        ),
                                                                                                                StringArgumentType
                                                                                                                        .getString(
                                                                                                                                context,
                                                                                                                                "secondCapitalId"
                                                                                                                        ),
                                                                                                                StringArgumentType
                                                                                                                        .getString(
                                                                                                                                context,
                                                                                                                                "state"
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "cleargiftcooldown"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "sourceCapitalId",
                                                                StringArgumentType
                                                                        .word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "targetCapitalId",
                                                                                StringArgumentType
                                                                                        .word()
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        clearGiftCooldown(
                                                                                                context
                                                                                                        .getSource(),
                                                                                                StringArgumentType
                                                                                                        .getString(
                                                                                                                context,
                                                                                                                "sourceCapitalId"
                                                                                                        ),
                                                                                                StringArgumentType
                                                                                                        .getString(
                                                                                                                context,
                                                                                                                "targetCapitalId"
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("forcetradedue")
                                        .then(
                                                Commands.argument(
                                                                "firstCapitalId",
                                                                StringArgumentType
                                                                        .word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "secondCapitalId",
                                                                                StringArgumentType
                                                                                        .word()
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        forceTradeDue(
                                                                                                context
                                                                                                        .getSource(),
                                                                                                StringArgumentType
                                                                                                        .getString(
                                                                                                                context,
                                                                                                                "firstCapitalId"
                                                                                                        ),
                                                                                                StringArgumentType
                                                                                                        .getString(
                                                                                                                context,
                                                                                                                "secondCapitalId"
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "removecapitaldata"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "capitalId",
                                                                StringArgumentType
                                                                        .word()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        removeCapitalData(
                                                                                context
                                                                                        .getSource(),
                                                                                StringArgumentType
                                                                                        .getString(
                                                                                                context,
                                                                                                "capitalId"
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int summary(
            CommandSourceStack source
    ) {
        ServerLevel level =
                source.getLevel();

        source.sendSuccess(
                () -> Component.literal(
                        "Capitals: "
                                + CapitalManager
                                .getAllCapitalRecords()
                                .size()
                                + " | Relationships: "
                                + CapitalDiplomacyDataAccess
                                .get(level)
                                .getRelationshipsSnapshot()
                                .size()
                                + " | Proposals: "
                                + CapitalAgreementDataAccess
                                .getProposalsSnapshot(level)
                                .size()
                                + " | Trade Agreements: "
                                + CapitalAgreementDataAccess
                                .getTradeAgreementsSnapshot(
                                        level
                                )
                                .size()
                                + " | Campaigns: "
                                + CapitalCampaignDataAccess
                                .getActiveCampaigns(level)
                                .size()
                                + " | Refugees: "
                                + CapitalRefugeeDataAccess
                                .getSnapshot(level)
                                .size()
                ),
                false
        );

        for (CapitalRecord capital :
                CapitalManager
                        .getAllCapitalRecords()) {
            String name =
                    capital.getVillageId() == null
                            ? "Unknown Capital"
                            : MCAIntegrationBridge
                            .getVillageName(
                                    level,
                                    capital.getVillageId()
                            );

            CapitalCampaignRecord campaign =
                    CapitalCampaignDataAccess
                            .getCampaignForCapital(
                                    level,
                                    capital.getCapitalId()
                            );

            CapitalInterregnumRecord interregnum =
                    CapitalWartimeSuccessionService
                            .getRecord(
                                    level,
                                    capital.getCapitalId()
                            );

            String campaignStatus =
                    campaign == null
                            ? "none"
                            : campaign.getPhase()
                            + "/"
                            + campaign.getCampaignId();

            String interregnumStatus =
                    interregnum == null
                            ? "none"
                            : CapitalWartimeSuccessionService
                            .getStatusLine(
                                    level,
                                    capital
                            );

            source.sendSuccess(
                    () -> Component.literal(
                            name
                                    + " | id="
                                    + capital.getCapitalId()
                                    + " | state="
                                    + capital.getState()
                                    + " | sovereign="
                                    + capital.getSovereign()
                                    + " | playerSovereign="
                                    + capital
                                    .getPlayerSovereignId()
                                    + " | campaign="
                                    + campaignStatus
                                    + " | interregnum="
                                    + interregnumStatus
                    ),
                    false
            );
        }

        for (Map.Entry<
                CapitalRelationKey,
                CapitalRelationRecord
                > entry :
                CapitalDiplomacyDataAccess
                        .get(level)
                        .getRelationshipsSnapshot()
                        .entrySet()) {
            CapitalRelationKey key =
                    entry.getKey();

            CapitalRelationRecord relation =
                    entry.getValue();

            source.sendSuccess(
                    () -> Component.literal(
                            "Relation "
                                    + key.first()
                                    + " <-> "
                                    + key.second()
                                    + " | score="
                                    + relation.getScore()
                                    + " | state="
                                    + relation
                                    .getDiplomaticState()
                                    + " | truceUntil="
                                    + relation.getTruceUntil()
                    ),
                    false
            );
        }

        for (CapitalTradeAgreement agreement :
                CapitalAgreementDataAccess
                        .getTradeAgreementsSnapshot(
                                level
                        )
                        .values()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "Trade "
                                    + agreement
                                    .getFirstCapitalId()
                                    + " <-> "
                                    + agreement
                                    .getSecondCapitalId()
                                    + " | established="
                                    + agreement
                                    .getEstablishedAt()
                                    + " | lastTrade="
                                    + agreement
                                    .getLastTradeAt()
                    ),
                    false
            );
        }

        return 1;
    }

    private static int setRelationship(
            CommandSourceStack source,
            String rawFirstCapitalId,
            String rawSecondCapitalId,
            String rawScore
    ) {
        ServerLevel level =
                source.getLevel();

        UUID firstCapitalId =
                parseCapitalId(
                        source,
                        rawFirstCapitalId,
                        "first"
                );

        UUID secondCapitalId =
                parseCapitalId(
                        source,
                        rawSecondCapitalId,
                        "second"
                );

        Integer desiredScore =
                parseScore(
                        source,
                        rawScore
                );

        if (firstCapitalId == null
                || secondCapitalId == null
                || desiredScore == null) {
            return 0;
        }

        int current =
                CapitalDiplomacyDataAccess
                        .getRelationshipScore(
                                level,
                                firstCapitalId,
                                secondCapitalId
                        );

        CapitalDiplomacyDataAccess
                .adjustRelationship(
                        level,
                        firstCapitalId,
                        secondCapitalId,
                        desiredScore - current,
                        "Integration test override",
                        null
                );

        int applied =
                CapitalDiplomacyDataAccess
                        .getRelationshipScore(
                                level,
                                firstCapitalId,
                                secondCapitalId
                        );

        source.sendSuccess(
                () -> Component.literal(
                        "Relationship score set to "
                                + applied
                                + "."
                ),
                false
        );

        return 1;
    }

    private static int setState(
            CommandSourceStack source,
            String rawFirstCapitalId,
            String rawSecondCapitalId,
            String rawState
    ) {
        ServerLevel level =
                source.getLevel();

        UUID firstCapitalId =
                parseCapitalId(
                        source,
                        rawFirstCapitalId,
                        "first"
                );

        UUID secondCapitalId =
                parseCapitalId(
                        source,
                        rawSecondCapitalId,
                        "second"
                );

        CapitalDiplomaticState state =
                parseState(
                        source,
                        rawState
                );

        if (firstCapitalId == null
                || secondCapitalId == null
                || state == null) {
            return 0;
        }

        long truceUntil =
                state
                        == CapitalDiplomaticState.TRUCE
                        ? level.getGameTime()
                        + TRUCE_DURATION_TICKS
                        : 0L;

        CapitalDiplomacyDataAccess
                .setDiplomaticState(
                        level,
                        firstCapitalId,
                        secondCapitalId,
                        state,
                        truceUntil
                );

        source.sendSuccess(
                () -> Component.literal(
                        "Diplomatic state set to "
                                + state
                                + "."
                ),
                false
        );

        return 1;
    }

    private static int clearGiftCooldown(
            CommandSourceStack source,
            String rawSourceCapitalId,
            String rawTargetCapitalId
    ) {
        ServerLevel level =
                source.getLevel();

        UUID sourceCapitalId =
                parseCapitalId(
                        source,
                        rawSourceCapitalId,
                        "source"
                );

        UUID targetCapitalId =
                parseCapitalId(
                        source,
                        rawTargetCapitalId,
                        "target"
                );

        if (sourceCapitalId == null
                || targetCapitalId == null) {
            return 0;
        }

        boolean removed =
                CapitalDiplomacyDataAccess
                        .clearGiftCooldown(
                                level,
                                sourceCapitalId,
                                targetCapitalId
                        );

        source.sendSuccess(
                () -> Component.literal(
                        removed
                                ? "Gift cooldown cleared."
                                : "No gift cooldown existed for that route."
                ),
                false
        );

        return 1;
    }

    private static int forceTradeDue(
            CommandSourceStack source,
            String rawFirstCapitalId,
            String rawSecondCapitalId
    ) {
        ServerLevel level =
                source.getLevel();

        UUID firstCapitalId =
                parseCapitalId(
                        source,
                        rawFirstCapitalId,
                        "first"
                );

        UUID secondCapitalId =
                parseCapitalId(
                        source,
                        rawSecondCapitalId,
                        "second"
                );

        if (firstCapitalId == null
                || secondCapitalId == null) {
            return 0;
        }

        CapitalTradeAgreement agreement =
                CapitalAgreementDataAccess
                        .getTradeAgreement(
                                level,
                                firstCapitalId,
                                secondCapitalId
                        );

        if (agreement == null) {
            source.sendFailure(
                    Component.literal(
                            "No active Trade Agreement exists between those capitals."
                    )
            );

            return 0;
        }

        agreement.setLastTradeAt(
                Math.max(
                        0L,
                        level.getGameTime()
                                - TRADE_INTERVAL_TICKS
                )
        );

        CapitalAgreementDataAccess
                .get(level)
                .setDirty();

        source.sendSuccess(
                () -> Component.literal(
                        "The Trade Agreement is now due for its next exchange check."
                ),
                false
        );

        return 1;
    }

    private static int removeCapitalData(
            CommandSourceStack source,
            String rawCapitalId
    ) {
        UUID capitalId =
                parseUuid(
                        source,
                        rawCapitalId,
                        "The capital ID is invalid."
                );

        if (capitalId == null) {
            return 0;
        }

        boolean removed =
                CapitalSystemCleanupService
                        .removeCapital(
                                source.getLevel(),
                                capitalId
                        );

        source.sendSuccess(
                () -> Component.literal(
                        removed
                                ? "The capital and its cross-system data were removed."
                                : "No capital or connected data was found for that ID."
                ),
                false
        );

        return removed ? 1 : 0;
    }

    private static UUID parseCapitalId(
            CommandSourceStack source,
            String rawValue,
            String label
    ) {
        UUID capitalId =
                parseUuid(
                        source,
                        rawValue,
                        "The "
                                + label
                                + " capital ID is invalid."
                );

        if (capitalId == null) {
            return null;
        }

        if (CapitalManager.getCapital(capitalId)
                == null) {
            source.sendFailure(
                    Component.literal(
                            "No active capital record exists for the "
                                    + label
                                    + " capital ID."
                    )
            );

            return null;
        }

        return capitalId;
    }

    private static UUID parseUuid(
            CommandSourceStack source,
            String rawValue,
            String failureMessage
    ) {
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            source.sendFailure(
                    Component.literal(
                            failureMessage
                    )
            );

            return null;
        }
    }

    private static Integer parseScore(
            CommandSourceStack source,
            String rawScore
    ) {
        try {
            int score =
                    Integer.parseInt(rawScore);

            if (score < -100 || score > 100) {
                source.sendFailure(
                        Component.literal(
                                "Relationship score must be between -100 and 100."
                        )
                );

                return null;
            }

            return score;
        } catch (NumberFormatException ignored) {
            source.sendFailure(
                    Component.literal(
                            "The relationship score is invalid."
                    )
            );

            return null;
        }
    }

    private static CapitalDiplomaticState parseState(
            CommandSourceStack source,
            String rawState
    ) {
        for (CapitalDiplomaticState state :
                CapitalDiplomaticState.values()) {
            if (state.getSerializedName()
                    .equals(rawState)) {
                return state;
            }
        }

        source.sendFailure(
                Component.literal(
                        "State must be peace, non_aggression_pact, alliance, truce, or war."
                )
        );

        return null;
    }
}