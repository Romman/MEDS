package org.meds.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.Player;
import org.meds.World;
import org.meds.net.ServerCommands;
import org.meds.net.ServerPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatHandler {

    private static Logger logger = LogManager.getLogger();

    public final static String Separator = "\u0002";
    public final static String MessageSeparator = "\u0030";
    public final static String SayChar = "\u0031";
    public final static String SystemChar = "\u0034";

    @Autowired
    private ChatCommandManager commandHandler;
    @Autowired
    private World world;

    public ServerPacket constructSystemMessage(String message) {
        return new ServerPacket(ServerCommands.ChatMessage)
                .add(new StringBuilder(Separator).append(SystemChar).append(message));
    }

    public void sendSystemMessage(Player player, String message) {
        if (player == null || player.getSession() == null || message == null || message.length() == 0) {
            return;
        }

        player.getSession().send(constructSystemMessage(message));
    }

    public void sendSystemMessage(String message) {
        if (message == null || message.length() == 0)
            return;

        world.send(constructSystemMessage(message));
    }

    public void handleSay(Player player, String message) {
        // Ignore empty messages
        if (message == null || message.length() == 0) {
            return;
        }

        message = message.trim();

        // Message contains only whitespaces
        if (message.isEmpty()) {
            return;
        }

        // Player is located nowhere
        if (player.getPosition() == null) {
            logger.warn("{} tries to speak in chat, but he is not on the map (no location specified). Skipped.",
                    player);
            return;
        }

        // Is a command
        if (message.charAt(0) == '\\' && message.length() > 1) {
            String text = message.substring(1);
            String[] commandData = text.split(" ");
            String args;
            if (commandData.length > 1) {
                args = message.substring(commandData[0].length() + 2);
            } else {
                args = "";
            }
            commandHandler.handle(player, commandData[0], args);
            return;
        }

        // Say this message
        ServerPacket packet = new ServerPacket(ServerCommands.ChatMessage);
        StringBuilder response = new StringBuilder()
                .append(Separator)
                .append(SayChar)
                .append("[")
                .append(player.getName())
                .append("]: ")
                .append(Separator)
                .append(MessageSeparator)
                .append(message);
        packet.add(response);

        // Send to all at the player's region
        player.getPosition().getRegion().send(packet);
    }

    public void handleWhisper(Player player, String message) {
        // TODO: Implement whispering
    }

}
