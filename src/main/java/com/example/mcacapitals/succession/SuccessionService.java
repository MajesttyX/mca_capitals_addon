package com.example.mcacapitals.succession;

import com.example.mcacapitals.capital.CapitalRecord;
import com.example.mcacapitals.noble.NobleManager;
import com.example.mcacapitals.noble.NobleTitle;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SuccessionService {

    private SuccessionService() {
    }

    public static UUID determineNextSovereign(CapitalRecord capital) {
        Set<UUID> children = capital.getRoyalChildren();

        Optional<UUID> next = children.stream()
                .filter(id -> {
                    NobleTitle title = NobleManager.getTitle(id);
                    return title == NobleTitle.PRINCE || title == NobleTitle.PRINCESS;
                })
                .sorted(Comparator.comparing(UUID::toString))
                .findFirst();

        return next.orElse(null);
    }

    public static void applySuccession(CapitalRecord capital) {
        UUID nextSovereign = determineNextSovereign(capital);
        if (nextSovereign == null) {
            return;
        }

        capital.setSovereign(nextSovereign);

        NobleTitle newTitle = NobleManager.getTitle(nextSovereign) == NobleTitle.PRINCESS
                ? NobleTitle.QUEEN
                : NobleTitle.KING;

        NobleManager.setTitle(nextSovereign, newTitle);
    }
}