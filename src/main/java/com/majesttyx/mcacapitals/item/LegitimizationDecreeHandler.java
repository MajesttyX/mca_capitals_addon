package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class LegitimizationDecreeHandler {

    private static final String[] KNOWN_TITLES = new String[] {
            "High Queen",
            "High King",
            "Dowager Queen",
            "Dowager King",
            "Queen Consort",
            "King Consort",
            "Heir Apparent",
            "Crown Princess",
            "Crown Prince",
            "Princess Consort",
            "Prince Consort",
            "Princess",
            "Prince",
            "Duchess",
            "Duke",
            "Lady",
            "Lord",
            "Commander",
            "Dame",
            "Sir",
            "Queen",
            "King"
    };

    public static InteractionResult handleEntityInteract(Player player, Entity rawTarget, InteractionHand hand) {
        if (player == null || rawTarget == null || hand == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.LEGITIMIZATION_DECREE.get())) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!(rawTarget instanceof LivingEntity livingTarget)) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        UUID targetId = livingTarget.getUUID();

        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            player.sendSystemMessage(Component.literal("Legitimization may only be granted to an MCA villager."));
            return InteractionResult.FAIL;
        }

        CapitalRecord capital = resolveCapital(level, targetId);
        if (capital == null) {
            player.sendSystemMessage(Component.literal("That villager has no claim tied to any capital."));
            return InteractionResult.FAIL;
        }

        if (capital.getSovereign() == null) {
            player.sendSystemMessage(Component.literal("That capital has no sovereign to grant legitimacy."));
            return InteractionResult.FAIL;
        }

        if (targetId.equals(capital.getSovereign())
                || targetId.equals(capital.getConsort())
                || targetId.equals(capital.getDowager())) {
            player.sendSystemMessage(Component.literal("That title cannot be granted through legitimization."));
            return InteractionResult.FAIL;
        }

        if (!isEligibleDynasticChild(level, capital, targetId)) {
            player.sendSystemMessage(Component.literal("That villager is not recognized as a child of this dynasty."));
            return InteractionResult.FAIL;
        }

        boolean female = MCAIntegrationBridge.isFemale(level, targetId);
        capital.addLegitimizedRoyalChild(targetId, female);

        if (!capital.getRoyalSuccessionOrder().contains(targetId)) {
            capital.getRoyalSuccessionOrder().add(targetId);
        }

        CapitalFoundationService.refreshCourt(level, capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());
        CapitalDataAccess.markDirty(level);

        String displayName = stripKnownTitles(livingTarget.getName().getString());
        String title = female ? "Princess" : "Prince";

        CapitalChronicleService.addEntry(level, capital,
                displayName + " was legitimized and recognized as " + title + " of "
                        + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");

        player.sendSystemMessage(Component.literal(
                displayName + " has been legitimized and recognized as " + title + "."
        ));

        return InteractionResult.SUCCESS;
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID targetId) {
        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, targetId);
        if (villageId != null) {
            CapitalRecord byVillage = CapitalManager.getCapitalByVillageId(villageId);
            if (byVillage != null) {
                return byVillage;
            }
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital.isRoyalChild(targetId)
                    || capital.isLegitimizedRoyalChild(targetId)
                    || MCAIntegrationBridge.isChildOf(level, targetId, capital.getSovereign())
                    || (capital.getDowager() != null && MCAIntegrationBridge.isChildOf(level, targetId, capital.getDowager()))) {
                return capital;
            }
        }

        return null;
    }

    private static boolean isEligibleDynasticChild(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (capital == null || capital.getSovereign() == null || targetId == null) {
            return false;
        }

        if (capital.isRoyalChild(targetId) || capital.isLegitimizedRoyalChild(targetId)) {
            return true;
        }

        if (MCAIntegrationBridge.isChildOf(level, targetId, capital.getSovereign())) {
            return true;
        }

        return capital.getDowager() != null && MCAIntegrationBridge.isChildOf(level, targetId, capital.getDowager());
    }

    private static String stripKnownTitles(String name) {
        if (name == null || name.isBlank()) {
            return "Unnamed";
        }

        String result = name.trim();

        for (String title : KNOWN_TITLES) {
            String prefix = title + " ";
            if (result.startsWith(prefix)) {
                return result.substring(prefix.length()).trim();
            }
        }

        return result;
    }
}