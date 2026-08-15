package com.majesttyx.mcacapitals.util;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public final class CapitalDiplomacyCommands {

    private CapitalDiplomacyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CapitalDiplomaticAgreementCommands.register(dispatcher);
    }
}
