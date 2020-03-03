package org.meds.chat.commands;

import org.meds.Group;
import org.meds.Player;
import org.meds.net.Session;
import org.meds.net.message.server.ChatMessage;

public abstract class TeamLootChatCommandHandler extends AbstractChatCommandHandler {

    public abstract Group.TeamLootModes getMode();

    @Override
    public void handle(Player player, String[] args) {
        Group group = player.getGroup();
        if (group == null || group.getLeader() != player) {
            return;
        }

        group.setTeamLootMode(getMode());
        Session session = player.getSession();
        if (session != null) {
            session.send(new ChatMessage(group.getTeamLootMode().getModeMessage()));
            session.send(group.getTeamLootData());
        }
    }
}
