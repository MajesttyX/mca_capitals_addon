package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.network.chat.Component;
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
                    Component.translatable("mcacapitals.war.validation.campaign_capitals_unavailable")
            );
        }

        if (!CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        attackingCapital,
                        initiatingPlayerId
                )) {
            return Validation.failure(
                    Component.translatable("mcacapitals.war.validation.attack_authority")
            );
        }

        if (attackingCapital.getCapitalId().equals(
                defendingCapital.getCapitalId()
        )) {
            return Validation.failure(
                    Component.translatable("mcacapitals.war.validation.self_attack")
            );
        }

        if (attackingCapital.getState()
                != CapitalState.ACTIVE
                || defendingCapital.getState()
                != CapitalState.ACTIVE) {
            return Validation.failure(
                    Component.translatable("mcacapitals.war.validation.both_active")
            );
        }

        if (attackingCapital.getVillageId() == null
                || defendingCapital.getVillageId() == null
                || !CapitalManager.isCapitalInLevel(attackingCapital, level)
                || !CapitalManager.isCapitalInLevel(defendingCapital, level)
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
                    Component.translatable("mcacapitals.war.validation.same_loaded_dimension")
            );
        }

        if (CapitalCampaignDataAccess
                .getCampaignForCapital(
                        level,
                        attackingCapital.getCapitalId()
                ) != null) {
            return Validation.failure(
                    Component.translatable("mcacapitals.war.validation.attacker_campaign_active")
            );
        }

        if (CapitalCampaignDataAccess
                .getCampaignForCapital(
                        level,
                        defendingCapital.getCapitalId()
                ) != null) {
            return Validation.failure(
                    Component.translatable("mcacapitals.war.validation.defender_campaign_active")
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
                    Component.translatable("mcacapitals.war.validation.no_eligible_attackers")
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

        if (!CapitalManager.isCapitalInLevel(capital, level)) {
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
            Component failureMessage
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
                Component message
        ) {
            return new Validation(
                    false,
                    List.of(),
                    message
            );
        }
    }
}