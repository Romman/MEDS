package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class RelaxOnMessage implements ServerMessage {

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.RelaxOn;
    }

    @Override
    public void serialize(MessageWriteStream stream) {

    }
}
