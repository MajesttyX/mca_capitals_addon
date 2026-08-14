package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.UUID;

public final class CapitalTitleOfficeIdentityResolver {

    public enum TitleIdentity {
        HIGH_SOVEREIGN,
        SOVEREIGN,
        SOVEREIGN_CONSORT,
        SOVEREIGN_DOWAGER,
        HEIR_APPARENT,
        CROWN_HEIR,
        ROYAL_CHILD,
        PRINCE_CONSORT,
        DOWAGER_PRINCE,
        DUKE,
        DOWAGER_DUKE,
        LORD,
        KNIGHT,
        COMMONER
    }

    public enum OfficeIdentity {
        NONE,
        HAND,
        GRAND_MAESTER,
        COURT_HERALD,
        LORD_COMMANDER,
        MAESTER,
        MASTER_OF_LAWS,
        ROYAL_GUARD,
        AMBASSADOR
    }

    public enum DialogueRankIdentity {
        SOVEREIGN,
        HEIR,
        HAND,
        GRAND_MAESTER,
        LORD_COMMANDER,
        DUKE_OR_DUCHESS,
        LORD_OR_LADY,
        ROYAL_CONSORT,
        ROYAL_DOWAGER,
        ROYAL_CHILD,
        KNIGHT,
        COMMONER,
        OTHER
    }

    private CapitalTitleOfficeIdentityResolver() {
    }

    public static TitleIdentity resolveTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || entityId == null) {
            return TitleIdentity.COMMONER;
        }

        if (isHighSovereign(capital, entityId)) {
            return TitleIdentity.HIGH_SOVEREIGN;
        }

        if (isLocalSovereign(capital, entityId)) {
            return TitleIdentity.SOVEREIGN;
        }

        if (entityId.equals(capital.getConsort()) || entityId.equals(capital.getPlayerConsortId())) {
            return TitleIdentity.SOVEREIGN_CONSORT;
        }

        if (entityId.equals(capital.getDowager())) {
            return TitleIdentity.SOVEREIGN_DOWAGER;
        }

        if (entityId.equals(capital.getHeir())) {
            if (capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL
                    && !isRoyalBloodChildOfCurrentLine(level, capital, entityId)) {
                return TitleIdentity.HEIR_APPARENT;
            }
            return TitleIdentity.CROWN_HEIR;
        }

        if (isDynasticPrinceOrPrincess(level, capital, entityId)) {
            return TitleIdentity.ROYAL_CHILD;
        }

        NobleTitle marriageTitle = PlayerCapitalTitleService.getMarriageTitle(level, capital, entityId);
        if (capital.isPrinceConsort(entityId)
                || marriageTitle == NobleTitle.PRINCE
                || marriageTitle == NobleTitle.PRINCESS) {
            return TitleIdentity.PRINCE_CONSORT;
        }

        NobleTitle dowagerBaseTitle = PlayerCapitalTitleService.getDowagerBaseTitle(level, capital, entityId);
        if (capital.isDowagerPrince(entityId)
                || dowagerBaseTitle == NobleTitle.PRINCE
                || dowagerBaseTitle == NobleTitle.PRINCESS) {
            return TitleIdentity.DOWAGER_PRINCE;
        }

        if (capital.isDuke(entityId)
                || capital.isMarriageDuke(entityId)
                || marriageTitle == NobleTitle.DUKE
                || marriageTitle == NobleTitle.DUCHESS) {
            return TitleIdentity.DUKE;
        }

        NobleTitle grantedTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, entityId);
        if (grantedTitle == NobleTitle.DUKE || grantedTitle == NobleTitle.DUCHESS) {
            return TitleIdentity.DUKE;
        }

        if (capital.isDowagerDuke(entityId)
                || dowagerBaseTitle == NobleTitle.DUKE
                || dowagerBaseTitle == NobleTitle.DUCHESS) {
            return TitleIdentity.DOWAGER_DUKE;
        }

        if (capital.isLord(entityId)
                || marriageTitle == NobleTitle.LORD
                || marriageTitle == NobleTitle.LADY
                || isMarriageLord(level, capital, entityId)
                || grantedTitle == NobleTitle.LORD
                || grantedTitle == NobleTitle.LADY) {
            return TitleIdentity.LORD;
        }

        if (capital.isRoyalGuard(entityId) || capital.isKnight(entityId)) {
            return TitleIdentity.KNIGHT;
        }

        return TitleIdentity.COMMONER;
    }

    public static OfficeIdentity resolveOffice(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || entityId == null) {
            return OfficeIdentity.NONE;
        }

        if (entityId.equals(capital.getHand()) || PlayerCapitalTitleService.isHand(level, capital, entityId)) {
            return OfficeIdentity.HAND;
        }

        if (entityId.equals(capital.getGrandMaester())) {
            return OfficeIdentity.GRAND_MAESTER;
        }

        if (entityId.equals(capital.getHerald())) {
            return OfficeIdentity.COURT_HERALD;
        }

        if (entityId.equals(capital.getCommander()) || PlayerCapitalTitleService.isCommander(level, capital, entityId)) {
            return OfficeIdentity.LORD_COMMANDER;
        }

        if (entityId.equals(capital.getMasterOfLaws())) {
            return OfficeIdentity.MASTER_OF_LAWS;
        }

        if (capital.isRoyalGuard(entityId)) {
            return OfficeIdentity.ROYAL_GUARD;
        }

        if (CapitalAmbassadorService.isAmbassador(level, capital, entityId)) {
            return OfficeIdentity.AMBASSADOR;
        }

        if (MCAIntegrationBridge.isMCAVillager(level, entityId)) {
            Set<UUID> residents = CapitalResidentScanner.scanResidents(level, capital.getCapitalId());
            if (CapitalMaesterSelection.isMaester(level, capital, entityId, residents)) {
                return OfficeIdentity.MAESTER;
            }
        }

        return OfficeIdentity.NONE;
    }

    public static DialogueRankIdentity resolveDialogueRank(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || entityId == null) {
            return DialogueRankIdentity.OTHER;
        }

        OfficeIdentity office = resolveOffice(level, capital, entityId);
        if (office == OfficeIdentity.HAND) {
            return DialogueRankIdentity.HAND;
        }
        if (office == OfficeIdentity.GRAND_MAESTER) {
            return DialogueRankIdentity.GRAND_MAESTER;
        }
        if (office == OfficeIdentity.LORD_COMMANDER) {
            return DialogueRankIdentity.LORD_COMMANDER;
        }

        TitleIdentity title = resolveTitle(level, capital, entityId);
        return switch (title) {
            case HIGH_SOVEREIGN, SOVEREIGN -> DialogueRankIdentity.SOVEREIGN;
            case HEIR_APPARENT, CROWN_HEIR -> DialogueRankIdentity.HEIR;
            case DUKE -> DialogueRankIdentity.DUKE_OR_DUCHESS;
            case LORD -> DialogueRankIdentity.LORD_OR_LADY;
            case SOVEREIGN_CONSORT, PRINCE_CONSORT -> DialogueRankIdentity.ROYAL_CONSORT;
            case SOVEREIGN_DOWAGER, DOWAGER_PRINCE -> DialogueRankIdentity.ROYAL_DOWAGER;
            case ROYAL_CHILD -> DialogueRankIdentity.ROYAL_CHILD;
            case KNIGHT -> DialogueRankIdentity.KNIGHT;
            case COMMONER -> office == OfficeIdentity.NONE || office == OfficeIdentity.MASTER_OF_LAWS
                    ? DialogueRankIdentity.COMMONER
                    : DialogueRankIdentity.OTHER;
            case DOWAGER_DUKE -> DialogueRankIdentity.DUKE_OR_DUCHESS;
        };
    }

    public static boolean isUntitledCommoner(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return true;
        }

        boolean foundCapital = false;
        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }

            TitleIdentity title = resolveTitle(level, capital, entityId);
            OfficeIdentity office = resolveOffice(level, capital, entityId);
            if (title != TitleIdentity.COMMONER
                    || office != OfficeIdentity.NONE && office != OfficeIdentity.MASTER_OF_LAWS) {
                return false;
            }

            if (capital.containsEntity(entityId)) {
                foundCapital = true;
            }
        }

        return foundCapital || CapitalTitleResolver.findCapitalForEntity(entityId) == null;
    }

    public static boolean isHighSovereign(CapitalRecord capital, UUID entityId) {
        if (capital == null || entityId == null || !entityId.equals(capital.getPlayerSovereignId())) {
            return false;
        }

        int sovereignCount = 0;
        for (CapitalRecord record : CapitalManager.getAllCapitalRecords()) {
            if (record != null
                    && record.getState() == CapitalState.ACTIVE
                    && entityId.equals(record.getPlayerSovereignId())) {
                sovereignCount++;
                if (sovereignCount >= 2) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isLocalSovereign(CapitalRecord capital, UUID entityId) {
        return entityId.equals(capital.getSovereign())
                || entityId.equals(capital.getPlayerSovereignId());
    }

    private static boolean isMarriageLord(ServerLevel level, CapitalRecord capital, UUID entityId) {
        UUID spouse = MCAIntegrationBridge.getSpouse(level, entityId);
        return spouse != null && capital.isLord(spouse);
    }

    private static boolean isRoyalBloodChildOfCurrentLine(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (capital.isDisinheritedRoyalChild(entityId)) {
            return false;
        }

        if (capital.isRoyalChild(entityId)) {
            return true;
        }

        UUID sovereign = capital.getSovereign();
        return sovereign != null && MCAIntegrationBridge.isChildOf(level, entityId, sovereign);
    }

    private static boolean isDynasticPrinceOrPrincess(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (capital.isDisinheritedRoyalChild(entityId)) {
            return false;
        }

        if (entityId.equals(capital.getHeir())
                && capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL
                && !isRoyalBloodChildOfCurrentLine(level, capital, entityId)) {
            return false;
        }

        if (capital.isRoyalChild(entityId) || capital.isLegitimizedRoyalChild(entityId)) {
            return true;
        }

        UUID sovereign = capital.getSovereign();
        return sovereign != null && MCAIntegrationBridge.isChildOf(level, entityId, sovereign);
    }
}
