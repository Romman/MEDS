package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class GetCorpseMessage implements ServerMessage {

    /**
     * Do not know why but always true
     */
    private final String unk1 = "true";

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.GetCorpse;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeString(unk1);
    }
}
