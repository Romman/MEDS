package org.meds.net.handlers;

import org.meds.World;
import org.meds.net.*;
import org.meds.net.message.ServerMessageIdentity;
import org.meds.net.message.server.GroupCreatedMessage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
@ClientCommand(ClientCommandTypes.GroupJoin)
public class GroupJoinCommandHandler extends CommonClientCommandHandler {

    @Autowired
    private SessionContext sessionContext;
    @Autowired
    private World world;

    @Override
    public int getMinDataLength() {
        return 1;
    }

    @Override
    public void handle(ClientCommandData data) {
        sessionContext.getPlayer().joinGroup(world.getPlayer(data.getInt(0)));

        // No matter a group has been create or has not
        // send a group relation anyway
        int leaderId;
        if (sessionContext.getPlayer().getGroup() == null) {
            leaderId = 0;
        } else {
            leaderId = sessionContext.getPlayer().getGroup().getLeader().getId();
        }

        sessionContext.getSession().send(new GroupCreatedMessage(false, leaderId));
    }
}
