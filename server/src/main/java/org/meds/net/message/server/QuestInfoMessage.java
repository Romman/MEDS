package org.meds.net.message.server;

import java.util.List;
import org.meds.enums.QuestTypes;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class QuestInfoMessage implements ServerMessage {

    private boolean forAccept = false;

    private final int templateId;
    private final String title;
    private final QuestTypes type;
    private final String description;
    /**
     * Always empty
     */
    private final String unk5 = "";
    private final int requiredCount;
    private final String requiredName;
    private final String unk8 = "";

    /**
     * 3 reward slots
     */
    private final List<String> rewards;
    /**
     * ???
     * Tutorial?
     */
    private final int unk13 = 0;

    public QuestInfoMessage(boolean forAccept, int templateId, String title, QuestTypes type, String description,
                            int requiredCount, String requiredName, List<String> rewards) {
        this(templateId, title, type, description, requiredCount, requiredName, rewards);
        this.forAccept = forAccept;
    }

    public QuestInfoMessage(int templateId, String title, QuestTypes type, String description,
                            int requiredCount, String requiredName, List<String> rewards) {
        this.templateId = templateId;
        this.title = title;
        this.type = type;
        this.description = description;
        this.requiredCount = requiredCount;
        this.requiredName = requiredName;
        this.rewards = rewards;
    }

    @Override
    public MessageIdentity getIdentity() {
        return this.forAccept ? ServerMessageIdentity.QuestInfoForAccept : ServerMessageIdentity.QuestInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.templateId);
        stream.writeString(this.title);
        stream.writeInt(this.type.getValue());
        stream.writeString(this.description);
        stream.writeString(this.unk5);
        if (this.requiredCount == 0 && this.requiredName == null) {
            stream.writeString("");
            stream.writeString("");
        } else {
            stream.writeInt(this.requiredCount);
            stream.writeString(this.requiredName);
        }
        stream.writeString(this.unk8);
        for (String reward : this.rewards) {
            stream.writeString(reward);
        }
        // Fill with empty values to get 3 reward slots
        for (int i = this.rewards.size(); i < 3; ++i) {
            stream.writeString("");
        }
        stream.writeInt(unk13);
    }
}
