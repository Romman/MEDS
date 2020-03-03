package org.meds.net.message.server;

import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class ServerTimeMessage implements ServerMessage {

    private final int time;

    public ServerTimeMessage(int time) {
        this.time = time;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.ServerTime;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(time);
    }
}
