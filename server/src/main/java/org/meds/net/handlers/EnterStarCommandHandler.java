package org.meds.net.handlers;

import org.meds.net.*;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
@ClientCommand(ClientCommandTypes.EnterStar)
public class EnterStarCommandHandler extends CommonClientCommandHandler {

    @Autowired
    private SessionContext sessionContext;

    @Override
    public void handle(ClientCommandData data) {
        // TODO: Implement Star Info Message
        ServerMessage starInfoMessage = new ServerMessage() {
            @Override
            public MessageIdentity getIdentity() {
                return ServerMessageIdentity.StarInfo;
            }

            @Override
            public void serialize(MessageWriteStream stream) {
                stream.writeInt(sessionContext.getPlayer().getHome().getId());
                stream.writeString("0"); // Corpse2 Location ID
                stream.writeString("0"); // Corpse3 Location ID
                stream.writeString("0"); // Corpse1 Location ID
                stream.writeString("0"); // ???
                stream.writeString("0"); // ???
                stream.writeString("0"); // ???
            }
        };
        sessionContext.getSession().send(starInfoMessage);
    }
}
