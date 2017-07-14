package org.meds.net.handlers;

import org.meds.net.ClientCommandData;
import org.meds.net.ClientCommandTypes;
import org.meds.net.SessionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
@ClientCommand(ClientCommandTypes.GroupQuit)
public class GroupQuitCommandHandler extends CommonClientCommandHandler {

    @Autowired
    private SessionContext sessionContext;

    @Override
    public void handle(ClientCommandData data) {
        sessionContext.getPlayer().leaveGroup();
    }
}
