package com.majesttyx.mcacapitals.mixin;

import com.majesttyx.mcacapitals.capital.CapitalChronicleEventId;
import com.majesttyx.mcacapitals.capital.CapitalChronicleIdentitySnapshot;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.identity.MarriageSurnameService;
import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.UUID;

@Pseudo
@Mixin(targets = "fabric.net.conczin.mca.server.world.data.PlayerSaveData", remap = false)
public class PlayerSaveDataMarriageMixin {

    @Inject(
            method = "marry(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void mcacapitals$onPlayerMarry(Entity spouse, CallbackInfo ci) {
        if (spouse == null) {
            return;
        }
        if (!(spouse.level() instanceof ServerLevel level)) {
            return;
        }

        UUID playerUuid = resolvePlayerUuid();
        if (playerUuid == null) {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
        if (player == null) {
            return;
        }

        MarriageSurnameService.onPlayerMarriage(level, player, spouse);

        PlayerCapitalTitleService.clearAllMarriageDerivedStateForRemarriage(level, playerUuid);

        UUID spouseId = spouse.getUUID();

        CapitalRecord sovereignCapital = CapitalManager.getCapitalBySovereign(spouseId);
        if (sovereignCapital != null) {
            handleSovereignMarriage(level, sovereignCapital, spouse, player);
            return;
        }

        CapitalRecord princeCapital = findCapitalByPrince(spouseId);
        if (princeCapital != null) {
            handleMarriageTitle(level, princeCapital, spouse, player, resolveMarriagePrinceTitle(level, player));
            return;
        }

        CapitalRecord ducalCapital = findCapitalByDuke(spouseId);
        if (ducalCapital != null) {
            handleMarriageTitle(level, ducalCapital, spouse, player, resolveMarriageDukeTitle(level, player));
            return;
        }

        CapitalRecord lordlyCapital = findCapitalByLord(spouseId);
        if (lordlyCapital != null) {
            handleMarriageTitle(level, lordlyCapital, spouse, player, resolveMarriageLordTitle(level, player));
        }
    }

    private static void handleSovereignMarriage(ServerLevel level, CapitalRecord capital, Entity spouse, ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        UUID previousConsort = capital.getConsort();

        capital.setConsort(playerUuid);
        capital.setConsortFemale(resolvePlayerFemale(level, player));
        capital.setPlayerConsort(true);
        capital.setPlayerConsortId(playerUuid);
        capital.setPlayerConsortName(player.getGameProfile().getName());

        String spouseName = CapitalChronicleIdentitySnapshot.name(level, capital, spouse.getUUID());
        String playerName = player.getGameProfile().getName();

        if (!playerUuid.equals(previousConsort) && !hasMarriageEntry(capital, spouseName, playerName)) {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.ROYAL_MARRIAGE,
                    spouseName,
                    playerName,
                    CapitalChronicleIdentitySnapshot.title(level, capital, spouse.getUUID()),
                    CapitalChronicleIdentitySnapshot.style(level, capital, spouse.getUUID()),
                    CapitalChronicleIdentitySnapshot.title(level, capital, playerUuid),
                    CapitalChronicleIdentitySnapshot.style(level, capital, playerUuid)
            );
        }

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    private static void handleMarriageTitle(ServerLevel level, CapitalRecord capital, Entity spouse, ServerPlayer player, NobleTitle marriageTitle) {
        if (marriageTitle == null || marriageTitle == NobleTitle.COMMONER) {
            return;
        }

        UUID playerUuid = player.getUUID();
        UUID spouseId = spouse.getUUID();

        PlayerCapitalTitleService.grantMarriageTitle(level, capital, playerUuid, spouseId, marriageTitle);

        String spouseName = CapitalChronicleIdentitySnapshot.name(level, capital, spouse.getUUID());
        String playerName = player.getGameProfile().getName();

        if (!hasMarriageEntry(capital, spouseName, playerName)) {
            CapitalChronicleService.addEvent(
                    level,
                    capital,
                    CapitalChronicleEventId.ROYAL_MARRIAGE,
                    spouseName,
                    playerName,
                    CapitalChronicleIdentitySnapshot.title(level, capital, spouseId),
                    CapitalChronicleIdentitySnapshot.style(level, capital, spouseId),
                    CapitalChronicleIdentitySnapshot.title(level, capital, playerUuid),
                    CapitalChronicleIdentitySnapshot.style(level, capital, playerUuid)
            );
        }

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
    }

    private UUID resolvePlayerUuid() {
        try {
            Method method = this.getClass().getMethod("getUUID");
            Object result = method.invoke(this);
            if (result instanceof UUID uuid) {
                return uuid;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean resolvePlayerFemale(ServerLevel level, ServerPlayer player) {
        try {
            Class<?> bridge = Class.forName("com.majesttyx.mcacapitals.util.MCAPlayerBridge");
            Method method = bridge.getDeclaredMethod("isPlayerFemale", ServerLevel.class, ServerPlayer.class);
            method.setAccessible(true);
            Object result = method.invoke(null, level, player);
            if (result instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static NobleTitle resolveMarriagePrinceTitle(ServerLevel level, ServerPlayer player) {
        return resolvePlayerFemale(level, player) ? NobleTitle.PRINCESS : NobleTitle.PRINCE;
    }

    private static NobleTitle resolveMarriageDukeTitle(ServerLevel level, ServerPlayer player) {
        return resolvePlayerFemale(level, player) ? NobleTitle.DUCHESS : NobleTitle.DUKE;
    }

    private static NobleTitle resolveMarriageLordTitle(ServerLevel level, ServerPlayer player) {
        return resolvePlayerFemale(level, player) ? NobleTitle.LADY : NobleTitle.LORD;
    }

    private static CapitalRecord findCapitalByPrince(UUID spouseId) {
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }
            if (spouseId.equals(capital.getHeir())) {
                return capital;
            }
            if (capital.isRoyalChild(spouseId) || capital.isLegitimizedRoyalChild(spouseId)) {
                return capital;
            }
        }
        return null;
    }

    private static CapitalRecord findCapitalByDuke(UUID spouseId) {
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && capital.isDuke(spouseId)) {
                return capital;
            }
        }
        return null;
    }

    private static CapitalRecord findCapitalByLord(UUID spouseId) {
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && capital.isLord(spouseId)) {
                return capital;
            }
        }
        return null;
    }

    private static boolean hasMarriageEntry(CapitalRecord capital, String spouseName, String playerName) {
        return CapitalChronicleService.hasMarriageEvent(
                capital,
                CapitalChronicleEventId.ROYAL_MARRIAGE,
                spouseName,
                playerName,
                null
        );
    }
}