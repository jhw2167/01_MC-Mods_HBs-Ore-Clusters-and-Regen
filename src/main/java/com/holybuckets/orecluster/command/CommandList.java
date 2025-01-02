package com.holybuckets.orecluster.command;

//Project imports

import com.holybuckets.foundation.event.CommandRegistry;
import com.holybuckets.orecluster.LoggerProject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class CommandList {

    public static final String CLASS_ID = "010";
    private static final String PREFIX = "hbOreClusters";

    public static void register() {
        CommandRegistry.register(LocateClusters::register);
    }

    //1. Locate Clusters
    private static class LocateClusters
    {
        private static LiteralArgumentBuilder<CommandSourceStack> register()
        {
            return Commands.literal(PREFIX)
                .then(Commands.literal("locateClusters")
                .executes(context -> execute()) );
        }

        private static int execute()
        {
            LoggerProject.logDebug("010001", "Locate Clusters Command");
            return 0;
        }
    }
    //END COMMAND




    //2. Locate Ore

    /*
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
    */



}
