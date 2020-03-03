package org.meds.net.message.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class NpcQuestsMessage implements ServerMessage {

    private final Collection<QuestInfo> quests;

    private NpcQuestsMessage(Collection<QuestInfo> quests) {
        this.quests = quests;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.NpcQuestList;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        for (QuestInfo quest : this.quests) {
            quest.serialize(stream);
        }
    }

    private static class QuestInfo {

        final int questId;
        final String title;

        QuestInfo(int questId, String title) {
            this.questId = questId;
            this.title = title;
        }

        void serialize(MessageWriteStream stream) {
            stream.writeInt(this.questId);
            stream.writeString(this.title);
        }
    }

    public static class Builder {

        private final List<QuestInfo> quests;

        public Builder() {
            this.quests = new ArrayList<>();
        }

        public Builder addQuest(int id, String title) {
            this.quests.add(new QuestInfo(id, title));
            return this;
        }

        public int size() {
            return this.quests.size();
        }

        public NpcQuestsMessage build() {
            return new NpcQuestsMessage(this.quests);
        }
    }
}
