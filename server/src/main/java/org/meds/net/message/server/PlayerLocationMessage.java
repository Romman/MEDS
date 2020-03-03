package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class PlayerLocationMessage implements ServerMessage {

    private final int playerId;
    private final int locationId;

    public PlayerLocationMessage(int playerId, int locationId) {
        this.playerId = playerId;
        this.locationId = locationId;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.PlayerLocation;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.playerId);
        stream.writeInt(this.locationId);
    }
}
