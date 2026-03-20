package com.example.mcacapitals.marriage;

import com.example.mcacapitals.noble.MarriageTitleService;
import com.example.mcacapitals.noble.NobleTitle;

public class PlayerMarriageTitleResolver {

    private PlayerMarriageTitleResolver() {
    }

    public static NobleTitle resolveSovereignMarriageTitle(boolean playerIsFemale) {
        return MarriageTitleService.resolvePlayerSpouseTitle(playerIsFemale);
    }
}