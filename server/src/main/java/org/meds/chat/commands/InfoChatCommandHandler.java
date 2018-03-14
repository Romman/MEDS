package org.meds.chat.commands;

import org.meds.Player;
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

        if (player.getSession() == null)
            return;

        player.getSession().sendServerMessage(1173, DateFormatter.format(new Date()))
                .sendServerMessage(1174, server.getFormattedStartTime())
                .sendServerMessage(1175, player.getSession().getLastLoginDate())
                .sendServerMessage(1176, player.getSession().getLastLoginIp())
                .sendServerMessage(1177, player.getSession().getCurrentIp());
    }
}
