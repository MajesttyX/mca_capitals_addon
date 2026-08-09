package com.majesttyx.mcacapitals.util;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

public final class CapitalJusticeText {

    private CapitalJusticeText() {
    }

    public static String accusationIntro() {
        return "Speak carefully. An accusation before the law is no small thing.";
    }

    public static String accusationCooldown() {
        return "One charge has already been heard. Wait before bringing another.";
    }

    public static String correctAccusation() {
        return "You spoke true. The accused will answer before the law.";
    }

    public static String falseAccusation() {
        return "The Crown does not reward careless accusations.";
    }

    public static String arrestWarrantIssued(String targetName) {
        return "An Arrest Warrant has been issued for " + targetName + ". See them to the cells quickly before the matter darkens. Do not let " + targetName + " escape!";
    }

    public static String missedWarrant(ServerLevel level, UUID targetId, String targetName) {
        return pick(level, targetId, List.of(
                "The warrant went unanswered. The court has chosen blood.",
                targetName + " did not come before the law. Execution has been ordered.",
                "The Crown’s patience ended. " + targetName + " is now condemned."
        ));
    }

    public static String deliveredToPrison(ServerLevel level, UUID targetId, String targetName) {
        return pick(level, targetId, List.of(
                targetName + " was delivered to the prison under Arrest Warrant.",
                targetName + " is now held in the Crown’s custody.",
                "The accused has been brought within the prison walls."
        ));
    }

    public static String releasedFromPrison(ServerLevel level, UUID targetId, String targetName) {
        return pick(level, targetId, List.of(
                "The warrant against " + targetName + " has been lifted.",
                "The law found no further cause to hold " + targetName + "."
        ));
    }

    public static String executionAfterPrison(String targetName) {
        return "Sufficient evidence was found against " + targetName + ", it is now the Crown’s pleasure to seek their execution.";
    }

    public static String exileDiscovery(ServerLevel level, UUID targetId, String targetName, String capitalName) {
        return pick(level, targetId, List.of(
                "Reports say " + targetName + " has been found beyond the capital’s bounds.",
                "The Master of Laws has received word of " + targetName + " in exile.",
                targetName + " has been seen outside " + capitalName + ", far from lawful answer.",
                "The court names " + targetName + " absent from the Crown’s reach.",
                "A report of exile has entered the records: " + targetName + " is gone."
        ));
    }

    public static String royalPardonGrantedBySovereign(ServerLevel level, UUID speakerId) {
        return pick(level, speakerId, List.of(
                "Mercy is mine to give. Take this Royal Pardon.",
                "The Crown may punish, but it may also forgive.",
                "Let this pardon carry my authority.",
                "Use this carefully. Mercy loses weight when spent foolishly.",
                "I grant it. Let the record know the Crown stayed its hand."
        ));
    }

    public static String royalPardonGrantedByHand(ServerLevel level, UUID speakerId) {
        return pick(level, speakerId, List.of(
                "I speak with the Crown’s trust. Take this Royal Pardon.",
                "The realm survives because law and mercy both have their hour.",
                "I will grant this, but do not make me regret it.",
                "The Crown’s mercy may pass through my hand.",
                "Take it. Some matters are better ended before they bleed."
        ));
    }

    public static String royalPardonGrantedByMasterOfLaws(ServerLevel level, UUID speakerId) {
        return pick(level, speakerId, List.of(
                "The law allows mercy when authority permits it. Take this.",
                "A pardon is not innocence. Remember that.",
                "I can grant this, but the record will remember.",
                "Very well. The law will make room for mercy.",
                "Take the pardon. Use it before the court hardens."
        ));
    }

    public static String royalPardonRefusedTrust(ServerLevel level, UUID speakerId) {
        return pick(level, speakerId, List.of(
                "A Royal Pardon requires trust. You have not earned mine.",
                "Mercy is too costly to hand to uncertain friends.",
                "I do not know you well enough to place mercy in your hands.",
                "Return when your loyalty is more than a hope.",
                "The Crown’s mercy is not given to strangers."
        ));
    }

    public static String royalPardonNoAuthority(ServerLevel level, UUID speakerId) {
        return pick(level, speakerId, List.of(
                "I have no authority to grant such mercy.",
                "That is not mine to give.",
                "You ask the wrong person for a pardon.",
                "Only lawful authority may stay the court’s hand.",
                "I cannot grant what the Crown has not placed in my keeping."
        ));
    }

    public static String royalPardonUsed(ServerLevel level, UUID targetId, String targetName) {
        return pick(level, targetId, List.of(
                targetName + " has been granted a Royal Pardon.",
                "The Crown’s mercy has reached " + targetName + ".",
                "The warrant against " + targetName + " is ended by pardon.",
                "The law steps back from " + targetName + ".",
                "Mercy has been entered into the record."
        ));
    }

    public static String sealedPurseNoCases(ServerLevel level, UUID speakerId) {
        return pick(level, speakerId, List.of(
                "There is no matter for a purse to settle.",
                "The records are quiet. Keep your gift.",
                "No case before me can be made to disappear.",
                "You bring silver to an empty desk.",
                "There is nothing here to bury."
        ));
    }

    public static String sealedPurseSuccess(ServerLevel level, UUID targetId, String targetName) {
        return pick(level, targetId, List.of(
                "The Master of Laws accepts the Sealed Purse. The matter disappears from the records.",
                "The purse changes hands. The record grows strangely quiet.",
                "The Master of Laws says nothing, but the case is gone.",
                "A seal is broken. A name is removed.",
                "The court forgets what it was ready to remember."
        ));
    }

    public static String sealedPurseFailure(ServerLevel level, UUID targetId) {
        return pick(level, targetId, List.of(
                "The Master of Laws accepts the purse, but refuses to alter the records.",
                "The gift is taken. The law remains unmoved.",
                "The purse vanishes into a drawer. The case does not.",
                "The Master of Laws gives you a cold look. The record stands.",
                "Gold can open doors, but not this one."
        ));
    }

    public static String sealedPurseFormalExecution(ServerLevel level, UUID targetId) {
        return pick(level, targetId, List.of(
                "That matter has gone beyond quiet influence.",
                "Execution business cannot be buried with a purse.",
                "Only a Royal Pardon can answer this now.",
                "The record is sealed in blood. A purse will not move it.",
                "Too late. This is no longer a matter for discretion."
        ));
    }

    public static String masterOfLawsAppointed(ServerLevel level, UUID targetId, String targetName) {
        return pick(level, targetId, List.of(
                targetName + " was appointed Master of Laws.",
                targetName + " now holds the law in the Crown’s name.",
                "The Crown’s justice now passes through " + targetName + "."
        ));
    }

    public static String masterOfLawsRemoved(ServerLevel level, UUID capitalId) {
        return pick(level, capitalId, List.of(
                "The prison is gone. The Master of Laws no longer holds office.",
                "The Master of Laws has been relieved, for the prison no longer stands.",
                "No prison, no warrants, no keeper of cells."
        ));
    }

    public static String naturalDukedom(ServerLevel level, UUID targetId, String targetName) {
        return pick(level, targetId, List.of(
                targetName + " was raised by the strength of the great houses.",
                "The great houses have lifted " + targetName + " into higher nobility.",
                "By wealth, blood, and influence, " + targetName + " now stands among the dukes.",
                "The court recognizes " + targetName + " as a power of the realm.",
                "A new high noble has risen from the great houses."
        ));
    }

    public static String royalPardonGrantLine(ServerLevel level, CapitalRecord capital, UUID speakerId) {
        if (capital != null && speakerId != null) {
            if (speakerId.equals(capital.getSovereign())) {
                return royalPardonGrantedBySovereign(level, speakerId);
            }
            if (speakerId.equals(capital.getHand())) {
                return royalPardonGrantedByHand(level, speakerId);
            }
            if (speakerId.equals(capital.getMasterOfLaws())) {
                return royalPardonGrantedByMasterOfLaws(level, speakerId);
            }
        }
        return royalPardonNoAuthority(level, speakerId);
    }

    private static String pick(ServerLevel level, UUID salt, List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        if (values.size() == 1 || level == null) {
            return values.get(0);
        }
        return values.get(level.random.nextInt(values.size()));
    }
}
