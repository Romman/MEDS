package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

/**
 * TODO: Implement Region quests
 */
public class QuestListRegionMessage implements ServerMessage {

    private final int questGiverIcon = 0;
    private final int questCount = 0;

    public QuestListRegionMessage() {

    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.QuestListRegion;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.questGiverIcon);
        stream.writeInt(this.questCount);
    }
}
