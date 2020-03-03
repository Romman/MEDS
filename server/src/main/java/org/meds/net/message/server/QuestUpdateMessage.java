package org.meds.net.message.server;

import org.meds.enums.QuestTypes;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class QuestUpdateMessage implements ServerMessage {

    private final int questTemplateId;
    private final int progress;
    private final int time;
    private final int status;
    private final boolean tracked;
    private final int tracking;

    public QuestUpdateMessage(int questTemplateId, int progress, int time, int status, boolean tracked, int tracking) {
        this.questTemplateId = questTemplateId;
        this.progress = progress;
        this.time = time;
        this.status = status;
        this.tracked = tracked;
        this.tracking = tracking;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.UpdateQuest;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.questTemplateId);
        stream.writeInt(this.progress);
        if (this.time == 0) {
            stream.writeString("");
        } else {
            stream.writeInt(this.time);
        }
        stream.writeInt(this.status);
        stream.writeBoolean(this.tracked);
        stream.writeInt(this.tracking);
    }
}
