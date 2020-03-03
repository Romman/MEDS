package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class _lh0Message implements ServerMessage {

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity._lh0;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeString("");
    }
}
