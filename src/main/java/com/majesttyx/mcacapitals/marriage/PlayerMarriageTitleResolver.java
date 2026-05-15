package com.majesttyx.mcacapitals.marriage;

import com.majesttyx.mcacapitals.noble.MarriageTitleService;
import com.majesttyx.mcacapitals.noble.NobleTitle;

public class PlayerMarriageTitleResolver {

    private PlayerMarriageTitleResolver() {
    }

    public static NobleTitle resolveSovereignMarriageTitle(boolean playerIsFemale) {
        return MarriageTitleService.resolvePlayerSpouseTitle(playerIsFemale);
    }
}