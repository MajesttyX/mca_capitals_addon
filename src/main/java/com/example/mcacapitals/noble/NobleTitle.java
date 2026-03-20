package com.example.mcacapitals.noble;

public enum NobleTitle {
    COMMONER,

    LORD,
    LADY,

    KNIGHT,
    DAME,

    DUKE,
    DUCHESS,

    ARCHDUKE,
    ARCHDUCHESS,

    PRINCE,
    PRINCESS,

    KING,
    QUEEN;

    public boolean isSovereign() {
        return this == KING || this == QUEEN;
    }

    public boolean isRoyal() {
        return isSovereign() || this == PRINCE || this == PRINCESS;
    }

    public boolean isNoble() {
        return this != COMMONER;
    }
}
