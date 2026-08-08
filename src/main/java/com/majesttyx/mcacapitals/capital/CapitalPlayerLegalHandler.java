package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalPlayerWarrantDataAccess;
import com.majesttyx.mcacapitals.data.CapitalPlayerWarrantSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CapitalPlayerLegalHandler {
    private final Set<String> notifiedWantedEntries = new HashSet<>();

    public void onPlayerTick(ServerPlayer player) {
        if (player == null) {
            return;
        }

        if (player.tickCount % 20 == 0) {
            PlayerCapitalAllegianceService.synchronizePlayerSovereignDeclaration(player);
            refreshForeignStorageIncident(player);
        }
        tickLeaveOrders(player);
        tickSentences(player);
        notifyWantedEntries(player);
    }

    private void tickLeaveOrders(ServerPlayer player) {
        Map<UUID, CapitalPlayerWarrantSavedData.LeaveOrder> orders =
                CapitalPlayerWarrantDataAccess.getLeaveOrders(player.serverLevel(), player.getUUID());
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
                player.sendSystemMessage(Component.literal(
                        CapitalDiplomaticAgreementText.capitalName(player.serverLevel(), capital)
                                + " has issued a warrant because you failed to leave within two real-time minutes."
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
            player.sendSystemMessage(Component.literal(
                    "Your five-minute sentence in "
                            + CapitalDiplomaticAgreementText.capitalName(player.serverLevel(), capital)
                            + " is complete. Its warrant has been cleared."
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
                    player.sendSystemMessage(Component.literal(
                            "You are wanted by "
                                    + CapitalDiplomaticAgreementText.capitalName(player.serverLevel(), capital)
                                    + ". Its court orders you to leave."
                    ));
                }
            }
        }
        String playerPrefix = player.getUUID() + "|";
        notifiedWantedEntries.removeIf(key -> key.startsWith(playerPrefix) && !activeKeys.contains(key));
    }

    private void refreshForeignStorageIncident(ServerPlayer player) {
        try {
            Class<?> service = Class.forName(
                    "com.majesttyx.mcacapitals.capital.CapitalForeignStorageRaidService"
            );
            Method method = service.getMethod("refreshIncident", ServerPlayer.class);
            method.invoke(null, player);
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException ignored) {
        }
    }
}