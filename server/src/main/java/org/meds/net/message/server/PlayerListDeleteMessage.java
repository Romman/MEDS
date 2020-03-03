package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class PlayerListDeleteMessage implements ServerMessage {

    private final int playerId;

    public PlayerListDeleteMessage(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.PlayersListDelete;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.playerId);
    }
}
