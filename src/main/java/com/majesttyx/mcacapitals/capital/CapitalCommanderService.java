package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

public class CapitalCommanderService {

    public static final int REQUIRED_POPULATION = 20;

    private CapitalCommanderService() {
    }

    public static boolean tickCommander(ServerLevel level, CapitalRecord capital, Set<UUID> residents) {
        if (level == null || capital == null) {
            return false;
        }

        boolean changed = false;
        UUID playerCommander = PlayerCapitalTitleService.getCommanderHolder(level, capital);

        UUID previousCommander = capital.getCommander();
        if (!CapitalCommanderSelection.isValidCommander(level, previousCommander, residents)) {
            if (previousCommander != null) {
                capital.setCommander(null);
                capital.setCommanderFemale(false);
                changed = true;
            }
        }

        if (playerCommander != null && capital.getCommander() != null) {
            capital.setCommander(null);
            capital.setCommanderFemale(false);
            changed = true;
        }

        if (playerCommander == null
                && capital.getCommander() == null
                && CapitalCommanderSelection.isEligibleForNewCommander(level, capital)) {
            UUID newCommander = CapitalCommanderSelection.findBestCommanderCandidate(level, capital, residents);
            if (newCommander != null) {
                capital.setCommander(newCommander);
                capital.setCommanderFemale(MCAIntegrationBridge.isFemale(level, newCommander));

                String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
                String commanderName = CapitalChronicleIdentitySnapshot.name(level, capital, newCommander);

                CapitalChronicleService.addEvent(
                        level, capital, CapitalChronicleEventId.ROYAL_GUARD_COMMANDER_APPOINTED,
                        commanderName, villageName
                );

                changed = true;
            }
        }

        if (previousCommander != null && capital.getCommander() == null && playerCommander == null) {
            CapitalChronicleService.addEvent(
                    level, capital, CapitalChronicleEventId.ROYAL_GUARD_COMMANDER_VACANT,
                    MCAIntegrationBridge.getVillageName(level, capital.getVillageId())
            );
        }

        Entity activeCommander = resolveActiveCommanderEntity(level, capital);
        if (activeCommander != null) {
            CapitalCommanderAuraService.tickCommanderAura(level, capital, activeCommander);
        }

        if (changed) {
            CapitalNameService.refreshCapitalNames(level, capital, residents);
            CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            CapitalDataAccess.markDirty(level);
        }

        return changed;
    }

    public static boolean isEligibleVillagerCommander(
            ServerLevel level,
            CapitalRecord capital,
            UUID villagerId,
            Set<UUID> residents
    ) {
        if (level == null
                || capital == null
                || villagerId == null
                || residents == null) {
            return false;
        }

        if (!CapitalCommanderSelection.isEligibleForNewCommander(
                level,
                capital
        )) {
            return false;
        }

        return CapitalCommanderSelection.isEligibleCandidate(
                level,
                capital,
                villagerId,
                residents
        );
    }

    public static UUID getPlayerCommander(ServerLevel level, CapitalRecord capital) {
        return PlayerCapitalTitleService.getCommanderHolder(level, capital);
    }

    public static boolean hasOtherPlayerCommander(ServerLevel level, CapitalRecord capital, UUID playerId) {
        UUID holder = getPlayerCommander(level, capital);
        return holder != null && !holder.equals(playerId);
    }

    public static boolean appointPlayerCommander(ServerLevel level, CapitalRecord capital, ServerPlayer player) {
        if (level == null || capital == null || player == null) {
            return false;
        }

        UUID existingPlayerCommander = getPlayerCommander(level, capital);
        if (existingPlayerCommander != null && !existingPlayerCommander.equals(player.getUUID())) {
            return false;
        }

        String villageName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());

        if (capital.getCommander() != null && !capital.getCommander().equals(player.getUUID())) {
            String formerName = CapitalChronicleIdentitySnapshot.name(level, capital, capital.getCommander());
            capital.setCommander(null);
            capital.setCommanderFemale(false);
            CapitalChronicleService.addEvent(
                    level, capital, CapitalChronicleEventId.ROYAL_GUARD_COMMANDER_RELIEVED,
                    formerName, villageName
            );
        }

        PlayerCapitalTitleService.revokeCommanderForCapital(level, capital);
        PlayerCapitalTitleService.grantCommander(level, capital, player.getUUID());

        String commanderName = CapitalChronicleIdentitySnapshot.name(level, capital, player.getUUID());
        CapitalChronicleService.addEvent(
                level, capital, CapitalChronicleEventId.ROYAL_GUARD_COMMANDER_APPOINTED,
                commanderName, villageName
        );

        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);
        return true;
    }

    private static Entity resolveActiveCommanderEntity(ServerLevel level, CapitalRecord capital) {
        if (capital.getCommander() != null) {
            Entity villagerCommander = MCAIntegrationBridge.getEntityByUuid(level, capital.getCommander());
            if (villagerCommander != null) {
                return villagerCommander;
            }
        }

        UUID playerCommander = PlayerCapitalTitleService.getCommanderHolder(level, capital);
        if (playerCommander != null) {
            return level.getServer().getPlayerList().getPlayer(playerCommander);
        }

        return null;
    }

    private static void broadcastCommanderAppointment(ServerLevel level, CapitalRecord capital, String villageName, String commanderName) {
        Component message = Component.translatable(
                "mcacapitals.system.commander.appointed_broadcast",
                commanderName,
                villageName
        );

        Integer villageId = capital.getVillageId();
        if (villageId == null) {
            return;
        }

        for (ServerPlayer serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            Integer playerVillage = MCAIntegrationBridge.getVillageIdForResident(level, serverPlayer.getUUID());
            if (playerVillage != null && playerVillage.equals(villageId)) {
                serverPlayer.sendSystemMessage(message);
            }
        }
    }

    private static String resolveDisplayName(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null) {
            return "Unknown";
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(entityId);
        if (player != null) {
            return player.getName().getString();
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, entityId);
        if (entity != null) {
            return entity.getName().getString();
        }

        UUID playerCommander = PlayerCapitalTitleService.getCommanderHolder(level, capital);
        if (playerCommander != null && playerCommander.equals(entityId)) {
            ServerPlayer offlineLookup = level.getServer().getPlayerList().getPlayer(playerCommander);
            if (offlineLookup != null) {
                return offlineLookup.getName().getString();
            }
        }

        return entityId.toString();
    }
}