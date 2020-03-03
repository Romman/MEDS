package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class MessageListMessage implements ServerMessage {

    private final Collection<MessageInfo> infos;

    public MessageListMessage(Collection<MessageInfo> infos) {
        this.infos = infos;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.MessageList;
    }

    @Override
    public void serialize(MessageWriteStream stream) {

    }

    public static class MessageInfo {

        private final int id;
        private final int type;
        private final String message;

        public MessageInfo(int id, int type, String message) {
            this.id = id;
            this.type = type;
            this.message = message;
        }
    }
}
