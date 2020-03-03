package org.meds.chat.commands;

import org.meds.Player;
import org.meds.net.Session;
import org.meds.net.message.server.ChatMessage;
import org.meds.server.Server;
import org.meds.util.DateFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@ChatCommand("info")
public class InfoChatCommandHandler extends AbstractChatCommandHandler {

    private Server server;

    @Autowired
    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public void handle(Player player, String[] args) {

        Session session = player.getSession();
        if (session == null) {
            return;
        }

        session.send(new ChatMessage(1173, DateFormatter.format(new Date())));
        session.send(new ChatMessage(1174, server.getFormattedStartTime()));
        session.send(new ChatMessage(1175, session.getLastLoginDate()));
        session.send(new ChatMessage(1176, session.getLastLoginIp()));
        session.send(new ChatMessage(1177, session.getCurrentIp()));
    }
}
