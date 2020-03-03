package org.meds.chat.commands;

import java.util.Arrays;
import java.util.List;
import org.meds.Player;
import org.meds.chat.ChatHandler;
import org.meds.net.Session;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * @author Romman
 */
@ChatCommand("?")
public class HelpChatCommandHandler extends AbstractChatCommandHandler {

    @Autowired
    private ChatHandler chatHandler;

    private List<ServerMessage> helpCommandMessages;

    @PostConstruct
    private void init() {
        this.helpCommandMessages = Arrays.asList(
                chatHandler.constructSystemMessage("=============== GENERAL ==============="),
                chatHandler.constructSystemMessage("\\info"),
                chatHandler.constructSystemMessage("\\who"),
                chatHandler.constructSystemMessage("\\relax"),
                chatHandler.constructSystemMessage("\\notell"),
                chatHandler.constructSystemMessage("\\locborn"),
                chatHandler.constructSystemMessage("\\observe"),
                chatHandler.constructSystemMessage("\\scan"),
                chatHandler.constructSystemMessage("\\nomelee"),
                chatHandler.constructSystemMessage("\\balance"),
                chatHandler.constructSystemMessage("\\mail"),
                chatHandler.constructSystemMessage("\\powerup"),
                chatHandler.constructSystemMessage("\\replay"),
                chatHandler.constructSystemMessage("\\skills"),
                chatHandler.constructSystemMessage("\\noprotect"),
                chatHandler.constructSystemMessage("\\total_filter"),
                // TODO: add tips for the next handlers (cost, format, etc.)
                chatHandler.constructSystemMessage("\\gra"),
                chatHandler.constructSystemMessage("\\invisible"),
                chatHandler.constructSystemMessage("\\doppel"),
                chatHandler.constructSystemMessage("\\hide_eq"),
                chatHandler.constructSystemMessage("\\compose"),
                chatHandler.constructSystemMessage("\\wimpy"),
                chatHandler.constructSystemMessage("\\sendpt"),
                chatHandler.constructSystemMessage("\\sendgold"),
                chatHandler.constructSystemMessage("\\roll"),
                chatHandler.constructSystemMessage("\\return"),
                chatHandler.constructSystemMessage("\\?")
        );
    }

    @Override
    public void handle(Player player, String[] args) {
        Session session = player.getSession();
        if (session != null) {
            session.send(new ChatMessage(1128));
            session.send(this.helpCommandMessages);
        }
    }
}
