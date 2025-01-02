package com.holybuckets.foundation.client;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

/**
 * Description: Messaging class handles user friendly messaging via console or popup
 *
 * Singleton
 */
public class Messager {

    private static Messager INSTANCE;

    public static Messager getInstance() {
        if(INSTANCE == null)
            INSTANCE = new Messager();
        return INSTANCE;
    }

    private Messager() {
    }

    /**
     * Sends a message to the chat console for all players
     * @param message
     */
    public void sendChat(String message) {

    }

    /**
    * Sends a message to the chat console for a specific player
    * @param player
    * @param message
    */
    public void sendChat(ServerPlayer player, String message)
    {
        PlayerChatMessage msg = PlayerChatMessage.unsigned(player.getUUID(), message);
        player.createCommandSourceStack().sendChatMessage(
            new OutgoingChatMessage.Player( msg ),
            false,
            ChatType.bind(ChatType.CHAT, player));
    }


}
