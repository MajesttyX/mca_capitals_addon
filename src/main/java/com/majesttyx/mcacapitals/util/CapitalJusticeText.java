package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

public final class CapitalJusticeText {

    private CapitalJusticeText() {
    }

    public static Component accusationIntro() {
        return Component.translatable("mcacapitals.justice.accusation.intro");
    }

    public static Component accusationCooldown() {
        return Component.translatable("mcacapitals.justice.accusation.cooldown");
    }

    public static Component correctAccusation() {
        return Component.translatable("mcacapitals.justice.accusation.correct");
    }

    public static Component falseAccusation() {
        return Component.translatable("mcacapitals.justice.accusation.false");
    }

    public static Component arrestWarrantIssued(String targetName) {
        return Component.translatable("mcacapitals.justice.arrest_warrant.issued", targetName);
    }

    public static Component missedWarrant(ServerLevel level, UUID targetId, String targetName) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.arrest_warrant.missed.1",
                        "mcacapitals.justice.arrest_warrant.missed.2",
                        "mcacapitals.justice.arrest_warrant.missed.3"
                ),
                targetName
        );
    }

    public static Component deliveredToPrison(ServerLevel level, UUID targetId, String targetName) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.prison.delivered.1",
                        "mcacapitals.justice.prison.delivered.2",
                        "mcacapitals.justice.prison.delivered.3"
                ),
                targetName
        );
    }

    public static Component releasedFromPrison(ServerLevel level, UUID targetId, String targetName) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.prison.released.1",
                        "mcacapitals.justice.prison.released.2"
                ),
                targetName
        );
    }

    public static Component executionAfterPrison(String targetName) {
        return Component.translatable("mcacapitals.justice.prison.execution_after", targetName);
    }

    public static Component exileDiscovery(ServerLevel level, UUID targetId, String targetName, String capitalName) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.exile.discovery.1",
                        "mcacapitals.justice.exile.discovery.2",
                        "mcacapitals.justice.exile.discovery.3",
                        "mcacapitals.justice.exile.discovery.4",
                        "mcacapitals.justice.exile.discovery.5"
                ),
                targetName,
                capitalName
        );
    }

    public static Component royalPardonGrantedBySovereign(ServerLevel level, UUID speakerId) {
        return pick(
                level,
                speakerId,
                List.of(
                        "mcacapitals.justice.royal_pardon.granted.sovereign.1",
                        "mcacapitals.justice.royal_pardon.granted.sovereign.2",
                        "mcacapitals.justice.royal_pardon.granted.sovereign.3",
                        "mcacapitals.justice.royal_pardon.granted.sovereign.4",
                        "mcacapitals.justice.royal_pardon.granted.sovereign.5"
                )
        );
    }

    public static Component royalPardonGrantedByHand(ServerLevel level, UUID speakerId) {
        return pick(
                level,
                speakerId,
                List.of(
                        "mcacapitals.justice.royal_pardon.granted.hand.1",
                        "mcacapitals.justice.royal_pardon.granted.hand.2",
                        "mcacapitals.justice.royal_pardon.granted.hand.3",
                        "mcacapitals.justice.royal_pardon.granted.hand.4",
                        "mcacapitals.justice.royal_pardon.granted.hand.5"
                )
        );
    }

    public static Component royalPardonGrantedByMasterOfLaws(ServerLevel level, UUID speakerId) {
        return pick(
                level,
                speakerId,
                List.of(
                        "mcacapitals.justice.royal_pardon.granted.master_of_laws.1",
                        "mcacapitals.justice.royal_pardon.granted.master_of_laws.2",
                        "mcacapitals.justice.royal_pardon.granted.master_of_laws.3",
                        "mcacapitals.justice.royal_pardon.granted.master_of_laws.4",
                        "mcacapitals.justice.royal_pardon.granted.master_of_laws.5"
                )
        );
    }

    public static Component royalPardonRefusedTrust(ServerLevel level, UUID speakerId) {
        return pick(
                level,
                speakerId,
                List.of(
                        "mcacapitals.justice.royal_pardon.refused_trust.1",
                        "mcacapitals.justice.royal_pardon.refused_trust.2",
                        "mcacapitals.justice.royal_pardon.refused_trust.3",
                        "mcacapitals.justice.royal_pardon.refused_trust.4",
                        "mcacapitals.justice.royal_pardon.refused_trust.5"
                )
        );
    }

    public static Component royalPardonNoAuthority(ServerLevel level, UUID speakerId) {
        return pick(
                level,
                speakerId,
                List.of(
                        "mcacapitals.justice.royal_pardon.no_authority.1",
                        "mcacapitals.justice.royal_pardon.no_authority.2",
                        "mcacapitals.justice.royal_pardon.no_authority.3",
                        "mcacapitals.justice.royal_pardon.no_authority.4",
                        "mcacapitals.justice.royal_pardon.no_authority.5"
                )
        );
    }

    public static Component royalPardonUsed(ServerLevel level, UUID targetId, String targetName) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.royal_pardon.used.1",
                        "mcacapitals.justice.royal_pardon.used.2",
                        "mcacapitals.justice.royal_pardon.used.3",
                        "mcacapitals.justice.royal_pardon.used.4",
                        "mcacapitals.justice.royal_pardon.used.5"
                ),
                targetName
        );
    }

    public static Component sealedPurseNoCases(ServerLevel level, UUID speakerId) {
        return pick(
                level,
                speakerId,
                List.of(
                        "mcacapitals.justice.sealed_purse.no_cases.1",
                        "mcacapitals.justice.sealed_purse.no_cases.2",
                        "mcacapitals.justice.sealed_purse.no_cases.3",
                        "mcacapitals.justice.sealed_purse.no_cases.4",
                        "mcacapitals.justice.sealed_purse.no_cases.5"
                )
        );
    }

    public static Component sealedPurseSuccess(ServerLevel level, UUID targetId, String targetName) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.sealed_purse.success.1",
                        "mcacapitals.justice.sealed_purse.success.2",
                        "mcacapitals.justice.sealed_purse.success.3",
                        "mcacapitals.justice.sealed_purse.success.4",
                        "mcacapitals.justice.sealed_purse.success.5"
                ),
                targetName
        );
    }

    public static Component sealedPurseFailure(ServerLevel level, UUID targetId) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.sealed_purse.failure.1",
                        "mcacapitals.justice.sealed_purse.failure.2",
                        "mcacapitals.justice.sealed_purse.failure.3",
                        "mcacapitals.justice.sealed_purse.failure.4",
                        "mcacapitals.justice.sealed_purse.failure.5"
                )
        );
    }

    public static Component sealedPurseFormalExecution(ServerLevel level, UUID targetId) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.sealed_purse.formal_execution.1",
                        "mcacapitals.justice.sealed_purse.formal_execution.2",
                        "mcacapitals.justice.sealed_purse.formal_execution.3",
                        "mcacapitals.justice.sealed_purse.formal_execution.4",
                        "mcacapitals.justice.sealed_purse.formal_execution.5"
                )
        );
    }

    public static Component masterOfLawsAppointed(ServerLevel level, UUID targetId, String targetName) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.master_of_laws.appointed.1",
                        "mcacapitals.justice.master_of_laws.appointed.2",
                        "mcacapitals.justice.master_of_laws.appointed.3"
                ),
                targetName
        );
    }

    public static Component masterOfLawsRemoved(ServerLevel level, UUID capitalId) {
        return pick(
                level,
                capitalId,
                List.of(
                        "mcacapitals.justice.master_of_laws.removed.1",
                        "mcacapitals.justice.master_of_laws.removed.2",
                        "mcacapitals.justice.master_of_laws.removed.3"
                )
        );
    }

    public static Component naturalDukedom(ServerLevel level, UUID targetId, String targetName) {
        return pick(
                level,
                targetId,
                List.of(
                        "mcacapitals.justice.natural_dukedom.1",
                        "mcacapitals.justice.natural_dukedom.2",
                        "mcacapitals.justice.natural_dukedom.3",
                        "mcacapitals.justice.natural_dukedom.4",
                        "mcacapitals.justice.natural_dukedom.5"
                ),
                targetName
        );
    }

    public static Component royalPardonGrantLine(ServerLevel level, CapitalRecord capital, UUID speakerId) {
        if (capital != null && speakerId != null) {
            if (speakerId.equals(capital.getSovereign())) {
                return royalPardonGrantedBySovereign(
                        level,
                        speakerId
                );
            }

            if (speakerId.equals(capital.getHand())) {
                return royalPardonGrantedByHand(
                        level,
                        speakerId
                );
            }

            if (speakerId.equals(capital.getMasterOfLaws())) {
                return royalPardonGrantedByMasterOfLaws(
                        level,
                        speakerId
                );
            }
        }

        return royalPardonNoAuthority(
                level,
                speakerId
        );
    }

    private static Component pick(ServerLevel level, UUID salt, List<String> translationKeys, Object... args) {
        if (translationKeys == null || translationKeys.isEmpty()) {
            return Component.empty();
        }
        String key;
        if (translationKeys.size() == 1 || level == null) {
            key = translationKeys.get(0);
        } else {
            key = translationKeys.get(level.random.nextInt(translationKeys.size()));
        }
        return Component.translatable(key, args);
    }
}