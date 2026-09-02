package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.noble.NobleTitle;
import com.majesttyx.mcacapitals.player.PlayerCapitalTitleService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class CapitalTitleResolver {

    private CapitalTitleResolver() {
    }

    public static String getDisplayTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        return getDisplayTitleComponent(level, capital, entityId).getString();
    }

    public static Component getDisplayTitleComponent(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return Component.translatable("mcacapitals.dynamic.title.none");
        }

        ResolvedTitle resolved = resolveLocalTitle(level, capital, entityId);
        return displayTitleComponent(level, resolved.capital(), entityId, resolved.id());
    }

    public static ResolvedTitleId getResolvedTitleId(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return ResolvedTitleId.NONE;
        }

        return resolveLocalTitle(level, capital, entityId).id();
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
            if (resolved.rank() >= ResolvedTitleId.COMMONER.rankValue()) {
                continue;
            }

            if (best == null || resolved.rank() < best.rank()) {
                best = resolved;
            }
        }

        return best == null ? null : best.capital();
    }

    public static String getDisplayTitleForEntity(ServerLevel level, UUID entityId) {
        return getDisplayTitleComponentForEntity(level, entityId).getString();
    }

    public static Component getDisplayTitleComponentForEntity(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return Component.translatable("mcacapitals.dynamic.title.commoner");
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

        if (best == null) {
            return Component.translatable("mcacapitals.dynamic.title.commoner");
        }

        return displayTitleComponent(level, best.capital(), entityId, best.id());
    }

    public static ResolvedTitleId getResolvedTitleIdForEntity(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return ResolvedTitleId.COMMONER;
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

        return best == null ? ResolvedTitleId.COMMONER : best.id();
    }

    public static String getCourtOfficeLineForEntity(ServerLevel level, UUID entityId) {
        return getCourtOfficeComponentForEntity(level, entityId).getString();
    }

    public static Component getCourtOfficeComponentForEntity(ServerLevel level, UUID entityId) {
        SecondaryOfficeId officeId = getCourtOfficeLineIdForEntity(level, entityId);
        return switch (officeId) {
            case MASTER_OF_LAWS -> Component.translatable("mcacapitals.dynamic.office.master_of_laws");
            case AMBASSADOR -> Component.translatable("mcacapitals.dynamic.office.ambassador");
            case NONE -> Component.empty();
        };
    }

    public static SecondaryOfficeId getCourtOfficeLineIdForEntity(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return SecondaryOfficeId.NONE;
        }

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }

            if (entityId.equals(capital.getMasterOfLaws())) {
                return SecondaryOfficeId.MASTER_OF_LAWS;
            }

            if (CapitalAmbassadorService.isAmbassador(level, capital, entityId)) {
                return SecondaryOfficeId.AMBASSADOR;
            }
        }

        return SecondaryOfficeId.NONE;
    }

    public static boolean isRoyalGuardTitle(ServerLevel level, UUID entityId) {
        if (entityId == null) {
            return false;
        }

        CapitalRecord capital = findCapitalForEntity(level, entityId);
        if (capital == null || !capital.isRoyalGuard(entityId)) {
            return false;
        }

        return getResolvedTitleId(level, capital, entityId) == ResolvedTitleId.ROYAL_GUARD;
    }

    private static ResolvedTitle resolveLocalTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return new ResolvedTitle(ResolvedTitleId.NONE, capital);
        }

        if (isHighSovereign(level, capital, entityId)) {
            return new ResolvedTitle(ResolvedTitleId.HIGH_SOVEREIGN, capital);
        }

        if (isLocalSovereign(capital, entityId)) {
            return new ResolvedTitle(ResolvedTitleId.SOVEREIGN, capital);
        }

        if (entityId.equals(capital.getConsort()) || entityId.equals(capital.getPlayerConsortId())) {
            return new ResolvedTitle(ResolvedTitleId.SOVEREIGN_CONSORT, capital);
        }

        if (entityId.equals(capital.getDowager())) {
            return new ResolvedTitle(ResolvedTitleId.SOVEREIGN_DOWAGER, capital);
        }

        if (entityId.equals(capital.getHeir())) {
            if (capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL && !isRoyalBloodChildOfCurrentLine(level, capital, entityId)) {
                return new ResolvedTitle(ResolvedTitleId.HEIR_APPARENT, capital);
            }
            return new ResolvedTitle(ResolvedTitleId.CROWN_HEIR, capital);
        }

        if (isDynasticPrinceOrPrincess(level, capital, entityId)) {
            return new ResolvedTitle(ResolvedTitleId.ROYAL_CHILD, capital);
        }

        NobleTitle marriageTitle = PlayerCapitalTitleService.getMarriageTitle(level, capital, entityId);
        if (capital.isPrinceConsort(entityId) || marriageTitle == NobleTitle.PRINCE || marriageTitle == NobleTitle.PRINCESS) {
            return new ResolvedTitle(ResolvedTitleId.PRINCE_CONSORT, capital);
        }

        NobleTitle dowagerBaseTitle = PlayerCapitalTitleService.getDowagerBaseTitle(level, capital, entityId);
        if (capital.isDowagerPrince(entityId) || dowagerBaseTitle == NobleTitle.PRINCE || dowagerBaseTitle == NobleTitle.PRINCESS) {
            return new ResolvedTitle(ResolvedTitleId.DOWAGER_PRINCE, capital);
        }

        if (entityId.equals(capital.getHand()) || PlayerCapitalTitleService.isHand(level, capital, entityId)) {
            return new ResolvedTitle(ResolvedTitleId.HAND, capital);
        }

        if (entityId.equals(capital.getGrandMaester())) {
            return new ResolvedTitle(ResolvedTitleId.GRAND_MAESTER, capital);
        }

        if (entityId.equals(capital.getHerald())) {
            return new ResolvedTitle(ResolvedTitleId.COURT_HERALD, capital);
        }

        if (capital.isDuke(entityId) || capital.isMarriageDuke(entityId) || marriageTitle == NobleTitle.DUKE || marriageTitle == NobleTitle.DUCHESS) {
            return new ResolvedTitle(ResolvedTitleId.DUKE, capital);
        }

        NobleTitle grantedTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, entityId);
        if (grantedTitle == NobleTitle.DUKE || grantedTitle == NobleTitle.DUCHESS) {
            return new ResolvedTitle(ResolvedTitleId.DUKE, capital);
        }

        if (capital.isDowagerDuke(entityId) || dowagerBaseTitle == NobleTitle.DUKE || dowagerBaseTitle == NobleTitle.DUCHESS) {
            return new ResolvedTitle(ResolvedTitleId.DOWAGER_DUKE, capital);
        }

        if (CapitalMaesterSelection.isMaester(level, capital, entityId, CapitalResidentScanner.scanResidents(level, capital.getCapitalId()))) {
            return new ResolvedTitle(ResolvedTitleId.MAESTER, capital);
        }

        if (entityId.equals(capital.getCommander()) || PlayerCapitalTitleService.isCommander(level, capital, entityId)) {
            return new ResolvedTitle(ResolvedTitleId.LORD_COMMANDER, capital);
        }

        if (capital.isRoyalGuard(entityId)) {
            return new ResolvedTitle(ResolvedTitleId.ROYAL_GUARD, capital);
        }

        if (capital.isLord(entityId)) {
            return new ResolvedTitle(ResolvedTitleId.LORD, capital);
        }

        if (marriageTitle == NobleTitle.LORD || marriageTitle == NobleTitle.LADY) {
            return new ResolvedTitle(ResolvedTitleId.LORD, capital);
        }

        if (isMarriageLord(level, capital, entityId)) {
            return new ResolvedTitle(ResolvedTitleId.LORD, capital);
        }

        if (grantedTitle == NobleTitle.LORD || grantedTitle == NobleTitle.LADY) {
            return new ResolvedTitle(ResolvedTitleId.LORD, capital);
        }

        if (capital.isKnight(entityId)) {
            return new ResolvedTitle(ResolvedTitleId.KNIGHT, capital);
        }

        if (CapitalAmbassadorService.isAmbassador(level, capital, entityId)) {
            return new ResolvedTitle(ResolvedTitleId.AMBASSADOR, capital);
        }

        return new ResolvedTitle(ResolvedTitleId.COMMONER, capital);
    }

    private static Component displayTitleComponent(
            ServerLevel level,
            CapitalRecord capital,
            UUID entityId,
            ResolvedTitleId titleId
    ) {
        boolean female = isFemaleForTitle(level, capital, entityId);

        return switch (titleId) {
            case NONE -> Component.translatable("mcacapitals.dynamic.title.none");
            case COMMONER -> Component.translatable("mcacapitals.dynamic.title.commoner");
            case AMBASSADOR -> Component.translatable("mcacapitals.dynamic.office.ambassador");
            case KNIGHT, ROYAL_GUARD -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.knight.female"
                            : "mcacapitals.dynamic.title.knight.male"
            );
            case LORD -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.lord.female"
                            : "mcacapitals.dynamic.title.lord.male"
            );
            case LORD_COMMANDER -> Component.translatable("mcacapitals.dynamic.office.lord_commander");
            case MAESTER -> Component.translatable("mcacapitals.dynamic.office.maester");
            case DOWAGER_DUKE -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.dowager_duke.female"
                            : "mcacapitals.dynamic.title.dowager_duke.male"
            );
            case DUKE -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.duke.female"
                            : "mcacapitals.dynamic.title.duke.male"
            );
            case GRAND_MAESTER -> Component.translatable("mcacapitals.dynamic.office.grand_maester");
            case HAND -> Component.translatable(
                    isCurrentSovereignFemale(level, capital)
                            ? "mcacapitals.dynamic.office.hand.female"
                            : "mcacapitals.dynamic.office.hand.male"
            );
            case DOWAGER_PRINCE -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.dowager_prince.female"
                            : "mcacapitals.dynamic.title.dowager_prince.male"
            );
            case PRINCE_CONSORT -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.prince_consort.female"
                            : "mcacapitals.dynamic.title.prince_consort.male"
            );
            case ROYAL_CHILD -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.royal_child.female"
                            : "mcacapitals.dynamic.title.royal_child.male"
            );
            case CROWN_HEIR -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.crown_heir.female"
                            : "mcacapitals.dynamic.title.crown_heir.male"
            );
            case HEIR_APPARENT -> Component.translatable("mcacapitals.dynamic.title.heir_apparent");
            case SOVEREIGN_DOWAGER -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.sovereign_dowager.female"
                            : "mcacapitals.dynamic.title.sovereign_dowager.male"
            );
            case SOVEREIGN_CONSORT -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.sovereign_consort.female"
                            : "mcacapitals.dynamic.title.sovereign_consort.male"
            );
            case SOVEREIGN -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.sovereign.female"
                            : "mcacapitals.dynamic.title.sovereign.male"
            );
            case HIGH_SOVEREIGN -> Component.translatable(
                    female
                            ? "mcacapitals.dynamic.title.high_sovereign.female"
                            : "mcacapitals.dynamic.title.high_sovereign.male"
            );
            case COURT_HERALD -> Component.translatable("mcacapitals.dynamic.office.court_herald");
        };
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

        UUID sovereignId = capital.getPlayerSovereignId() != null
                ? capital.getPlayerSovereignId()
                : capital.getSovereign();
        java.util.Optional<Boolean> mcaGender = MCAIntegrationBridge.getFemaleIfKnown(level, sovereignId);
        if (mcaGender.isPresent()) {
            return mcaGender.get();
        }

        return capital.isSovereignFemale();
    }

    private static boolean isFemaleForTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return false;
        }

        java.util.Optional<Boolean> mcaGender = MCAIntegrationBridge.getFemaleIfKnown(level, entityId);
        if (mcaGender.isPresent()) {
            return mcaGender.get();
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

    public enum SecondaryOfficeId {
        NONE,
        MASTER_OF_LAWS,
        AMBASSADOR
    }

    public enum ResolvedTitleId {
        NONE(999),
        COMMONER(900),

        AMBASSADOR(195),
        KNIGHT(180),
        LORD(170),
        ROYAL_GUARD(160),
        LORD_COMMANDER(150),
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
        COURT_HERALD(190);

        private final int rankValue;

        ResolvedTitleId(int rankValue) {
            this.rankValue = rankValue;
        }

        public int rankValue() {
            return rankValue;
        }
    }

    private record ResolvedTitle(ResolvedTitleId id, CapitalRecord capital) {
        private int rank() {
            return id.rankValue();
        }
    }
}
