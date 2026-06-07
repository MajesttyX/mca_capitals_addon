package com.majesttyx.mcacapitals.util;

import net.minecraft.server.MinecraftServer;

public final class FabricServerAccess {

    private static MinecraftServer currentServer;

    private FabricServerAccess() {
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }

    public static void setServer(MinecraftServer server) {
        currentServer = server;
    }

    public static void clearServer(MinecraftServer server) {
        if (currentServer == server) {
            currentServer = null;
        }
    }
}