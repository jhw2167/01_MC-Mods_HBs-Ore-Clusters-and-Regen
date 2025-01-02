package com.holybuckets.foundation.event;

import com.holybuckets.foundation.LoggerBase;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

//Project imports

public class CommandRegistry {

    private static final String CLASS_ID = "011";
    //private static final Deque<Consumer<CommandSourceStack>> COMMANDS = new ArrayDeque<>();
    private static final Deque<Supplier<LiteralArgumentBuilder<CommandSourceStack>>> COMMANDS = new ArrayDeque<>();

    //Package private
    static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LoggerBase.logDebug(null, "011001", "Registering Commands " + COMMANDS.size());
        COMMANDS.forEach(command -> dispatcher.register(command.get()));
        LoggerBase.logDebug(null, "011002", "Finished Registering Commands " + COMMANDS.size());
    }

    public static void register( Supplier<LiteralArgumentBuilder<CommandSourceStack>> command)
    {
        LoggerBase.logDebug(null, "011000", "Adding command" + COMMANDS.size());
        COMMANDS.add(command);
    }


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
