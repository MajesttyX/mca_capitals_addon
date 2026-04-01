package com.example.mcacapitals.item;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.data.CapitalDataAccess;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;
import java.util.UUID;

public class LegitimizationDecreeHandler {

    private static final String[] KNOWN_TITLES = new String[] {
            "Queen Dowager",
            "Prince Consort",
            "Queen Consort",
            "King Consort",
            "Heir Apparent",
            "Princess",
            "Prince",
            "Duchess",
            "Duke",
            "Lady",
            "Lord",
            "Dame",
            "Sir",
            "Queen",
            "King"
    };

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity livingTarget)) {
            return;
        }

        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(ModItems.LEGITIMIZATION_DECREE.get())) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        UUID targetId = livingTarget.getUUID();

        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            player.sendSystemMessage(Component.literal("Legitimization may only be granted to an MCA villager."));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        CapitalRecord capital = resolveCapital(level, targetId);
        if (capital == null) {
            player.sendSystemMessage(Component.literal("That villager has no claim tied to any capital."));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (capital.getSovereign() == null) {
            player.sendSystemMessage(Component.literal("That capital has no sovereign to grant legitimacy."));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (targetId.equals(capital.getSovereign())
                || targetId.equals(capital.getConsort())
                || targetId.equals(capital.getDowager())) {
            player.sendSystemMessage(Component.literal("That title cannot be granted through legitimization."));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (!isEligibleDynasticChild(level, capital, targetId)) {
            player.sendSystemMessage(Component.literal("That villager is not recognized as a child of this dynasty."));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
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
                displayName + " was legitimized as " + title + " of "
                        + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + ".");

        player.sendSystemMessage(Component.literal(
                "By royal decree, " + displayName + " is legitimized as " + title
                        + " of " + MCAIntegrationBridge.getVillageName(level, capital.getVillageId()) + "."
        ));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private CapitalRecord resolveCapital(ServerLevel level, UUID targetId) {
        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, targetId);
        if (villageId != null) {
            CapitalRecord byVillage = CapitalManager.getCapitalByVillageId(villageId);
            if (byVillage != null && isEligibleDynasticChild(level, byVillage, targetId)) {
                return byVillage;
            }
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (isEligibleDynasticChild(level, capital, targetId)) {
                return capital;
            }
        }

        return null;
    }

    private boolean isEligibleDynasticChild(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (capital == null || capital.getSovereign() == null || targetId == null) {
            return false;
        }

        if (capital.isRoyalChild(targetId) || capital.isDisinheritedRoyalChild(targetId) || capital.isLegitimizedRoyalChild(targetId)) {
            return true;
        }

        if (MCAIntegrationBridge.isChildOf(level, targetId, capital.getSovereign())) {
            return true;
        }

        UUID consort = capital.getConsort();
        if (consort != null && MCAIntegrationBridge.isChildOf(level, targetId, consort)) {
            return true;
        }

        UUID dowager = capital.getDowager();
        if (dowager != null && MCAIntegrationBridge.isChildOf(level, targetId, dowager)) {
            return true;
        }

        Set<UUID> childrenOfSovereign = MCAIntegrationBridge.getChildren(level, capital.getSovereign());
        if (childrenOfSovereign.contains(targetId)) {
            return true;
        }

        if (consort != null && MCAIntegrationBridge.getChildren(level, consort).contains(targetId)) {
            return true;
        }

        if (dowager != null && MCAIntegrationBridge.getChildren(level, dowager).contains(targetId)) {
            return true;
        }

        return false;
    }

    private String stripKnownTitles(String name) {
        if (name == null || name.isBlank()) {
            return "Unnamed";
        }

        String result = name.trim();
        boolean changed = true;

        while (changed) {
            changed = false;
            for (String title : KNOWN_TITLES) {
                String prefix = title + " ";
                if (result.startsWith(prefix)) {
                    result = result.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }

        return result.isBlank() ? "Unnamed" : result;
    }
}