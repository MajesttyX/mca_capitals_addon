package com.example.mcacapitals.capital;

import com.example.mcacapitals.noble.NobleTitle;
import com.example.mcacapitals.player.PlayerCapitalTitleService;
import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class CapitalTitleResolver {

    private CapitalTitleResolver() {
    }

    public static String getDisplayTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return "None";
        }

        boolean female = isFemaleForTitle(level, capital, entityId);

        if (entityId.equals(capital.getSovereign())) {
            return female ? "Queen" : "King";
        }

        if (entityId.equals(capital.getConsort())) {
            return female ? "Queen Consort" : "King Consort";
        }

        if (entityId.equals(capital.getDowager())) {
            return female ? "Queen Dowager" : "Prince Consort";
        }

        if (entityId.equals(capital.getCommander()) || PlayerCapitalTitleService.isCommander(level, capital, entityId)) {
            return "Commander";
        }

        if (entityId.equals(capital.getHeir())) {
            if (capital.getHeirMode() == CapitalRecord.HeirMode.MANUAL) {
                return "Heir Apparent";
            }

            return female ? "Princess" : "Prince";
        }

        if (isDynasticPrinceOrPrincess(level, capital, entityId)) {
            return female ? "Princess" : "Prince";
        }

        if (capital.isRoyalGuard(entityId)) {
            return female ? "Dame" : "Sir";
        }

        if (capital.isDuke(entityId)) {
            return female ? "Duchess" : "Duke";
        }

        if (isMarriageDuke(level, capital, entityId)) {
            return female ? "Duchess" : "Duke";
        }

        NobleTitle grantedTitle = PlayerCapitalTitleService.getGrantedTitle(level, capital, entityId);
        if (grantedTitle == NobleTitle.DUKE || grantedTitle == NobleTitle.DUCHESS) {
            return female ? "Duchess" : "Duke";
        }

        if (capital.isKnight(entityId)) {
            return female ? "Dame" : "Sir";
        }

        if (capital.isLord(entityId)) {
            return female ? "Lady" : "Lord";
        }

        if (isMarriageLord(level, capital, entityId)) {
            return female ? "Lady" : "Lord";
        }

        if (grantedTitle == NobleTitle.LORD || grantedTitle == NobleTitle.LADY) {
            return female ? "Lady" : "Lord";
        }

        return "Commoner";
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

        for (CapitalRecord capital : CapitalManager.getAllCapitalRecords()) {
            if (capital == null) {
                continue;
            }

            if (capital.containsEntity(entityId)) {
                return capital;
            }

            if (level != null && (
                    isMarriageDuke(level, capital, entityId)
                            || isMarriageLord(level, capital, entityId)
                            || PlayerCapitalTitleService.hasAnyOffice(level, capital, entityId)
            )) {
                return capital;
            }
        }

        return null;
    }

    public static String getDisplayTitleForEntity(ServerLevel level, UUID entityId) {
        CapitalRecord capital = findCapitalForEntity(level, entityId);
        if (capital == null) {
            return "Commoner";
        }

        return getDisplayTitle(level, capital, entityId);
    }

    private static boolean isFemaleForTitle(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (entityId == null || capital == null) {
            return false;
        }

        if (entityId.equals(capital.getSovereign())) {
            return capital.isSovereignFemale();
        }
        if (entityId.equals(capital.getConsort())) {
            return capital.isConsortFemale();
        }
        if (entityId.equals(capital.getDowager())) {
            return capital.isDowagerFemale();
        }
        if (entityId.equals(capital.getCommander())) {
            return capital.isCommanderFemale();
        }
        if (capital.isRoyalChild(entityId)) {
            return capital.isRoyalChildFemale(entityId);
        }
        if (capital.isRoyalGuard(entityId)) {
            return capital.isRoyalGuardFemale(entityId);
        }
        if (capital.isDuke(entityId)) {
            return capital.isDukeFemale(entityId);
        }
        if (capital.isKnight(entityId)) {
            return capital.isKnightFemale(entityId);
        }
        if (capital.isLord(entityId)) {
            return capital.isLordFemale(entityId);
        }

        return MCAIntegrationBridge.isFemale(level, entityId);
    }

    private static boolean isMarriageDuke(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (capital == null || entityId == null) {
            return false;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, entityId);
        return spouse != null && capital.isDuke(spouse);
    }

    private static boolean isMarriageLord(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (capital == null || entityId == null) {
            return false;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, entityId);
        return spouse != null && capital.isLord(spouse);
    }

    private static boolean isDynasticPrinceOrPrincess(ServerLevel level, CapitalRecord capital, UUID entityId) {
        if (capital == null || entityId == null) {
            return false;
        }

        if (capital.isRoyalChild(entityId)) {
            return true;
        }

        UUID sovereign = capital.getSovereign();
        if (sovereign != null && MCAIntegrationBridge.isChildOf(level, entityId, sovereign)) {
            return true;
        }

        UUID consort = capital.getConsort();
        if (consort != null && MCAIntegrationBridge.isChildOf(level, entityId, consort)) {
            return true;
        }

        UUID dowager = capital.getDowager();
        if (dowager != null && MCAIntegrationBridge.isChildOf(level, entityId, dowager)) {
            return true;
        }

        return false;
    }
}