package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class NoGoMessage implements ServerMessage {

    private final int locationId;

    public NoGoMessage(int locationId) {
        this.locationId = locationId;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.NoGo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.locationId);

    }
}

