package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class SocialChatMessage implements ServerMessage {

    private final String message;

    public SocialChatMessage(String message) {
        this.message = message;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.SocialChatMessage;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeString(message);
    }
}
