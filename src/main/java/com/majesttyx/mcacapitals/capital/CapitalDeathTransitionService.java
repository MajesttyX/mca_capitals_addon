package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.PendingVillagerBetrothalAccess;
import com.majesttyx.mcacapitals.data.PlayerCapitalTitleSavedData;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleRecord;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CapitalDeathTransitionService {

    private static final Map<UUID, String> RECENT_DEATH_DISPLAY_NAMES = new ConcurrentHashMap<>();

    private CapitalDeathTransitionService() {
    }

    public static void handleVillagerDeath(ServerLevel level, UUID deadId) {
        handleVillagerDeath(level, deadId, null);
    }

    public static void handleVillagerDeath(ServerLevel level, LivingEntity deadEntity) {
        if (deadEntity == null) {
            return;
        }

        handleVillagerDeath(level, deadEntity.getUUID(), deadEntity);
    }

    public static String getRecentDeathDisplayName(UUID id) {
        if (id == null) {
            return null;
        }

        return RECENT_DEATH_DISPLAY_NAMES.get(id);
    }

    private static void handleVillagerDeath(ServerLevel level, UUID deadId, Entity deadEntity) {
        if (level == null || deadId == null) {
            return;
        }

        String bestDeathDisplayName = resolveBestDeathDisplayName(level, deadId, deadEntity);
        if (bestDeathDisplayName != null && !bestDeathDisplayName.isBlank()) {
            RECENT_DEATH_DISPLAY_NAMES.put(deadId, bestDeathDisplayName);
        }

        boolean changedAny = false;

        PendingVillagerBetrothalAccess.removeVillager(level, deadId);

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }

            boolean changed = false;
            RoyalDeathRole deathRole = resolveRoyalDeathRole(capital, deadId);

            if (deathRole != RoyalDeathRole.NONE) {
                String displayName = resolveDeathDisplayName(level, capital, deadId, deadEntity);

                CapitalChronicleService.addEntry(
                        level,
                        capital,
                        displayName + " died."
                );

                if (deathRole.triggersMourning()) {
                    CapitalMourningService.startMourning(level, capital, displayName + " died.");
                }

                if (deathRole == RoyalDeathRole.CONSORT) {
                    capital.setConsort(null);
                    capital.setConsortFemale(false);
                    changed = true;
                }

                if (deathRole == RoyalDeathRole.HEIR) {
                    capital.setHeir(null);
                    capital.setHeirFemale(false);
                    capital.setHeirMode(CapitalRecord.HeirMode.NONE);
                    changed = true;
                }
            }

            for (UUID holder : Set.copyOf(capital.getPrinceConsortSources().keySet())) {
                UUID source = capital.getPrinceConsortSource(holder);
                if (!deadId.equals(source)) {
                    continue;
                }

                boolean holderFemale = capital.isPrinceConsortFemale(holder);
                capital.removePrinceConsortSource(holder);
                capital.setDowagerPrinceSource(holder, deadId, holderFemale);
                changed = true;
            }

            for (UUID holder : Set.copyOf(capital.getMarriageDukeSources().keySet())) {
                UUID source = capital.getMarriageDukeSource(holder);
                if (!deadId.equals(source)) {
                    continue;
                }

                boolean holderFemale = capital.isMarriageDukeFemale(holder);
                capital.removeMarriageDukeSource(holder);
                capital.setDowagerDukeSource(holder, deadId, holderFemale);
                changed = true;
            }

            List<PlayerCapitalTitleRecord> playerRecords = new ArrayList<>(PlayerCapitalTitleSavedData.get(level).getRecords().values());
            for (PlayerCapitalTitleRecord record : playerRecords) {
                if (record == null) {
                    continue;
                }
                if (!capital.getCapitalId().equals(record.getCapitalId())) {
                    continue;
                }
                if (!deadId.equals(record.getMarriageSourceSpouseId())) {
                    continue;
                }

                PlayerCapitalTitleService.transitionMarriageToDowager(level, capital, record.getPlayerId(), deadId);
                changed = true;
            }

            if (changed) {
                changedAny = true;
                CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
            }
        }

        PlayerCapitalTitleService.clearMarriageTitlesFromDeadSpouse(level, deadId);

        if (changedAny) {
            CapitalDataAccess.markDirty(level);
        }
    }

    private static RoyalDeathRole resolveRoyalDeathRole(CapitalRecord capital, UUID deadId) {
        if (capital == null || deadId == null) {
            return RoyalDeathRole.NONE;
        }

        if (deadId.equals(capital.getSovereign())) {
            return RoyalDeathRole.SOVEREIGN;
        }

        if (deadId.equals(capital.getConsort())) {
            return RoyalDeathRole.CONSORT;
        }

        if (deadId.equals(capital.getHeir())) {
            return RoyalDeathRole.HEIR;
        }

        if (deadId.equals(capital.getDowager())) {
            return RoyalDeathRole.ROYAL_FAMILY;
        }

        if (capital.isRoyalChild(deadId) || capital.isLegitimizedRoyalChild(deadId)) {
            return RoyalDeathRole.ROYAL_FAMILY;
        }

        if (capital.isPrinceConsort(deadId) || capital.isDowagerPrince(deadId)) {
            return RoyalDeathRole.ROYAL_FAMILY;
        }

        if (capital.isRoyalHouseholdMember(deadId)) {
            return RoyalDeathRole.ROYAL_FAMILY;
        }

        return RoyalDeathRole.NONE;
    }

    private static String resolveBestDeathDisplayName(ServerLevel level, UUID deadId, Entity deadEntity) {
        CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(level, deadId);
        if (capital != null) {
            return resolveDeathDisplayName(level, capital, deadId, deadEntity);
        }

        if (deadEntity != null) {
            return deadEntity.getName().getString();
        }

        return deadId.toString();
    }

    private static String resolveDeathDisplayName(ServerLevel level, CapitalRecord capital, UUID deadId, Entity deadEntity) {
        String title = CapitalTitleResolver.getDisplayTitle(level, capital, deadId);
        String rawName = deadEntity != null ? deadEntity.getName().getString() : null;

        if (rawName == null || rawName.isBlank()) {
            rawName = RECENT_DEATH_DISPLAY_NAMES.get(deadId);
        }

        if (rawName == null || rawName.isBlank()) {
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, deadId);
            if (entity != null) {
                rawName = entity.getName().getString();
            }
        }

        if (rawName == null || rawName.isBlank()) {
            rawName = deadId.toString();
        }

        if (title == null || title.isBlank() || "None".equals(title) || "Commoner".equals(title)) {
            return rawName;
        }

        if (alreadyStartsWithTitle(rawName, title)) {
            return rawName;
        }

        return title + " " + rawName;
    }

    private static boolean alreadyStartsWithTitle(String name, String title) {
        if (name == null || title == null) {
            return false;
        }

        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        String normalizedTitle = title.trim().toLowerCase(Locale.ROOT);

        return normalizedName.equals(normalizedTitle)
                || normalizedName.startsWith(normalizedTitle + " ");
    }

    private enum RoyalDeathRole {
        NONE(false),
        SOVEREIGN(true),
        CONSORT(true),
        HEIR(true),
        ROYAL_FAMILY(false);

        private final boolean triggersMourning;

        RoyalDeathRole(boolean triggersMourning) {
            this.triggersMourning = triggersMourning;
        }

        boolean triggersMourning() {
            return triggersMourning;
        }
    }
}