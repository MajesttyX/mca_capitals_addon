package com.example.mcacapitals.noble;

public final class ConsortTitleResolver {

    private ConsortTitleResolver() {
    }

    public static NobleTitle getSpouseTitle(NobleTitle spouseTitle, boolean spouseIsFemale) {
        if (spouseTitle == null) {
            return NobleTitle.COMMONER;
        }

        return switch (spouseTitle) {
            case KING -> NobleTitle.QUEEN;
            case QUEEN -> NobleTitle.KING;
            case PRINCE -> NobleTitle.PRINCESS;
            case PRINCESS -> NobleTitle.PRINCE;
            case ARCHDUKE -> NobleTitle.ARCHDUCHESS;
            case ARCHDUCHESS -> NobleTitle.ARCHDUKE;
            case DUKE -> NobleTitle.DUCHESS;
            case DUCHESS -> NobleTitle.DUKE;
            case KNIGHT -> NobleTitle.DAME;
            case DAME -> NobleTitle.KNIGHT;
            case LORD -> NobleTitle.LADY;
            case LADY -> NobleTitle.LORD;
            default -> NobleTitle.COMMONER;
        };
    }
}
