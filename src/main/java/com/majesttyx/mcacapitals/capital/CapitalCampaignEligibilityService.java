package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import fabric.net.mca.server.world.data.Village;
import fabric.net.mca.server.world.data.VillageManager;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CapitalCampaignEligibilityService {

    private CapitalCampaignEligibilityService() {
    }

    static Validation validateCampaign(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            UUID initiatingPlayerId
    ) {
        if (level == null
                || attackingCapital == null
                || defendingCapital == null
                || attackingCapital.getCapitalId() == null
                || defendingCapital.getCapitalId() == null) {
            return Validation.failure(
                    "The campaign capitals are unavailable."
            );
        }

        if (!CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        attackingCapital,
                        initiatingPlayerId
                )) {
            return Validation.failure(
                    "Only the player sovereign, or the player Hand serving a villager sovereign, may plan an attack."
            );
        }

        if (attackingCapital.getCapitalId().equals(
                defendingCapital.getCapitalId()
        )) {
            return Validation.failure(
                    "A capital cannot attack itself."
            );
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(
                level,
                attackingCapital,
                defendingCapital
        );
        if (com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess
                .getDiplomaticState(
                        level,
                        attackingCapital.getCapitalId(),
                        defendingCapital.getCapitalId()
                ) == CapitalDiplomaticState.TRUCE) {
            return Validation.failure(
                    "An active Truce forbids either capital from planning an attack."
            );
        }

        if (attackingCapital.getState()
                != CapitalState.ACTIVE
                || defendingCapital.getState()
                != CapitalState.ACTIVE) {
            return Validation.failure(
                    "Both capitals must be active before an attack can be planned."
            );
        }

        if (attackingCapital.getVillageId() == null
                || defendingCapital.getVillageId() == null
                || VillageManager.get(level)
                .getOrEmpty(
                        attackingCapital.getVillageId()
                )
                .isEmpty()
                || VillageManager.get(level)
                .getOrEmpty(
                        defendingCapital.getVillageId()
                )
                .isEmpty()) {
            return Validation.failure(
                    "Attacks can begin only when both capitals are in the same loaded dimension."
            );
        }

        if (CapitalCampaignDataAccess
                .getCampaignForCapital(
                        level,
                        attackingCapital.getCapitalId()
                ) != null) {
            return Validation.failure(
                    "The attacking capital is already involved in an active campaign."
            );
        }

        if (CapitalCampaignDataAccess
                .getCampaignForCapital(
                        level,
                        defendingCapital.getCapitalId()
                ) != null) {
            return Validation.failure(
                    "The defending capital is already involved in an active campaign."
            );
        }

        List<UUID> attackers =
                findEligibleAttackers(
                        level,
                        attackingCapital,
                        null
                );

        if (attackers.isEmpty()) {
            return Validation.failure(
                    "The attacking capital has no eligible ordinary Guards or Archers currently available to pledge to a campaign."
            );
        }

        return Validation.success(attackers);
    }

    static List<UUID> findEligibleAttackers(
            ServerLevel level,
            CapitalRecord capital
    ) {
        return findEligibleAttackers(
                level,
                capital,
                null
        );
    }

    static List<UUID> findEligibleAttackers(
            ServerLevel level,
            CapitalRecord capital,
            UUID allowedCampaignId
    ) {
        if (level == null
                || capital == null
                || capital.getCapitalId() == null) {
            return List.of();
        }

        Set<UUID> residents =
                CapitalResidentScanner.scanResidents(
                        level,
                        capital.getCapitalId()
                );

        return residents.stream()
                .filter(id -> id != null)
                .filter(id ->
                        !id.equals(
                                capital.getSovereign()
                        )
                )
                .filter(id ->
                        !capital.isRoyalGuard(id)
                )
                .filter(id ->
                        MCAIntegrationBridge.isMCAGuard(
                                level,
                                id
                        )
                )
                .filter(id ->
                        MCAIntegrationBridge
                                .isTeenOrAdultVillager(
                                        level,
                                        id
                                )
                )
                .filter(id ->
                        MCAIntegrationBridge
                                .isLoadedAndAlive(
                                        level,
                                        id
                                )
                )
                .filter(id -> {
                    CapitalCampaignRecord existing =
                            CapitalCampaignDataAccess
                                    .getCampaignForAttacker(
                                            level,
                                            id
                                    );

                    return existing == null
                            || allowedCampaignId != null
                            && allowedCampaignId.equals(
                            existing.getCampaignId()
                    );
                })
                .sorted(
                        Comparator.comparing(
                                UUID::toString
                        )
                )
                .limit(
                        CapitalCampaignRecord
                                .MAX_ATTACKERS
                )
                .toList();
    }

    static Village getVillage(
            ServerLevel level,
            CapitalRecord capital
    ) {
        if (level == null
                || capital == null
                || capital.getVillageId() == null) {
            return null;
        }

        return VillageManager.get(level)
                .getOrEmpty(
                        capital.getVillageId()
                )
                .orElse(null);
    }

    record Validation(
            boolean valid,
            List<UUID> attackers,
            String failureMessage
    ) {

        static Validation success(
                List<UUID> attackers
        ) {
            return new Validation(
                    true,
                    List.copyOf(attackers),
                    null
            );
        }

        static Validation failure(
                String message
        ) {
            return new Validation(
                    false,
                    List.of(),
                    message
            );
        }
    }
}
