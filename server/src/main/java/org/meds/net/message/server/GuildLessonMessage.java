package org.meds.net.message.server;

import java.util.List;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class GuildLessonMessage implements ServerMessage {

    private final int guildId;
    private final String guildName;
    private final List<String> lessonDescriptions;

    public GuildLessonMessage(int guildId, String guildName, List<String> lessonDescriptions) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.lessonDescriptions = lessonDescriptions;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.GuildLessonsInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.guildId);
        stream.writeString(this.guildName);
        lessonDescriptions.forEach(stream::writeString);
    }
}
