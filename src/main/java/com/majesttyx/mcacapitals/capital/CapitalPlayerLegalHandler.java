package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalPlayerWarrantDataAccess;
import com.majesttyx.mcacapitals.data.CapitalPlayerWarrantSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CapitalPlayerLegalHandler {

    private final Set<String> notifiedWantedEntries = new HashSet<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 == 0) {
            PlayerCapitalAllegianceService.synchronizePlayerSovereignDeclaration(player);
            CapitalForeignStorageRaidService.refreshIncident(player);
        }

        tickLeaveOrders(player);
        tickSentences(player);
        notifyWantedEntries(player);
    }

    private void tickLeaveOrders(ServerPlayer player) {
        Map<UUID, CapitalPlayerWarrantSavedData.LeaveOrder> orders =
                CapitalPlayerWarrantDataAccess.getLeaveOrders(
                        player.serverLevel(),
                        player.getUUID()
                );

        for (Map.Entry<UUID, CapitalPlayerWarrantSavedData.LeaveOrder> entry : orders.entrySet()) {
            CapitalRecord capital = CapitalManager.getCapital(entry.getKey());
            if (capital == null || capital.getState() != CapitalState.ACTIVE) {
                CapitalPlayerWarrantDataAccess.clearLeaveOrder(
                        player.serverLevel(),
                        player.getUUID(),
                        entry.getKey()
                );
                continue;
            }

            if (!CapitalPlayerNotificationService.isPlayerWithinCapital(
                    player.serverLevel(),
                    capital,
                    player
            )) {
                CapitalPlayerWarrantDataAccess.clearLeaveOrder(
                        player.serverLevel(),
                        player.getUUID(),
                        entry.getKey()
                );
                continue;
            }

            long remaining = entry.getValue().remainingTicks() - 1L;
            if (remaining > 0L) {
                CapitalPlayerWarrantDataAccess.updateLeaveOrder(
                        player.serverLevel(),
                        player.getUUID(),
                        entry.getKey(),
                        remaining
                );
                continue;
            }

            CapitalPlayerWarrantDataAccess.clearLeaveOrder(
                    player.serverLevel(),
                    player.getUUID(),
                    entry.getKey()
            );
            if (CapitalPlayerWarrantDataAccess.issueWarrant(
                    player.serverLevel(),
                    player.getUUID(),
                    entry.getKey()
            )) {
                player.sendSystemMessage(Component.translatable(
                        "mcacapitals.justice.player_warrant.issued_for_failure_to_leave",
                        CapitalDiplomaticAgreementText.capitalName(
                                player.serverLevel(),
                                capital
                        )
                ));
            }
        }
    }

    private void tickSentences(ServerPlayer player) {
        Map<UUID, Long> sentences = CapitalPlayerWarrantDataAccess.getSentences(
                player.serverLevel(),
                player.getUUID()
        );
        for (Map.Entry<UUID, Long> entry : sentences.entrySet()) {
            CapitalRecord capital = CapitalManager.getCapital(entry.getKey());
            if (capital == null
                    || !CapitalBuildingService.hasPrison(player.serverLevel(), capital)
                    || !CapitalPlayerWarrantService.isInsidePrison(player, capital)) {
                continue;
            }

            long remaining = entry.getValue() - 1L;
            if (remaining > 0L) {
                CapitalPlayerWarrantDataAccess.updateSentence(
                        player.serverLevel(),
                        player.getUUID(),
                        entry.getKey(),
                        remaining
                );
                continue;
            }

            CapitalPlayerWarrantDataAccess.clearWarrant(
                    player.serverLevel(),
                    player.getUUID(),
                    entry.getKey()
            );
            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.justice.player_warrant.sentence_complete",
                    CapitalDiplomaticAgreementText.capitalName(
                            player.serverLevel(),
                            capital
                    )
            ));
        }
    }

    private void notifyWantedEntries(ServerPlayer player) {
        Set<UUID> warrantCapitals = CapitalPlayerWarrantDataAccess.getWarrantCapitals(
                player.serverLevel(),
                player.getUUID()
        );
        Set<String> activeKeys = new HashSet<>();

        for (UUID capitalId : warrantCapitals) {
            CapitalRecord capital = CapitalManager.getCapital(capitalId);
            if (capital == null || capital.getState() != CapitalState.ACTIVE) {
                continue;
            }

            String key = player.getUUID() + "|" + capitalId;
            if (CapitalPlayerNotificationService.isPlayerWithinCapital(
                    player.serverLevel(),
                    capital,
                    player
            )) {
                activeKeys.add(key);
                if (notifiedWantedEntries.add(key)) {
                    player.sendSystemMessage(Component.translatable(
                            "mcacapitals.justice.player_warrant.wanted_entry",
                            CapitalDiplomaticAgreementText.capitalName(
                                    player.serverLevel(),
                                    capital
                            )
                    ));
                }
            }
        }

        String playerPrefix = player.getUUID() + "|";
        notifiedWantedEntries.removeIf(key ->
                key.startsWith(playerPrefix) && !activeKeys.contains(key)
        );
    }
}