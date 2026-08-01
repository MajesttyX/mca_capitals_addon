package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationshipEvent;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CapitalForeignAffairsService {

    public static final String DIALOGUE_COMMAND =
            "mcacapitals_ask_foreign_affairs";

    private static final double MAX_AMBASSADOR_DISTANCE_SQR =
            12.0D * 12.0D;

    private CapitalForeignAffairsService() {
    }

    public static boolean canShowDialogueAnswer(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        return resolveAudience(
                player,
                ambassadorEntity
        ) != null;
    }

    public static boolean showReport(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        if (player == null
                || ambassadorEntity == null) {
            return true;
        }

        CapitalRecord source =
                resolveAudience(
                        player,
                        ambassadorEntity
                );

        if (source == null) {
            sendMessage(
                    player,
                    "Foreign Affairs",
                    "This villager is not available to discuss foreign affairs."
            );

            return true;
        }

        ServerLevel level =
                player.serverLevel();

        List<CapitalRecord> targets =
                CapitalManager
                        .getAllCapitalRecords()
                        .stream()
                        .filter(target ->
                                target != null
                        )
                        .filter(target ->
                                target.getState()
                                        == CapitalState.ACTIVE
                        )
                        .filter(target ->
                                target.getCapitalId()
                                        != null
                        )
                        .filter(target ->
                                !target.getCapitalId()
                                        .equals(
                                                source.getCapitalId()
                                        )
                        )
                        .sorted(
                                Comparator.comparing(
                                        target ->
                                                CapitalDiplomaticAgreementText
                                                        .capitalName(
                                                                level,
                                                                target
                                                        ),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        MCAIntegrationBridge.stopInteracting(
                ambassadorEntity
        );

        String ambassadorName =
                ambassadorEntity
                        .getName()
                        .getString();

        if (targets.isEmpty()) {
            sendMessage(
                    player,
                    "Foreign Affairs",
                    ambassadorName
                            + ": There are no other established capitals with which we have foreign affairs."
            );

            return true;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries =
                new ArrayList<>();

        if (CapitalRoyalBetrothalService
                .hasOpenEscortRequests(
                        level,
                        source
                )) {
            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            "Royal Escort Requests",
                            "An accepted royal betrothal is waiting for its escort.",
                            "",
                            "",
                            "Review Royal Escorts",
                            "/capitalroyalescort review "
                                    + ambassadorEntity.getUUID(),
                            true,
                            ""
                    )
            );
        }

        for (CapitalRecord target :
                targets) {
            CapitalDiplomaticTruceService
                    .refreshExpiredTruce(
                            level,
                            source,
                            target
                    );

            int score =
                    CapitalDiplomacyDataAccess
                            .getRelationshipScore(
                                    level,
                                    source.getCapitalId(),
                                    target.getCapitalId()
                            );

            CapitalDiplomaticState state =
                    CapitalDiplomacyDataAccess
                            .getDiplomaticState(
                                    level,
                                    source.getCapitalId(),
                                    target.getCapitalId()
                            );

            boolean tradeActive =
                    CapitalDiplomaticTradeAgreementService
                            .isActive(
                                    level,
                                    source,
                                    target
                            );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            CapitalDiplomaticAgreementText
                                    .capitalName(
                                            level,
                                            target
                                    ),
                            "Relationship: "
                                    + CapitalRelationshipBand
                                    .fromScore(score)
                                    .getDisplayName()
                                    + " ("
                                    + score
                                    + ")",
                            "Status: "
                                    + foreignAffairsStatus(
                                    state
                            )
                                    + (tradeActive
                                    ? " | Trade Agreement: Active"
                                    : ""),
                            recentRelationshipChanges(
                                    level,
                                    source,
                                    target
                            ),
                            "",
                            "",
                            true,
                            ""
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.FOREIGN_AFFAIRS,
                        "Foreign Affairs",
                        ambassadorName,
                        "Here is the current state of our foreign affairs.",
                        "",
                        entries,
                        List.of()
                )
        );

        return true;
    }

    private static String recentRelationshipChanges(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target
    ) {
        List<CapitalRelationshipEvent> history =
                CapitalDiplomacyDataAccess
                        .getRelationshipHistory(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        if (history.isEmpty()) {
            return "Recent changes: None recorded";
        }

        List<String> recent = new ArrayList<>();

        for (int index = history.size() - 1;
             index >= 0 && recent.size() < 2;
             index--) {
            CapitalRelationshipEvent event =
                    history.get(index);

            if (event == null
                    || event.reason() == null
                    || event.reason().isBlank()) {
                continue;
            }

            String amount = event.amount() > 0
                    ? "+" + event.amount()
                    : Integer.toString(event.amount());

            recent.add(
                    amount
                            + " "
                            + event.reason()
            );
        }

        return recent.isEmpty()
                ? "Recent changes: None recorded"
                : "Recent changes: "
                + String.join("; ", recent);
    }

    private static String foreignAffairsStatus(
            CapitalDiplomaticState state
    ) {
        if (state == null) {
            return "Unknown";
        }

        return switch (state) {
            case PEACE -> "Peaceful";
            case ALLIANCE -> "Allied";
            case WAR -> "At War";
            case TRUCE -> "Truce";
            case NON_AGGRESSION_PACT ->
                    "Non-Aggression Pact";
        };
    }

    private static CapitalRecord resolveAudience(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        if (player == null
                || ambassadorEntity == null
                || !ambassadorEntity.isAlive()
                || player.level()
                != ambassadorEntity.level()
                || player.distanceToSqr(
                ambassadorEntity
        ) > MAX_AMBASSADOR_DISTANCE_SQR) {
            return null;
        }

        ServerLevel level =
                player.serverLevel();

        for (CapitalRecord capital :
                CapitalManager
                        .getAllCapitalRecords()) {
            if (capital != null
                    && capital.getState()
                    == CapitalState.ACTIVE
                    && CapitalAmbassadorService
                    .isAmbassador(
                            level,
                            capital,
                            ambassadorEntity.getUUID()
                    )) {
                return capital;
            }
        }

        return null;
    }

    private static void sendMessage(
            ServerPlayer player,
            String title,
            String message
    ) {
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.MESSAGE,
                        title,
                        "",
                        message,
                        "",
                        List.of(),
                        List.of()
                )
        );
    }
}