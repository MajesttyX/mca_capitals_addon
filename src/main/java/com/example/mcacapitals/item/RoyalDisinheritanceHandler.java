package com.example.mcacapitals.item;

import com.example.mcacapitals.capital.CapitalChronicleService;
import com.example.mcacapitals.capital.CapitalCourtWatcher;
import com.example.mcacapitals.capital.CapitalFoundationService;
import com.example.mcacapitals.capital.CapitalManager;
import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class RoyalDisinheritanceHandler {

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
        if (!held.is(ModItems.ROYAL_DISINHERITANCE.get())) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        UUID targetId = livingTarget.getUUID();

        if (!MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            player.sendSystemMessage(Component.literal("Royal disinheritance can only be used on an MCA villager."));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        CapitalRecord capital = resolveCapital(level, targetId);
        if (capital == null) {
            player.sendSystemMessage(Component.literal("That villager is not part of a capital."));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (!capital.isRoyalChild(targetId) && !targetId.equals(capital.getHeir())) {
            player.sendSystemMessage(Component.literal("That villager has no royal claim to strip."));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
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

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private CapitalRecord resolveCapital(ServerLevel level, UUID targetId) {
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

    private String stripKnownTitles(String name) {
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