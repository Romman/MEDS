package org.meds.net.handlers;

import org.meds.enums.PlayerSettings;
import org.meds.net.ClientCommandData;
import org.meds.net.ClientCommandTypes;
import org.meds.net.SessionContext;
import org.meds.net.message.server.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
@ClientCommand(ClientCommandTypes.SetAsceticism)
public class SetAsceticismCommandHandler extends CommonClientCommandHandler {

    @Autowired
    private SessionContext sessionContext;

    @Override
    public int getMinDataLength() {
        return 1;
    }

    @Override
    public void handle(ClientCommandData data) {
        boolean set = data.getInt(0) == 1;
        if (set) {
            sessionContext.getPlayer().getSettings().set(PlayerSettings.Asceticism);
            sessionContext.getSession().send(new ChatMessage(430));
        } else {
            sessionContext.getPlayer().getSettings().unset(PlayerSettings.Asceticism);
            sessionContext.getSession().send(new ChatMessage(431));
        }

        sessionContext.getSession().send(sessionContext.getPlayer().getParametersData());
    }
}
