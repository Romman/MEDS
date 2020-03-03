package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class QuestFinalTextMessage implements ServerMessage {

    private final String title;
    private final String text;

    public QuestFinalTextMessage(String title, String text) {
        this.title = title;
        this.text = text;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.QuestFinalText;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeString(this.title);
        stream.writeString(this.text);
    }
}
