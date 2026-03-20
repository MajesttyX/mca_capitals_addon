package com.example.mcacapitals.capital;

import java.util.UUID;

public class CourtAssignmentService {

    private CourtAssignmentService() {
    }

    public static void assignRoyalChild(CapitalRecord capital, UUID villagerId, boolean female) {
        capital.addRoyalChild(villagerId, female);
    }

    public static void assignDuke(CapitalRecord capital, UUID villagerId, boolean female) {
        capital.addDuke(villagerId, female);
    }

    public static void assignLord(CapitalRecord capital, UUID villagerId, boolean female) {
        capital.addLord(villagerId, female);
    }

    public static void assignKnight(CapitalRecord capital, UUID villagerId, boolean female) {
        capital.addKnight(villagerId, female);
    }

    public static void clearDynamicAssignments(CapitalRecord capital) {
        // No longer needed because court rebuilding is now atomic.
        // This method intentionally does nothing.
    }
}