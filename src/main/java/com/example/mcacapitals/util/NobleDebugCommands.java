package com.example.mcacapitals.util;

import com.example.mcacapitals.noble.NobleManager;
import com.example.mcacapitals.noble.NobleTitle;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class NobleDebugCommands {

    private NobleDebugCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("noble")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("title")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    UUID id = player.getUUID();

                                    NobleTitle title = NobleManager.getTitle(id);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Your noble title is: " + title),
                                            false
                                    );

                                    return 1;
                                }))

                        .then(Commands.literal("clear")
                                .executes(context -> {

                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    UUID id = player.getUUID();

                                    NobleManager.setTitle(id, NobleTitle.COMMONER);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Your title has been cleared."),
                                            true
                                    );

                                    return 1;
                                }))

                        .then(Commands.literal("set")
                                .then(Commands.argument("title", StringArgumentType.word())
                                        .executes(context -> {

                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            UUID id = player.getUUID();

                                            String input = StringArgumentType.getString(context, "title").toUpperCase();

                                            NobleTitle title;

                                            try {
                                                title = NobleTitle.valueOf(input);
                                            } catch (Exception e) {
                                                context.getSource().sendFailure(Component.literal("Invalid title."));
                                                return 0;
                                            }

                                            NobleManager.setTitle(id, title);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Title set to: " + title),
                                                    true
                                            );

                                            return 1;
                                        })))
                        .then(Commands.literal("uuid")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Your UUID is: " + player.getUUID()),
                                            false
                                    );

                                    return 1;
                                }))
        );
    }
}