package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.MCACapitals;
import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class CapitalDialoguePersonalityResolver {

    private static final String DEFAULT_PERSONALITY = "friendly";

    private static final Set<String> SUPPORTED_PERSONALITIES = Set.of(
            "friendly",
            "flirty",
            "playful",
            "gloomy",
            "sensitive",
            "greedy",
            "odd",
            "crabby",
            "extroverted",
            "introverted",
            "relaxed",
            "anxious",
            "peaceful",
            "upbeat"
    );

    private static final Set<String> WARNED_PERSONALITIES = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> WARNED_ENTITIES = ConcurrentHashMap.newKeySet();

    private CapitalDialoguePersonalityResolver() {
    }

    static String resolve(Entity speaker) {
        if (!(speaker instanceof VillagerEntityMCA villager)) {
            if (speaker != null && WARNED_ENTITIES.add(speaker.getUUID())) {
                MCACapitals.LOGGER.warn(
                        "[MCACapitals] Skipping Capitals personality dialogue for non-MCA speaker {}",
                        speaker.getUUID()
                );
            }
            return null;
        }

        ResourceLocation personalityId = villager.getVillagerBrain().getPersonalityId();
        if (personalityId == null) {
            warnFallback(villager, "<missing>");
            return DEFAULT_PERSONALITY;
        }

        String path = personalityId.getPath();
        if (!MCA.MOD_ID.equals(personalityId.getNamespace()) || !SUPPORTED_PERSONALITIES.contains(path)) {
            warnFallback(villager, personalityId.toString());
            return DEFAULT_PERSONALITY;
        }

        return path;
    }

    static boolean isSupported(String personality) {
        return personality != null && SUPPORTED_PERSONALITIES.contains(personality);
    }

    static Set<String> supportedPersonalities() {
        return SUPPORTED_PERSONALITIES;
    }

    static String defaultPersonality() {
        return DEFAULT_PERSONALITY;
    }

    private static void warnFallback(VillagerEntityMCA villager, String personalityId) {
        String warningKey = personalityId == null ? "<missing>" : personalityId;
        if (WARNED_PERSONALITIES.add(warningKey)) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Unsupported or missing MCA personality '{}' on villager {}; using '{}' Capitals dialogue",
                    warningKey,
                    villager.getUUID(),
                    DEFAULT_PERSONALITY
            );
        }
    }

    static void clearWarningState() {
        WARNED_PERSONALITIES.clear();
        WARNED_ENTITIES.clear();
    }

}
