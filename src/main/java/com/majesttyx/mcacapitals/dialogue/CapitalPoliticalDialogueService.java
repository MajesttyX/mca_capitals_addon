package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalCrownStandingService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.capital.CrownStanding;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CapitalPoliticalDialogueService {

    private static final int POLITICAL_HINT_MIN_HEARTS = 100;
    private static final int POLITICAL_PRIVATE_MIN_HEARTS = 200;
    private static final int POLITICAL_HINT_CHANCE = 20;
    private static final int POLITICAL_PRIVATE_CHANCE = 10;
    private static final long POLITICAL_BRANCH_COOLDOWN_TICKS = 24000L;

    private static final Map<String, Long> POLITICAL_CHAT_COOLDOWNS = new HashMap<>();

    private CapitalPoliticalDialogueService() {
    }

    public static String maybeResolvePoliticalDialogueId(ServerPlayer player, Entity villagerEntity) {
        if (player == null || villagerEntity == null) {
            return null;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villagerEntity)) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = villagerEntity.getUUID();
        CapitalRecord capital = resolveCapital(level, villagerId);
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            return null;
        }

        int hearts = MCAIntegrationBridge.getHeartsWithPlayer(level, villagerId, player.getUUID());
        if (hearts < POLITICAL_HINT_MIN_HEARTS) {
            return null;
        }

        long now = level.getGameTime();
        String cooldownKey = player.getUUID() + ":" + villagerId;
        long lastSpoken = POLITICAL_CHAT_COOLDOWNS.getOrDefault(cooldownKey, Long.MIN_VALUE);
        if (now - lastSpoken < POLITICAL_BRANCH_COOLDOWN_TICKS) {
            return null;
        }

        boolean privateDialogue = hearts >= POLITICAL_PRIVATE_MIN_HEARTS;
        int chance = privateDialogue ? POLITICAL_PRIVATE_CHANCE : POLITICAL_HINT_CHANCE;
        if (level.random.nextInt(100) >= chance) {
            return null;
        }

        CrownStanding standing = CapitalCrownStandingService.getStanding(level, capital, villagerId);
        POLITICAL_CHAT_COOLDOWNS.put(cooldownKey, now);

        if (privateDialogue) {
            return standing == CrownStanding.ENEMY_OF_CROWN
                    ? CapitalDialogueRuntime.POLITICAL_PRIVATE_ENEMY
                    : CapitalDialogueRuntime.POLITICAL_PRIVATE_FRIEND;
        }

        return standing == CrownStanding.ENEMY_OF_CROWN
                ? CapitalDialogueRuntime.POLITICAL_HINT_ENEMY
                : CapitalDialogueRuntime.POLITICAL_HINT_FRIEND;
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        CapitalRecord byTitle = CapitalTitleResolver.findCapitalForEntity(villagerId);
        if (byTitle != null) {
            return byTitle;
        }

        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        return CapitalManager.getCapitalByVillageId(level, villageId);
    }

    static void clearRuntimeState() {
        POLITICAL_CHAT_COOLDOWNS.clear();
    }

}