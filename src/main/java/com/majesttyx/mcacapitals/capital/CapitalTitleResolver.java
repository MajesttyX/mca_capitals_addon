package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class CapitalTitleResolver {

    private CapitalTitleResolver() {
    }

    public static String getDisplayTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return "None";
        }

        return resolveLocalTitle(level, capital, entityId).title();
    }

    public static CapitalRecord findCapitalForEntity(UUID entityId) {
        if (entityId == null) {
            return null;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital != null && capital.containsEntity(entityId)) {
                return capital;
            }
        }

        return null;
    }

    public static CapitalRecord findCapitalForEntity(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return null;
        }

        ResolvedTitle best = null;

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }

            ResolvedTitle resolved = resolveLocalTitle(level, capital, entityId);
            if (resolved.rank() >= TitleRank.COMMONER.rankValue()) {
                continue;
            }

            if (best == null || resolved.rank() < best.rank()) {
                best = resolved;
            }
        }

        return best == null ? null : best.capital();
    }

    public static String getDisplayTitleForEntity(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return "Commoner";
        }

        ResolvedTitle best = null;

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }

            ResolvedTitle resolved = resolveLocalTitle(level, capital, entityId);
            if (best == null || resolved.rank() < best.rank()) {
                best = resolved;
            }
        }

        return best == null ? "Commoner" : best.title();
    }

    public static String getCourtOfficeLineForEntity(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return "";
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }

            if (entityId.equals(capital.getMasterOfLaws())) {
                return "Master of Laws";
            }

            if (CapitalAmbassadorService.isAmbassador(level, capital, entityId)) {
                return "Ambassador";
            }
        }

        return "";
    }

    public static boolean isRoyalGuardTitle(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return false;
        }

        CapitalRecord capital = findCapitalForEntity(level, entityId);
        if (capital == null || !capital.isRoyalGuard(entityId)) {
            return false;
        }

        String title = getDisplayTitle(level, capital, entityId);
        return "Sir".equals(title) || "Dame".equals(title);
    }

    private static ResolvedTitle resolveLocalTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return new ResolvedTitle("None", TitleRank.NONE.rankValue(), capital);
        }

        boolean female = isFemaleForTitle(level, capital, entityId);

        if (isHighSovereign(level, capital, entityId)) {
            return new ResolvedTitle(female ? "High Queen" : "High King", TitleRank.HIGH_SOVEREIGN.rankValue(), capital);
        }

        if (isLocalSovereign(capital, entityId)) {
            return new ResolvedTitle(female ? "Queen" : "King", TitleRank.SOVEREIGN.rankValue(), capital);
        }

        if (entityId.equals(capital.getConsort()) || entityId.equals(capital.getPlayerConsortId())) {
            return new ResolvedTitle(female ? "Queen Consort" : "King Consort", TitleRank.SOVEREIGN_CONSORT.rankValue(), capital);
        }

        if (entityId.equals(capital.getDowager())) {
            return new ResolvedTitle(female ? "Dowager Queen" : "Dowager King", TitleRank.SOVEREIGN_DOWAGER.rankValue(), capital);
        }

        if (entityId.equals(capital.getHeir())) {
            if (capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL && !isRoyalBloodChildOfCurrentLine(level, capital, entityId)) {
                return new ResolvedTitle("Heir Apparent", TitleRank.HEIR_APPARENT.rankValue(), capital);
            }
            return new ResolvedTitle(female ? "Crown Princess" : "Crown Prince", TitleRank.CROWN_HEIR.rankValue(), capital);
        }

        if (isDynasticPrinceOrPrincess(level, capital, entityId)) {
            return new ResolvedTitle(female ? "Princess" : "Prince", TitleRank.ROYAL_CHILD.rankValue(), capital);
        }

        NobleTitle marriageTitle = PlayerCapitalTitleService.getMarriageTitle(level, capital, entityId);
        if (capital.isPrinceConsort(entityId) || marriageTitle == NobleTitle.PRINCE || marriageTitle == NobleTitle.PRINCESS) {
            return new ResolvedTitle(female ? "Princess Consort" : "Prince Consort", TitleRank.PRINCE_CONSORT.rankValue(), capital);
        }

        NobleTitle dowagerBaseTitle = PlayerCapitalTitleService.getDowagerBaseTitle(level, capital, entityId);
        if (capital.isDowagerPrince(entityId) || dowagerBaseTitle == NobleTitle.PRINCE || dowagerBaseTitle == NobleTitle.PRINCESS) {
            return new ResolvedTitle(female ? "Dowager Princess" : "Dowager Prince", TitleRank.DOWAGER_PRINCE.rankValue(), capital);
        }

        if (entityId.equals(capital.getHand()) || PlayerCapitalTitleService.isHand(level, capital, entityId)) {
            return new ResolvedTitle(isCurrentSovereignFemale(level, capital) ? "Hand of the Queen" : "Hand of the King", TitleRank.HAND.rankValue(), capital);
        }

        if (entityId.equals(capital.getGrandMaester())) {
            return new ResolvedTitle("Grand Maester", TitleRank.GRAND_MAESTER.rankValue(), capital);
        }

        if (entityId.equals(capital.getHerald())) {
            return new ResolvedTitle("Court Herald", TitleRank.HERALD.rankValue(), capital);
        }

        if (capital.isDuke(entityId) || capital.isMarriageDuke(entityId) || marriageTitle == NobleTitle.DUKE || marriageTitle == NobleTitle.DUCHESS) {
            return new ResolvedTitle(female ? "Duchess" : "Duke", TitleRank.DUKE.rankValue(), capital);
        }

        NobleTitle grantedTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, entityId);
        if (grantedTitle == NobleTitle.DUKE || grantedTitle == NobleTitle.DUCHESS) {
            return new ResolvedTitle(female ? "Duchess" : "Duke", TitleRank.DUKE.rankValue(), capital);
        }

        if (capital.isDowagerDuke(entityId) || dowagerBaseTitle == NobleTitle.DUKE || dowagerBaseTitle == NobleTitle.DUCHESS) {
            return new ResolvedTitle(female ? "Dowager Duchess" : "Dowager Duke", TitleRank.DOWAGER_DUKE.rankValue(), capital);
        }

        if (CapitalMaesterSelection.isMaester(level, capital, entityId, CapitalResidentScanner.scanResidents(level, capital.getCapitalId()))) {
            return new ResolvedTitle("Maester", TitleRank.MAESTER.rankValue(), capital);
        }

        if (entityId.equals(capital.getCommander()) || PlayerCapitalTitleService.isCommander(level, capital, entityId)) {
            return new ResolvedTitle("Lord Commander", TitleRank.COMMANDER.rankValue(), capital);
        }

        if (capital.isRoyalGuard(entityId)) {
            return new ResolvedTitle(female ? "Dame" : "Sir", TitleRank.ROYAL_GUARD.rankValue(), capital);
        }

        if (capital.isLord(entityId)) {
            return new ResolvedTitle(female ? "Lady" : "Lord", TitleRank.LORD.rankValue(), capital);
        }

        if (marriageTitle == NobleTitle.LORD || marriageTitle == NobleTitle.LADY) {
            return new ResolvedTitle(female ? "Lady" : "Lord", TitleRank.LORD.rankValue(), capital);
        }

        if (isMarriageLord(level, capital, entityId)) {
            return new ResolvedTitle(female ? "Lady" : "Lord", TitleRank.LORD.rankValue(), capital);
        }

        if (grantedTitle == NobleTitle.LORD || grantedTitle == NobleTitle.LADY) {
            return new ResolvedTitle(female ? "Lady" : "Lord", TitleRank.LORD.rankValue(), capital);
        }

        if (capital.isKnight(entityId)) {
            return new ResolvedTitle(female ? "Dame" : "Sir", TitleRank.KNIGHT.rankValue(), capital);
        }

        if (CapitalAmbassadorService.isAmbassador(level, capital, entityId)) {
            return new ResolvedTitle("Ambassador", TitleRank.AMBASSADOR.rankValue(), capital);
        }

        return new ResolvedTitle("Commoner", TitleRank.COMMONER.rankValue(), capital);
    }

    private static boolean isLocalSovereign(CapitalRecord capital, UUID entityId) {
        if (capital == null || entityId == null) {
            return false;
        }

        return entityId.equals(capital.getSovereign())
                || entityId.equals(capital.getPlayerSovereignId());
    }

    private static boolean isHighSovereign(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (level == null || capital == null || entityId == null) {
            return false;
        }
        if (!entityId.equals(capital.getPlayerSovereignId())) {
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

    private static boolean isCurrentSovereignFemale(ServerLevel level, CapitalRecord capital) {
        if (capital == null) {
            return false;
        }
        if (capital.getPlayerSovereignId() != null) {
            return MCAIntegrationBridge.isFemale(level, capital.getPlayerSovereignId());
        }
        return capital.isSovereignFemale();
    }

    private static boolean isFemaleForTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return false;
        }

        if (entityId.equals(capital.getSovereign())) {
            return capital.isSovereignFemale();
        }
        if (entityId.equals(capital.getPlayerSovereignId())) {
            return MCAIntegrationBridge.isFemale(level, entityId);
        }
        if (entityId.equals(capital.getConsort())) {
            return capital.isConsortFemale();
        }
        if (entityId.equals(capital.getPlayerConsortId())) {
            return MCAIntegrationBridge.isFemale(level, entityId);
        }
        if (entityId.equals(capital.getDowager())) {
            return capital.isDowagerFemale();
        }
        if (entityId.equals(capital.getCommander())) {
            return capital.isCommanderFemale();
        }
        if (entityId.equals(capital.getHand())) {
            return capital.isHandFemale();
        }
        if (entityId.equals(capital.getHerald())) {
            return capital.isHeraldFemale();
        }
        if (entityId.equals(capital.getGrandMaester())) {
            return capital.isGrandMaesterFemale();
        }
        if (capital.isRoyalChild(entityId)) {
            return capital.isRoyalChildFemale(entityId);
        }
        if (capital.isPrinceConsort(entityId)) {
            return capital.isPrinceConsortFemale(entityId);
        }
        if (capital.isRoyalGuard(entityId)) {
            return capital.isRoyalGuardFemale(entityId);
        }
        if (capital.isDuke(entityId)) {
            return capital.isDukeFemale(entityId);
        }
        if (capital.isMarriageDuke(entityId)) {
            return capital.isMarriageDukeFemale(entityId);
        }
        if (capital.isDowagerDuke(entityId)) {
            return capital.isDowagerDukeFemale(entityId);
        }
        if (capital.isDowagerPrince(entityId)) {
            return capital.isDowagerPrinceFemale(entityId);
        }
        if (capital.isKnight(entityId)) {
            return capital.isKnightFemale(entityId);
        }
        if (capital.isLord(entityId)) {
            return capital.isLordFemale(entityId);
        }

        return MCAIntegrationBridge.isFemale(level, entityId);
    }

    private static boolean isMarriageLord(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (capital == null || entityId == null) {
            return false;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, entityId);
        return spouse != null && capital.isLord(spouse);
    }

    private static boolean isRoyalBloodChildOfCurrentLine(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (capital == null || entityId == null || capital.isDisinheritedRoyalChild(entityId)) {
            return false;
        }

        if (capital.isRoyalChild(entityId)) {
            return true;
        }

        UUID sovereign = capital.getSovereign();
        return sovereign != null && MCAIntegrationBridge.isChildOf(level, entityId, sovereign);
    }

    private static boolean isDynasticPrinceOrPrincess(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (capital == null || entityId == null) {
            return false;
        }

        if (capital.isDisinheritedRoyalChild(entityId)) {
            return false;
        }

        if (entityId.equals(capital.getHeir()) && capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL && !isRoyalBloodChildOfCurrentLine(level, capital, entityId)) {
            return false;
        }

        if (capital.isRoyalChild(entityId) || capital.isLegitimizedRoyalChild(entityId)) {
            return true;
        }

        UUID sovereign = capital.getSovereign();
        return sovereign != null && MCAIntegrationBridge.isChildOf(level, entityId, sovereign);
    }

    private enum TitleRank {
        NONE(999),
        COMMONER(900),

        AMBASSADOR(195),
        KNIGHT(180),
        LORD(170),
        ROYAL_GUARD(160),
        COMMANDER(150),
        MAESTER(140),
        DOWAGER_DUKE(130),
        DUKE(120),
        GRAND_MAESTER(115),
        HAND(110),

        DOWAGER_PRINCE(100),
        PRINCE_CONSORT(90),
        ROYAL_CHILD(80),
        CROWN_HEIR(70),
        HEIR_APPARENT(60),

        SOVEREIGN_DOWAGER(50),
        SOVEREIGN_CONSORT(40),
        SOVEREIGN(30),
        HIGH_SOVEREIGN(20),
        HERALD(190);

        private final int rankValue;

        TitleRank(int rankValue) {
            this.rankValue = rankValue;
        }

        public int rankValue() {
            return rankValue;
        }
    }

    private record ResolvedTitle(String title, int rank, CapitalRecord capital) {
    }
}