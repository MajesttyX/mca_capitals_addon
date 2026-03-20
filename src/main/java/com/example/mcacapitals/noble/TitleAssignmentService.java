package com.example.mcacapitals.noble;

public class TitleAssignmentService {

    private TitleAssignmentService() {
    }

    public static NobleTitle getChildRoyalTitle(boolean childIsFemale) {
        return childIsFemale ? NobleTitle.PRINCESS : NobleTitle.PRINCE;
    }

    public static NobleTitle getKnightTitle(boolean isFemale) {
        return isFemale ? NobleTitle.DAME : NobleTitle.KNIGHT;
    }

    public static NobleTitle getLordTitle(boolean isFemale) {
        return isFemale ? NobleTitle.LADY : NobleTitle.LORD;
    }

    public static NobleTitle getDukeTitle(boolean isFemale) {
        return isFemale ? NobleTitle.DUCHESS : NobleTitle.DUKE;
    }

    public static NobleTitle getArchdukeTitle(boolean isFemale) {
        return isFemale ? NobleTitle.ARCHDUCHESS : NobleTitle.ARCHDUKE;
    }

    public static NobleTitle getSovereignTitle(boolean isFemale) {
        return isFemale ? NobleTitle.QUEEN : NobleTitle.KING;
    }
}
