package com.majesttyx.mcacapitals.item;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCourtWatcher;
import com.majesttyx.mcacapitals.capital.CapitalFoundationService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
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

public class RoyalDisinheritanceHandler {

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
        if (!held.is(ModItems.ROYAL_DISINHERITANCE.get())) {
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
            player.sendSystemMessage(Component.literal("Royal disinheritance can only be used on an MCA villager."));
            return InteractionResult.FAIL;
        }

        CapitalRecord capital = resolveCapital(level, targetId);
        if (capital == null) {
            player.sendSystemMessage(Component.literal("That villager is not part of a capital."));
            return InteractionResult.FAIL;
        }

        if (!capital.isRoyalChild(targetId) && !targetId.equals(capital.getHeir())) {
            player.sendSystemMessage(Component.literal("That villager has no royal claim to strip."));
            return InteractionResult.FAIL;
        }

        String displayName = stripKnownTitles(livingTarget.getName().getString());

        capital.disinheritRoyalChild(targetId);
        CapitalFoundationService.refreshCourt(level, capital);
        CapitalCourtWatcher.clearFingerprint(capital.getCapitalId());

        CapitalChronicleService.addEntry(level, capital,
                displayName + " was disinherited and stripped of royal claim in "
                        + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");

        player.sendSystemMessage(Component.literal(
                "By royal decree, " + displayName + " is disinherited and stripped of all royal claim."
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
            if (capital.isRoyalChild(targetId) || targetId.equals(capital.getHeir())) {
                return capital;
            }
        }

        return null;
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