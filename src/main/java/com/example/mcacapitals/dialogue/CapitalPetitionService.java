package com.example.mcacapitals.dialogue;

import com.example.mcacapitals.MCACapitals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class CapitalPetitionService {

    public static final String PETITION_THRONE = "mcacapitals_petition_throne";
    public static final String PETITION_SEIZE_THRONE = "mcacapitals_seize_throne";
    public static final String PETITION_COMMANDER = "mcacapitals_petition_commander";
    public static final String PETITION_NOBLE_LORD = "mcacapitals_petition_noble_lord";
    public static final String PETITION_NOBLE_DUKE = "mcacapitals_petition_noble_duke";
    public static final String PETITION_BETROTHAL = "mcacapitals_petition_betrothal";
    public static final String PETITION_BETROTHAL_RECOMMEND = "mcacapitals_petition_betrothal_recommend";

    private static final int THRONE_PETITION_MIN_POPULATION = 35;
    private static final int THRONE_PETITION_MIN_HEARTS = 2500;

    private static final int SEIZURE_MIN_REPUTATION = 1500;
    private static final int SEIZURE_COMMANDER_HEARTS = 200;

    private static final int COMMANDER_PETITION_MIN_POPULATION = 30;
    private static final int COMMANDER_PETITION_MIN_HEARTS = 1000;

    private static final int LORD_PETITION_MIN_HEARTS = 400;
    private static final int LORD_PETITION_MIN_MASTER_VILLAGERS = 3;

    private static final int DUKE_PETITION_MIN_HEARTS = 1500;
    private static final int DUKE_PETITION_MIN_POPULATION = 30;

    private static final double MAX_AUDIENCE_DISTANCE_SQR = 12.0D * 12.0D;
    private static final ResourceLocation COVER_ME_IN_DIAMONDS_ID = new ResourceLocation("minecraft", "story/shiny_gear");

    private CapitalPetitionService() {
    }

    public static boolean handleCustomCommand(ServerPlayer player, Entity villagerEntity, String command) {
        MCACapitals.LOGGER.info(
                "[MCACapitals] handleCustomCommand called. command='{}', player='{}', villager='{}'",
                command,
                player != null ? player.getName().getString() : "null",
                villagerEntity != null ? villagerEntity.getName().getString() : "null"
        );

        if (player == null || villagerEntity == null || command == null) {
            return false;
        }

        return CapitalPetitionCommandRouter.route(
                command,
                () -> handleThronePetition(player, villagerEntity),
                () -> handleSeizeThrone(player, villagerEntity),
                () -> handleCommanderPetition(player, villagerEntity),
                () -> handleLordPetition(player, villagerEntity),
                () -> handleDukePetition(player, villagerEntity),
                () -> handleBetrothalPetition(player, villagerEntity),
                () -> handleBetrothalPetition(player, villagerEntity)
        );
    }

    public static boolean handleBetrothalSelection(ServerPlayer player, UUID capitalId, UUID targetId) {
        return CapitalPetitionBetrothalActions.handleBetrothalSelection(player, capitalId, targetId);
    }

    public static boolean handleRecommendedBetrothalSelection(ServerPlayer player, UUID capitalId, UUID firstId, UUID secondId) {
        return CapitalPetitionBetrothalActions.handleRecommendedBetrothalSelection(player, capitalId, firstId, secondId);
    }

    private static void handleThronePetition(ServerPlayer player, Entity villagerEntity) {
        CapitalPetitionSovereignActions.handleThronePetition(
                player,
                villagerEntity,
                THRONE_PETITION_MIN_POPULATION,
                THRONE_PETITION_MIN_HEARTS,
                MAX_AUDIENCE_DISTANCE_SQR
        );
    }

    private static void handleSeizeThrone(ServerPlayer player, Entity villagerEntity) {
        CapitalPetitionSovereignActions.handleSeizeThrone(
                player,
                villagerEntity,
                SEIZURE_MIN_REPUTATION,
                SEIZURE_COMMANDER_HEARTS,
                MAX_AUDIENCE_DISTANCE_SQR,
                COVER_ME_IN_DIAMONDS_ID
        );
    }

    private static void handleCommanderPetition(ServerPlayer player, Entity villagerEntity) {
        CapitalPetitionTitleActions.handleCommanderPetition(
                player,
                villagerEntity,
                COMMANDER_PETITION_MIN_POPULATION,
                COMMANDER_PETITION_MIN_HEARTS,
                MAX_AUDIENCE_DISTANCE_SQR
        );
    }

    private static void handleLordPetition(ServerPlayer player, Entity villagerEntity) {
        CapitalPetitionTitleActions.handleLordPetition(
                player,
                villagerEntity,
                LORD_PETITION_MIN_HEARTS,
                LORD_PETITION_MIN_MASTER_VILLAGERS,
                MAX_AUDIENCE_DISTANCE_SQR
        );
    }

    private static void handleDukePetition(ServerPlayer player, Entity villagerEntity) {
        CapitalPetitionTitleActions.handleDukePetition(
                player,
                villagerEntity,
                DUKE_PETITION_MIN_HEARTS,
                DUKE_PETITION_MIN_POPULATION,
                MAX_AUDIENCE_DISTANCE_SQR
        );
    }

    private static void handleBetrothalPetition(ServerPlayer player, Entity villagerEntity) {
        CapitalPetitionBetrothalActions.handleBetrothalPetition(
                player,
                villagerEntity,
                MAX_AUDIENCE_DISTANCE_SQR
        );
    }
}