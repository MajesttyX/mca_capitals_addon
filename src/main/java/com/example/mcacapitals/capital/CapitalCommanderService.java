package com.example.mcacapitals.capital;

import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

public class CapitalCommanderService {

    public static final int REQUIRED_POPULATION = 30;

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
                String commanderName = resolveDisplayName(level, capital, newCommander);

                CapitalChronicleService.addEntry(
                        level,
                        capital,
                        commanderName + " was appointed Commander of the Royal Guard of " + villageName + "."
                );

                broadcastCommanderAppointment(level, capital, villageName, commanderName);
                changed = true;
            }
        }

        if (previousCommander != null && capital.getCommander() == null && playerCommander == null) {
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    "The office of Commander of the Royal Guard stands vacant in "
                            + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
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
            String formerName = resolveDisplayName(level, capital, capital.getCommander());
            capital.setCommander(null);
            capital.setCommanderFemale(false);
            CapitalChronicleService.addEntry(
                    level,
                    capital,
                    formerName + " was relieved of the office of Commander of the Royal Guard of " + villageName + "."
            );
        }

        PlayerCapitalTitleService.revokeCommanderForCapital(level, capital);
        PlayerCapitalTitleService.grantCommander(level, capital, player.getUUID());

        String commanderName = resolveDisplayName(level, capital, player.getUUID());
        CapitalChronicleService.addEntry(
                level,
                capital,
                commanderName + " was appointed Commander of the Royal Guard of " + villageName + "."
        );

        broadcastCommanderAppointment(level, capital, villageName, commanderName);
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
        String message = commanderName + " has been appointed Commander of the Royal Guard of " + villageName + ".";

        Integer villageId = capital.getVillageId();
        if (villageId == null) {
            return;
        }

        for (ServerPlayer serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            Integer playerVillage = MCAIntegrationBridge.getVillageIdForResident(level, serverPlayer.getUUID());
            if (playerVillage != null && playerVillage.equals(villageId)) {
                serverPlayer.sendSystemMessage(Component.literal(message));
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