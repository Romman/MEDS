package org.meds.net.message.server;

import org.meds.enums.QuestTypes;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class QuestListInfoMessage implements ServerMessage {

    private final int questTemplateId;
    private final QuestTypes type;
    private final String title;
    private final int progress;
    private final int required;
    private final int time;
    private final int status;
    private final boolean tracked;
    private final int tracking;

    public QuestListInfoMessage(int questTemplateId, QuestTypes type, String title, int progress, int required,
                                int time, int status, boolean tracked, int tracking) {
        this.questTemplateId = questTemplateId;
        this.type = type;
        this.title = title;
        this.progress = progress;
        this.required = required;
        this.time = time;
        this.status = status;
        this.tracked = tracked;
        this.tracking = tracking;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.QuestListInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.questTemplateId);
        stream.writeInt(this.type.getValue());
        stream.writeString(this.title);
        stream.writeInt(this.progress);
        stream.writeInt(this.required);
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
