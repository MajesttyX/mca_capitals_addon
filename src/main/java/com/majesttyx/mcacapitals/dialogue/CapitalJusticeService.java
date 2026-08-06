package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalChronicleService;
import com.majesttyx.mcacapitals.capital.CapitalCrownJusticeService;
import com.majesttyx.mcacapitals.capital.CapitalCrownStandingService;
import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalPlayerNotificationService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalState;
import com.majesttyx.mcacapitals.capital.CrownStanding;
import com.majesttyx.mcacapitals.data.CapitalDataAccess;
import com.majesttyx.mcacapitals.data.CapitalJusticeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalPublicCrownStatus;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.OpenAmbassadorCommunicationPacket;
import com.majesttyx.mcacapitals.util.CapitalJusticeText;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CapitalJusticeService {
    private static final long ACCUSATION_COOLDOWN_DAYS = 2L;

    private CapitalJusticeService() {
    }

    public static boolean openAccusationSelection(ServerPlayer player, Entity masterOfLawsEntity) {
        if (player == null || masterOfLawsEntity == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = resolveCapital(level, masterOfLawsEntity.getUUID());
        if (!isMasterOfLaws(capital, masterOfLawsEntity.getUUID())) {
            player.sendSystemMessage(Component.literal("Only the Master of Laws may receive accusations against the Crown."));
            MCAIntegrationBridge.stopInteracting(masterOfLawsEntity);
            return true;
        }
        long remainingDays = getRemainingCooldownDays(level, capital, player.getUUID());
        if (remainingDays > 0L) {
            player.sendSystemMessage(Component.literal(masterOfLawsEntity.getName().getString() + ": " + CapitalJusticeText.accusationCooldown()));
            MCAIntegrationBridge.stopInteracting(masterOfLawsEntity);
            return true;
        }

        List<OpenAmbassadorCommunicationPacket.Entry> entries = buildCandidates(level, capital).stream()
                .map(candidate -> new OpenAmbassadorCommunicationPacket.Entry(
                        candidate.name(),
                        "Resident of " + capitalName(level, capital),
                        "Bring this accusation before the Master of Laws.",
                        "A false accusation carries a reputation penalty.",
                        "Accuse " + candidate.name(),
                        "/capitaljustice accuse " + capital.getCapitalId() + " " + candidate.id(),
                        true,
                        ""
                ))
                .toList();
        if (entries.isEmpty()) {
            player.sendSystemMessage(Component.literal(masterOfLawsEntity.getName().getString() + ": There is no one suitable to accuse at this time."));
            MCAIntegrationBridge.stopInteracting(masterOfLawsEntity);
            return true;
        }

        player.sendSystemMessage(Component.literal(masterOfLawsEntity.getName().getString() + ": " + CapitalJusticeText.accusationIntro()));
        ModNetwork.sendToPlayer(player, new OpenAmbassadorCommunicationPacket(
                OpenAmbassadorCommunicationPacket.Mode.JUSTICE_CASES,
                "Make an Accusation",
                masterOfLawsEntity.getName().getString(),
                "Choose the resident you accuse of acting against the Crown.",
                "",
                entries,
                List.of()
        ));
        MCAIntegrationBridge.stopInteracting(masterOfLawsEntity);
        return true;
    }

    public static boolean handleAccusation(ServerPlayer player, UUID capitalId, UUID targetId) {
        if (player == null || capitalId == null || targetId == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital == null || capital.getState() != CapitalState.ACTIVE) {
            player.sendSystemMessage(Component.literal("That capital no longer exists."));
            return false;
        }
        if (capital.getMasterOfLaws() == null) {
            player.sendSystemMessage(Component.literal("This capital has no Master of Laws."));
            return false;
        }
        Entity master = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, capital.getMasterOfLaws());
        if (master == null || player.distanceToSqr(master) > 12.0D * 12.0D) {
            player.sendSystemMessage(Component.literal("You must remain near the Master of Laws to make this accusation."));
            return false;
        }
        if (!CapitalPlayerNotificationService.isPlayerWithinCapital(level, capital, player)) {
            player.sendSystemMessage(Component.literal("You must be within the capital to make this accusation."));
            return false;
        }
        if (getRemainingCooldownDays(level, capital, player.getUUID()) > 0L) {
            player.sendSystemMessage(Component.literal(CapitalJusticeText.accusationCooldown()));
            return false;
        }
        if (!isValidAccusationTarget(level, capital, targetId)) {
            player.sendSystemMessage(Component.literal("That person cannot be accused before the Master of Laws."));
            return false;
        }

        CapitalJusticeDataAccess.setLastAccusationDay(level, capitalId, player.getUUID(), currentDay(level));
        CrownStanding standing = CapitalCrownStandingService.getStanding(level, capital, targetId);
        String targetName = CapitalNameService.resolveDisplayName(level, capital, targetId);
        String playerName = player.getName().getString();

        if (standing == CrownStanding.ENEMY_OF_CROWN) {
            rewardCorrectAccusation(player);
            CapitalCrownJusticeService.onCorrectAccusation(level, capital, targetId);
            boolean newWarrant = CapitalJusticeDataAccess.issueArrestWarrant(level, capitalId, targetId);
            String warrantLine = CapitalJusticeText.arrestWarrantIssued(targetName);
            if (newWarrant) {
                CapitalChronicleService.addEntry(level, capital, warrantLine);
                player.sendSystemMessage(Component.literal(CapitalJusticeText.correctAccusation()));
                player.sendSystemMessage(Component.literal(warrantLine));
            } else {
                CapitalChronicleService.addEntry(level, capital, playerName + " renewed the accusation against " + targetName + ", who already stood under Arrest Warrant.");
                player.sendSystemMessage(Component.literal(CapitalJusticeText.correctAccusation()));
                player.sendSystemMessage(Component.literal(targetName + " already stands under Arrest Warrant."));
            }
            CapitalDataAccess.markDirty(level);
            return true;
        }

        applyWrongAccusationPenalties(level, capital, targetId, player);
        CapitalChronicleService.addEntry(level, capital, playerName + " falsely accused " + targetName + " of being an enemy of the Crown.");
        CapitalDataAccess.markDirty(level);
        player.sendSystemMessage(Component.literal(CapitalJusticeText.falseAccusation()));
        return true;
    }

    private static CapitalRecord resolveCapital(ServerLevel level, UUID masterOfLawsId) {
        if (masterOfLawsId == null) {
            return null;
        }
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && masterOfLawsId.equals(capital.getMasterOfLaws())) {
                return capital;
            }
        }
        Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, masterOfLawsId);
        return CapitalManager.getCapitalByVillageId(villageId);
    }

    private static boolean isMasterOfLaws(CapitalRecord capital, UUID villagerId) {
        return capital != null && villagerId != null && villagerId.equals(capital.getMasterOfLaws());
    }

    private static List<Candidate> buildCandidates(ServerLevel level, CapitalRecord capital) {
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
        List<Candidate> candidates = new ArrayList<>();
        for (UUID resident : residents) {
            if (isValidAccusationTarget(level, capital, resident)) {
                candidates.add(new Candidate(resident, CapitalNameService.resolveDisplayName(level, capital, resident)));
            }
        }
        candidates.sort(Comparator.comparing(Candidate::name, String.CASE_INSENSITIVE_ORDER));
        return candidates;
    }

    private static boolean isValidAccusationTarget(ServerLevel level, CapitalRecord capital, UUID targetId) {
        if (level == null || capital == null || targetId == null
                || targetId.equals(capital.getMasterOfLaws())
                || targetId.equals(capital.getSovereign())
                || targetId.equals(capital.getPlayerSovereignId())) {
            return false;
        }
        CapitalPublicCrownStatus publicStatus = CapitalJusticeDataAccess.getPublicStatus(level, capital.getCapitalId(), targetId);
        if (publicStatus == CapitalPublicCrownStatus.DISCOVERED_ENEMY
                || publicStatus == CapitalPublicCrownStatus.RESTORED_TO_PEACE) {
            return false;
        }
        if (!MCAIntegrationBridge.isLoadedAndAlive(level, targetId)
                || !MCAIntegrationBridge.isMCAVillager(level, targetId)) {
            return false;
        }
        String ageState = MCAIntegrationBridge.getAgeState(level, targetId);
        return !"BABY".equalsIgnoreCase(ageState) && !"TODDLER".equalsIgnoreCase(ageState);
    }

    private static void rewardCorrectAccusation(ServerPlayer player) {
        giveOrDrop(player, new ItemStack(Items.EMERALD_BLOCK, 2));
        giveOrDrop(player, new ItemStack(Items.DIAMOND, 3));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static void applyWrongAccusationPenalties(ServerLevel level, CapitalRecord capital, UUID targetId, ServerPlayer player) {
        boolean recognizedFriend = CapitalCrownJusticeService.isRecognizedFriend(level, capital, targetId);
        MCAIntegrationBridge.adjustHearts(level, targetId, player.getUUID(), recognizedFriend ? -75 : -50);
        UUID sovereign = capital.getSovereign();
        if (sovereign != null && !sovereign.equals(targetId) && MCAIntegrationBridge.isMCAVillager(level, sovereign)) {
            MCAIntegrationBridge.adjustHearts(level, sovereign, player.getUUID(), recognizedFriend ? -50 : -30);
        }
    }

    private static long getRemainingCooldownDays(ServerLevel level, CapitalRecord capital, UUID playerId) {
        long lastDay = CapitalJusticeDataAccess.getLastAccusationDay(level, capital.getCapitalId(), playerId);
        if (lastDay == Long.MIN_VALUE) {
            return 0L;
        }
        long elapsed = currentDay(level) - lastDay;
        return elapsed >= ACCUSATION_COOLDOWN_DAYS ? 0L : ACCUSATION_COOLDOWN_DAYS - elapsed;
    }

    private static long currentDay(ServerLevel level) {
        return Math.max(1L, level.getDayTime() / 24000L + 1L);
    }

    private static String capitalName(ServerLevel level, CapitalRecord capital) {
        String name = capital.getVillageId() == null ? "this capital" : MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        return name == null || name.isBlank() ? "this capital" : name;
    }

    private record Candidate(UUID id, String name) {
    }
}
