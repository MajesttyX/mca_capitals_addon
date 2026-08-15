package com.majesttyx.mcacapitals.dialogue;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalNameService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalRoyalGuardService;
import com.majesttyx.mcacapitals.capital.CapitalResidentScanner;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.network.OpenBetrothalSelectionPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CapitalPetitionRequirements {

    private CapitalPetitionRequirements() {
    }

    static boolean isAudienceValid(ServerPlayer player, Entity villagerEntity, double maxDistanceSqr) {
        return player != null
                && villagerEntity != null
                && player.distanceToSqr(villagerEntity) <= maxDistanceSqr;
    }

    static int countMasterProfessionVillagers(ServerLevel level, Set<UUID> residents) {
        int count = 0;
        for (UUID residentId : residents) {
            if (MCAIntegrationBridge.isMasterProfessionVillager(level, residentId)) {
                count++;
            }
        }
        return count;
    }

    static CapitalRecord resolveSovereignCapital(ServerLevel level, Entity villagerEntity) {
        if (level == null || villagerEntity == null) {
            return null;
        }

        CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(level, villagerEntity.getUUID());
        if (capital == null) {
            Integer villageId = MCAIntegrationBridge.getVillageIdForResident(level, villagerEntity.getUUID());
            capital = CapitalManager.getCapitalByVillageId(villageId);
        }

        if (capital == null) {
            return null;
        }

        return capital;
    }

    static boolean hasCommanderAllegiance(ServerLevel level, CapitalRecord capital, UUID playerId, int requiredHearts) {
        if (level == null || capital == null || playerId == null) {
            return false;
        }

        UUID commanderId = capital.getCommander();
        if (commanderId == null) {
            return false;
        }

        int hearts = MCAIntegrationBridge.getHeartsWithPlayer(level, commanderId, playerId);
        return hearts >= requiredHearts;
    }

    static boolean hasAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return false;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        return progress.isDone();
    }

    static List<OpenBetrothalSelectionPacket.Candidate> collectPlayerBetrothalCandidates(ServerLevel level, CapitalRecord capital) {
        List<OpenBetrothalSelectionPacket.Candidate> result = new ArrayList<>();
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        for (UUID residentId : residents) {
            if (!isPlayerBetrothalCandidate(level, capital, residentId)) {
                continue;
            }
            result.add(new OpenBetrothalSelectionPacket.Candidate(
                    residentId,
                    buildBetrothalCandidateNameComponent(level, capital, residentId)
            ));
        }

        result.sort(Comparator.comparing(
                candidate -> candidate.name().getString(),
                String.CASE_INSENSITIVE_ORDER
        ));
        return result;
    }

    static List<OpenBetrothalSelectionPacket.Candidate> collectRecommendedBetrothalCandidates(ServerLevel level, CapitalRecord capital) {
        List<OpenBetrothalSelectionPacket.Candidate> result = new ArrayList<>();
        Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());

        for (UUID residentId : residents) {
            if (!isRecommendedBetrothalCandidate(level, capital, residentId)) {
                continue;
            }
            result.add(new OpenBetrothalSelectionPacket.Candidate(
                    residentId,
                    buildBetrothalCandidateNameComponent(level, capital, residentId)
            ));
        }

        result.sort(Comparator.comparing(
                candidate -> candidate.name().getString(),
                String.CASE_INSENSITIVE_ORDER
        ));
        return result;
    }

    static boolean isPlayerBetrothalCandidate(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || entityId == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isAliveMCAVillager(level, entityId)) {
            return false;
        }

        if (!MCAIntegrationBridge.isTeenOrAdultVillager(level, entityId)) {
            return false;
        }

        if (entityId.equals(capital.getSovereign())
                || entityId.equals(capital.getConsort())
                || entityId.equals(capital.getDowager())
                || entityId.equals(capital.getCommander())) {
            return false;
        }

        CapitalTitleResolver.ResolvedTitleId titleId = CapitalTitleResolver.getResolvedTitleId(level, capital, entityId);
        return titleId == CapitalTitleResolver.ResolvedTitleId.LORD
                || titleId == CapitalTitleResolver.ResolvedTitleId.DUKE
                || titleId == CapitalTitleResolver.ResolvedTitleId.ROYAL_CHILD
                || titleId == CapitalTitleResolver.ResolvedTitleId.HEIR_APPARENT;
    }

    static boolean isRecommendedBetrothalCandidate(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || entityId == null) {
            return false;
        }

        if (!MCAIntegrationBridge.isAliveMCAVillager(level, entityId)) {
            return false;
        }

        return !entityId.equals(capital.getSovereign());
    }

    static Component buildBetrothalCandidateNameComponent(ServerLevel level, CapitalRecord capital, UUID entityId) {
        Component baseName = CapitalNameService.resolveDisplayNameComponent(level, capital, entityId);
        CapitalTitleResolver.ResolvedTitleId titleId = CapitalTitleResolver.getResolvedTitleId(level, capital, entityId);

        if (titleId == CapitalTitleResolver.ResolvedTitleId.COMMONER
                || titleId == CapitalTitleResolver.ResolvedTitleId.NONE) {
            return baseName;
        }

        if (titleId == CapitalTitleResolver.ResolvedTitleId.ROYAL_GUARD) {
            return CapitalRoyalGuardService.buildRoyalGuardDisplayNameComponent(
                    level,
                    capital,
                    entityId
            );
        }

        return Component.translatable(
                "mcacapitals.dynamic.name.titled",
                CapitalTitleResolver.getDisplayTitleComponent(level, capital, entityId),
                baseName
        );
    }
}