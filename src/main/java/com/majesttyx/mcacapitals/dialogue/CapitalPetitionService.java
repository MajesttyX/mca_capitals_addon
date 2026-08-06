package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRankRequirements;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.identity.DecreeOfTheHouseService;
import com.majesttyx.mcacapitals.item.ModItems;
import com.majesttyx.mcacapitals.util.CapitalJusticeText;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class CapitalPetitionService {

    public static final String PETITION_THRONE = "mcacapitals_petition_throne";
    public static final String PETITION_SEIZE_THRONE = "mcacapitals_seize_throne";
    public static final String PETITION_COMMANDER = "mcacapitals_petition_commander";
    public static final String PETITION_HAND = "mcacapitals_petition_hand";
    public static final String PETITION_NOBLE_LORD = "mcacapitals_petition_noble_lord";
    public static final String PETITION_NOBLE_DUKE = "mcacapitals_petition_noble_duke";
    public static final String PETITION_BETROTHAL = "mcacapitals_petition_betrothal";
    public static final String PETITION_BETROTHAL_RECOMMEND = "mcacapitals_petition_betrothal_recommend";
    public static final String REQUEST_DECREE_OF_THE_HOUSE = "mcacapitals_request_decree_of_the_house";
    public static final String REQUEST_ACCUSATION = "mcacapitals_accuse_enemy";
    public static final String REQUEST_ROYAL_PARDON = "mcacapitals_request_royal_pardon";

    private static final int THRONE_PETITION_MIN_POPULATION = 25;
    private static final int THRONE_PETITION_MIN_HEARTS = 2500;

    private static final int SEIZURE_MIN_REPUTATION = 1500;
    private static final int SEIZURE_COMMANDER_HEARTS = 200;

    private static final int ROYAL_PARDON_MIN_HEARTS = 85;

    private static final double MAX_AUDIENCE_DISTANCE_SQR = 12.0D * 12.0D;
    private static final ResourceLocation COVER_ME_IN_DIAMONDS_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "story/shiny_gear");

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
                () -> handleHandPetition(player, villagerEntity),
                () -> handleLordPetition(player, villagerEntity),
                () -> handleDukePetition(player, villagerEntity),
                () -> handleBetrothalPetition(player, villagerEntity),
                () -> handleBetrothalPetition(player, villagerEntity),
                () -> handleDecreeOfTheHouseRequest(player, villagerEntity),
                () -> handleAccusationRequest(player, villagerEntity),
                () -> handleRoyalPardonRequest(player, villagerEntity)
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
                CapitalRankRequirements.LORD_COMMANDER_POPULATION,
                CapitalRankRequirements.LORD_COMMANDER_REPUTATION,
                MAX_AUDIENCE_DISTANCE_SQR
        );
    }

    private static void handleHandPetition(ServerPlayer player, Entity villagerEntity) {
        CapitalPetitionTitleActions.handleHandPetition(
                player,
                villagerEntity,
                CapitalRankRequirements.HAND_CAPITAL_REPUTATION,
                CapitalRankRequirements.HAND_SOVEREIGN_REPUTATION,
                MAX_AUDIENCE_DISTANCE_SQR
        );
    }

    private static void handleLordPetition(ServerPlayer player, Entity villagerEntity) {
        CapitalPetitionTitleActions.handleLordPetition(
                player,
                villagerEntity,
                CapitalRankRequirements.LORD_REPUTATION,
                CapitalRankRequirements.LORD_MASTER_PROFESSIONALS,
                MAX_AUDIENCE_DISTANCE_SQR
        );
    }

    private static void handleDukePetition(ServerPlayer player, Entity villagerEntity) {
        CapitalPetitionTitleActions.handleDukePetition(
                player,
                villagerEntity,
                CapitalRankRequirements.DUKE_REPUTATION,
                CapitalRankRequirements.DUKE_POPULATION,
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

    private static void handleDecreeOfTheHouseRequest(ServerPlayer player, Entity villagerEntity) {
        if (!CapitalPetitionRequirements.isAudienceValid(player, villagerEntity, MAX_AUDIENCE_DISTANCE_SQR)) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED
            );
            return;
        }

        ItemStack decree = DecreeOfTheHouseService.createFreshDecree();
        boolean inserted = player.addItem(decree);
        if (!inserted) {
            player.drop(decree, false);
        }

        player.sendSystemMessage(Component.literal(
                villagerEntity.getName().getString() + ": The records of a House are not changed lightly. Take this Decree and use it carefully."
        ));
    }
    private static void handleAccusationRequest(ServerPlayer player, Entity villagerEntity) {
        CapitalJusticeService.openAccusationSelection(player, villagerEntity);
    }

    private static void handleRoyalPardonRequest(ServerPlayer player, Entity villagerEntity) {
        if (!CapitalPetitionRequirements.isAudienceValid(player, villagerEntity, MAX_AUDIENCE_DISTANCE_SQR)) {
            CapitalPetitionDialogueHelper.sendDialogueKeyAndClose(
                    player,
                    villagerEntity,
                    CapitalDialogueKey.PETITION_AUDIENCE_REQUIRED
            );
            return;
        }

        ServerLevel level = player.serverLevel();
        UUID villagerId = villagerEntity.getUUID();
        CapitalRecord capital = resolveCapital(level, villagerId);
        if (capital == null
                || capital.getState() != CapitalState.ACTIVE
                || !canGrantRoyalPardon(capital, villagerId)) {
            player.sendSystemMessage(Component.literal(
                    villagerEntity.getName().getString() + ": "
                            + CapitalJusticeText.royalPardonNoAuthority(level, villagerId)
            ));
            return;
        }

        int hearts = MCAIntegrationBridge.getHeartsWithPlayer(
                level,
                villagerId,
                player.getUUID()
        );
        if (hearts < ROYAL_PARDON_MIN_HEARTS) {
            player.sendSystemMessage(Component.literal(
                    villagerEntity.getName().getString() + ": "
                            + CapitalJusticeText.royalPardonRefusedTrust(level, villagerId)
            ));
            return;
        }

        ItemStack pardon = new ItemStack(ModItems.ROYAL_PARDON.get());
        boolean inserted = player.addItem(pardon);
        if (!inserted) {
            player.drop(pardon, false);
        }
        player.sendSystemMessage(Component.literal(
                villagerEntity.getName().getString() + ": "
                        + CapitalJusticeText.royalPardonGrantLine(level, capital, villagerId)
        ));
    }

    private static boolean canGrantRoyalPardon(CapitalRecord capital, UUID villagerId) {
        return villagerId != null
                && (villagerId.equals(capital.getSovereign())
                || villagerId.equals(capital.getHand())
                || villagerId.equals(capital.getMasterOfLaws()));
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID villagerId) {
        CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(level, villagerId);
        if (capital != null) {
            return capital;
        }
        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerId);
        return CapitalManager.getCapitalByVillageId(villageId);
    }

}