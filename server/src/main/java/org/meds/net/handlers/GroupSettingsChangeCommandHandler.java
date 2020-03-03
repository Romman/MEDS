package org.meds.net.handlers;

import org.meds.Group;
import org.meds.net.ClientCommandData;
import org.meds.net.ClientCommandTypes;
import org.meds.net.SessionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
@ClientCommand(ClientCommandTypes.GroupSettingsChange)
public class GroupSettingsChangeCommandHandler extends CommonClientCommandHandler {

    @Autowired
    private SessionContext sessionContext;

    @Override
    public int getMinDataLength() {
        return 9;
    }

    @Override
    public void handle(ClientCommandData data) {
        Group group = sessionContext.getPlayer().getGroup();

        // The player is not in a group
        // or is not a leader
        if (group == null || group.getLeader() != sessionContext.getPlayer()) {
            return;
        }

        group.setMinLevel(data.getInt(0));
        group.setMaxLevel(data.getInt(1));
        group.setNoReligionAllowed(data.getInt(2, 1) != 0);
        group.setSunAllowed(data.getInt(3, 1) != 0);
        group.setMoonAllowed(data.getInt(4, 1) != 0);
        group.setOrderAllowed(data.getInt(5, 1) != 0);
        group.setChaosAllowed(data.getInt(6, 1) != 0);
        Group.ClanAccessModes mode = Group.ClanAccessModes.parse(data.getInt(7, 0));
        if (mode == null) {
            mode = Group.ClanAccessModes.All;
        }
        group.setClanAccessMode(mode);
        group.setOpen(data.getInt(8, 1) != 0);

        sessionContext.getSession().send(group.getSettingsData());
        sessionContext.getSession().send(group.getTeamLootData());
    }
}
