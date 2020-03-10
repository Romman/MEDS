package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

/**
 * TODO: Message has no fix parameters but may have repeatable sequence of these two parameters
 */
public class AchievementUpdateMessage implements ServerMessage {

    private final int achievementId;
    private final int progress;


    public AchievementUpdateMessage(int achievementId, int progress) {
        this.achievementId = achievementId;
        this.progress = progress;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.AchievementUpdate;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.achievementId);
        stream.writeInt(this.progress);
    }
}
