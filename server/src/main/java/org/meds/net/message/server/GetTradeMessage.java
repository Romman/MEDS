package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class GetTradeMessage implements ServerMessage {

    private final int playerId;

    public GetTradeMessage(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.GetTrade;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.playerId);
    }
}
