package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class RelaxOffMessage implements ServerMessage {

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.RelaxOff;
    }

    @Override
    public void serialize(MessageWriteStream stream) {

    }
}
