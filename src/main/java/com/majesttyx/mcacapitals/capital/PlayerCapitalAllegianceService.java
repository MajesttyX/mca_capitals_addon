package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import com.majesttyx.mcacapitals.data.PlayerCapitalAllegianceDataAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class PlayerCapitalAllegianceService {

    public static final long CHANGE_COOLDOWN_DAYS = 5L;

    private PlayerCapitalAllegianceService() {
    }

    public static CapitalRecord getDeclaredCapital(ServerLevel level, UUID playerId) {
        UUID capitalId = getDeclaredCapitalId(level, playerId);
        return capitalId == null ? null : CapitalManager.getCapital(capitalId);
    }

    public static UUID getDeclaredCapitalId(ServerLevel level, UUID playerId) {
        if (level == null || playerId == null) {
            return null;
        }

        UUID capitalId = PlayerCapitalAllegianceDataAccess.getDeclaredCapitalId(
                level,
                playerId
        );
        if (capitalId == null) {
            return null;
        }

        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            PlayerCapitalAllegianceDataAccess.clearDeclaration(level, playerId);
            return null;
        }

        return capitalId;
    }

    public static DeclarationResult declare(
            ServerPlayer player,
            CapitalRecord target
    ) {
        if (player == null || target == null || target.getCapitalId() == null) {
            return DeclarationResult.failure(
                    Component.translatable(
                            "mcacapitals.system.declaration.invalid_capital"
                    )
            );
        }

        ServerLevel level = player.serverLevel();
        if (target.getState() != CapitalState.ACTIVE) {
            return DeclarationResult.failure(
                    Component.translatable(
                            "mcacapitals.system.declaration.inactive_capital"
                    )
            );
        }

        UUID playerId = player.getUUID();
        List<CapitalRecord> sovereignCapitals = getPlayerSovereignCapitals(playerId);
        if (!sovereignCapitals.isEmpty()
                && sovereignCapitals.stream().noneMatch(capital ->
                target.getCapitalId().equals(capital.getCapitalId()))) {
            return DeclarationResult.failure(
                    Component.translatable(
                            "mcacapitals.system.declaration.sovereign_other_crown"
                    )
            );
        }

        UUID currentId = getDeclaredCapitalId(level, playerId);
        if (target.getCapitalId().equals(currentId)) {
            return DeclarationResult.failure(
                    Component.translatable(
                            "mcacapitals.system.declaration.already_declared",
                            CapitalDiplomaticAgreementText.capitalName(level, target)
                    )
            );
        }

        long currentDay = CapitalWarDataAccess.currentDay(level);
        if (currentId != null) {
            long lastChangeDay = PlayerCapitalAllegianceDataAccess.getLastChangeDay(
                    level,
                    playerId
            );
            long availableDay = lastChangeDay + CHANGE_COOLDOWN_DAYS;
            if (currentDay < availableDay) {
                long remaining = availableDay - currentDay;
                return DeclarationResult.failure(
                        Component.translatable(
                                remaining == 1L
                                        ? "mcacapitals.system.declaration.cooldown.one"
                                        : "mcacapitals.system.declaration.cooldown.many",
                                remaining
                        )
                );
            }
        }

        PlayerCapitalAllegianceDataAccess.setDeclaration(
                level,
                playerId,
                target.getCapitalId(),
                currentDay
        );

        return DeclarationResult.success(
                Component.translatable(
                        "mcacapitals.system.declaration.success",
                        CapitalDiplomaticAgreementText.capitalName(level, target)
                )
        );
    }

    public static void synchronizePlayerSovereignDeclaration(ServerPlayer player) {
        if (player == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        List<CapitalRecord> sovereignCapitals = getPlayerSovereignCapitals(player.getUUID());
        if (sovereignCapitals.isEmpty()) {
            return;
        }

        UUID currentId = getDeclaredCapitalId(level, player.getUUID());
        if (currentId != null && sovereignCapitals.stream().anyMatch(capital ->
                currentId.equals(capital.getCapitalId()))) {
            return;
        }

        CapitalRecord selected = sovereignCapitals.get(0);
        PlayerCapitalAllegianceDataAccess.setDeclaration(
                level,
                player.getUUID(),
                selected.getCapitalId(),
                CapitalWarDataAccess.currentDay(level)
        );
    }

    public static boolean canOfferDeclaration(
            ServerPlayer player,
            CapitalRecord capital
    ) {
        if (player == null
                || capital == null
                || capital.getState() != CapitalState.ACTIVE
                || capital.getCapitalId() == null) {
            return false;
        }

        UUID current = getDeclaredCapitalId(player.serverLevel(), player.getUUID());
        if (capital.getCapitalId().equals(current)) {
            return false;
        }

        List<CapitalRecord> sovereignCapitals = getPlayerSovereignCapitals(player.getUUID());
        return sovereignCapitals.isEmpty()
                || sovereignCapitals.stream().anyMatch(sovereign ->
                capital.getCapitalId().equals(sovereign.getCapitalId()));
    }

    private static List<CapitalRecord> getPlayerSovereignCapitals(UUID playerId) {
        List<CapitalRecord> result = new ArrayList<>();
        if (playerId == null) {
            return result;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && capital.getState() == CapitalState.ACTIVE
                    && playerId.equals(capital.getPlayerSovereignId())) {
                result.add(capital);
            }
        }

        result.sort(Comparator.comparing(capital -> capital.getCapitalId().toString()));
        return result;
    }

    public record DeclarationResult(boolean successful, Component message) {
        private static DeclarationResult success(Component message) {
            return new DeclarationResult(true, message);
        }

        private static DeclarationResult failure(Component message) {
            return new DeclarationResult(false, message);
        }
    }
}
