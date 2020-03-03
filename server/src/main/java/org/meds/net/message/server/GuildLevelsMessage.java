package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class GuildLevelsMessage implements ServerMessage {

    private final int guildLevel;
    private final Collection<GuildInfo> guildInfos;

    public GuildLevelsMessage(int guildLevel, Collection<GuildInfo> guildInfos) {
        this.guildLevel = guildLevel;
        this.guildInfos = guildInfos;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.GuildLevels;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.guildLevel);
        stream.writeInt(this.guildInfos.size());
        for (GuildInfo info : this.guildInfos) {
            stream.writeString(info.name);
            stream.writeInt(info.level);
        }
    }

    public static class GuildInfo {

        private final String name;
        private final int level;

        public GuildInfo(String name, int level) {
            this.name = name;
            this.level = level;
        }
    }
}
