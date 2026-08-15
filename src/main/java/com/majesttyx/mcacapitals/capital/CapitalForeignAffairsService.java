package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationshipEvent;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
                    Component.translatable("mcacapitals.ui.foreign_affairs.title"),
                    Component.translatable("mcacapitals.ui.foreign_affairs.unavailable")
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

        if (targets.isEmpty()) {
            sendMessage(
                    player,
                    Component.translatable("mcacapitals.ui.foreign_affairs.title"),
                    Component.translatable(
                            "mcacapitals.ui.foreign_affairs.no_targets",
                            ambassadorEntity.getName()
                    )
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
                            Component.translatable("mcacapitals.ui.royal_escort.title"),
                            Component.translatable("mcacapitals.ui.royal_escort.waiting"),
                            Component.empty(),
                            Component.empty(),
                            Component.translatable("mcacapitals.ui.royal_escort.review"),
                            "/capitalroyalescort review "
                                    + ambassadorEntity.getUUID(),
                            true,
                            Component.empty()
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

            String targetName =
                    CapitalDiplomaticAgreementText
                            .capitalName(
                                    level,
                                    target
                            );

            Component targetNameComponent = targetName == null || targetName.isBlank()
                    ? Component.translatable("mcacapitals.diplomacy.unknown_capital")
                    : Component.literal(targetName);

            Component status = tradeActive
                    ? Component.translatable(
                            "mcacapitals.ui.foreign_affairs.status_with_trade",
                            foreignAffairsStatus(state)
                    )
                    : Component.translatable(
                            "mcacapitals.ui.foreign_affairs.status",
                            foreignAffairsStatus(state)
                    );

            entries.add(
                    new OpenAmbassadorCommunicationPacket.Entry(
                            targetNameComponent,
                            Component.translatable(
                                    "mcacapitals.ui.diplomacy.relationship_score",
                                    CapitalRelationshipBand
                                            .fromScore(score)
                                            .getDisplayComponent(),
                                    score
                            ),
                            status,
                            recentRelationshipChanges(
                                    level,
                                    source,
                                    target
                            ),
                            Component.empty(),
                            "",
                            true,
                            Component.empty()
                    )
            );
        }

        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.FOREIGN_AFFAIRS,
                        Component.translatable("mcacapitals.ui.foreign_affairs.title"),
                        ambassadorEntity.getName(),
                        Component.translatable("mcacapitals.ui.foreign_affairs.current_state"),
                        "",
                        entries,
                        List.of()
                )
        );

        return true;
    }

    private static Component recentRelationshipChanges(
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
            return Component.translatable("mcacapitals.ui.foreign_affairs.recent_none");
        }

        List<Component> recent = new ArrayList<>();

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
                    Component.translatable(
                            "mcacapitals.ui.foreign_affairs.change_entry",
                            Component.literal(amount),
                            event.reasonComponent()
                    )
            );
        }

        if (recent.isEmpty()) {
            return Component.translatable("mcacapitals.ui.foreign_affairs.recent_none");
        }

        MutableComponent joined = Component.empty();
        for (int index = 0; index < recent.size(); index++) {
            if (index > 0) {
                joined.append(Component.literal("; "));
            }
            joined.append(recent.get(index));
        }

        return Component.translatable(
                "mcacapitals.ui.foreign_affairs.recent_changes",
                joined
        );
    }

    private static Component foreignAffairsStatus(
            CapitalDiplomaticState state
    ) {
        if (state == null) {
            return Component.translatable("mcacapitals.ui.foreign_affairs.status_unknown");
        }

        return Component.translatable(
                "mcacapitals.ui.foreign_affairs.status_"
                        + state.getSerializedName()
        );
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
            Component title,
            Component message
    ) {
        ModNetwork.sendToPlayer(
                player,
                new OpenAmbassadorCommunicationPacket(
                        OpenAmbassadorCommunicationPacket.Mode.MESSAGE,
                        title,
                        Component.empty(),
                        message,
                        "",
                        List.of(),
                        List.of()
                )
        );
    }
}
