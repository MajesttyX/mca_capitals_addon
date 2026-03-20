package com.example.mcacapitals.client;

import com.example.mcacapitals.MCACapitals;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MCACapitals.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapitalNameTagHandler {

    private CapitalNameTagHandler() {
    }
}