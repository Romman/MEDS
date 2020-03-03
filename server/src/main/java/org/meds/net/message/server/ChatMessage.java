package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class ChatMessage implements ServerMessage {

    private static final Object[] NO_PARAMS = new Object[0];

    private final int messageId;
    private final Object[] params;

    public ChatMessage(int messageId) {
        this.messageId = messageId;
        this.params = NO_PARAMS;
    }

    public ChatMessage(int messageId, Object... params) {
        this.messageId = messageId;
        this.params = params;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.ChatMessage;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.messageId);
        for (Object param : params) {
            stream.writeObject(param);
        }
    }
}
