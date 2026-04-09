package com.example.mcacapitals.capital;

import java.util.UUID;

public class CapitalRoyalHouseholdService {

    private CapitalRoyalHouseholdService() {
    }

    public static void refreshDynasticHousehold(CapitalRecord capital) {
        if (capital == null) {
            return;
        }

        capital.clearRoyalHousehold();
        addCurrentCourtRoles(capital);

        for (UUID id : capital.getRoyalChildren()) {
            capital.addRoyalHouseholdMember(id);
        }

        for (UUID id : capital.getDisinheritedRoyalChildren()) {
            capital.addRoyalHouseholdMember(id);
        }

        for (UUID id : capital.getLegitimizedRoyalChildren()) {
            capital.addRoyalHouseholdMember(id);
        }
    }

    public static void beginNewRegime(CapitalRecord capital) {
        if (capital == null) {
            return;
        }

        capital.clearRoyalHousehold();
        addCurrentCourtRoles(capital);
    }

    public static boolean isRoyalHouseholdMember(CapitalRecord capital, UUID villagerId) {
        return capital != null && villagerId != null && capital.isRoyalHouseholdMember(villagerId);
    }

    private static void addCurrentCourtRoles(CapitalRecord capital) {
        add(capital, capital.getSovereign());
        add(capital, capital.getConsort());
        add(capital, capital.getDowager());
        add(capital, capital.getHeir());
    }

    private static void add(CapitalRecord capital, UUID id) {
        if (id != null) {
            capital.addRoyalHouseholdMember(id);
        }
    }
}