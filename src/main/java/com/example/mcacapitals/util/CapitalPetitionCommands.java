package com.example.mcacapitals.util;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public final class CapitalPetitionCommands {

    private CapitalPetitionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Petition handling is now driven through MCA's native dialogue flow.
    }
}