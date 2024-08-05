package com.holybuckets.orecluster.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

//Project imports

public class ClientCommands {

    private static final String PREFIX = "hbMods";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal(PREFIX)
            .then(Commands.argument("command", StringArgumentType.string())
                .executes(context -> execute(context, ""))
            .then(Commands.argument("arg1", StringArgumentType.string())
                .executes(context -> execute(context, ""))
            .then(Commands.argument("arg2", StringArgumentType.string())
                .executes(context -> execute(context, ""))
            .then(Commands.argument("arg3", StringArgumentType.string())
                .executes(context -> execute(context, ""))
            .then(Commands.argument("arg4", StringArgumentType.string())
                .executes(context -> execute(context, "")))))))
            .executes(context -> execute(context, "")));

    }


    /**
     * Personal command execution boilerplate. There will only be one command for my mods
     * /HBCmd <command> <arg1> <arg2> <arg3> <arg4> which takes up to 4 possible arguments
     * @param context
     * @param defaultArg
     * @return
     * @throws CommandSyntaxException
     */
    private static final int totalArgs = 4;
    private static int execute(CommandContext<CommandSourceStack> context, String defaultArg ) throws CommandSyntaxException
    {
        String cmd = StringArgumentType.getString(context, "command");
        String[] args = new String[totalArgs];
        for( int i = 0; i < totalArgs; i++ )
        {
            try {
                args[i] = StringArgumentType.getString(context, "arg" + (i+1));
            }
            catch (IllegalArgumentException e) {
                args[i] = defaultArg;
            }
            cmd += " " + args[i];
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal("Received message: " + cmd));
        }
        return 1;
    }
}
