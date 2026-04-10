package com.example.mcacapitals.mixin;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.noble.NobleTitle;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@Pseudo
@Mixin(targets = "forge.net.mca.server.world.data.PlayerSaveData", remap = false)
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

        UUID spouseId = spouse.getUUID();

        CapitalRecord sovereignCapital = CapitalManager.getCapitalBySovereign(spouseId);
        if (sovereignCapital != null) {
            handleSovereignMarriage(level, sovereignCapital, spouse, player);
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

        String spouseName = spouse.getName().getString();
        String playerName = player.getGameProfile().getName();

        if (!playerUuid.equals(previousConsort) && !hasMarriageEntry(capital, spouseName, playerName)) {
            CapitalChronicleService.addEntry(level, capital, spouseName + " was married to " + playerName + ".");
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

        String spouseName = spouse.getName().getString();
        String playerName = player.getGameProfile().getName();

        if (!hasMarriageEntry(capital, spouseName, playerName)) {
            CapitalChronicleService.addEntry(level, capital, spouseName + " was married to " + playerName + ".");
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
            Class<?> bridge = Class.forName("com.example.mcacapitals.util.MCAPlayerBridge");
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

    private static NobleTitle resolveMarriageDukeTitle(ServerLevel level, ServerPlayer player) {
        return resolvePlayerFemale(level, player) ? NobleTitle.DUCHESS : NobleTitle.DUKE;
    }

    private static NobleTitle resolveMarriageLordTitle(ServerLevel level, ServerPlayer player) {
        return resolvePlayerFemale(level, player) ? NobleTitle.LADY : NobleTitle.LORD;
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
        String needle = spouseName + " was married to " + playerName + ".";
        List<String> entries = capital.getChronicleEntries();
        for (String entry : entries) {
            if (needle.equals(entry) || entry.endsWith(needle)) {
                return true;
            }
        }
        return false;
    }
}