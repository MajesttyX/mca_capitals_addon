package com.majesttyx.mcacapitals.house;

import java.util.UUID;

public record CapitalHouseHistoryEntry(
        CapitalHouseHistoryType type,
        long gameTime,
        UUID subjectId,
        UUID relatedHouseId
) {
}
