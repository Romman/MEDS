package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class GuildInfoMessage implements ServerMessage {

    private final Collection<Guild> guilds;

    public GuildInfoMessage(Collection<Guild> guilds) {
        this.guilds = guilds;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.GuildInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.guilds.size());

        for (Guild guild : this.guilds) {
            stream.writeInt(guild.id);
            stream.writeString(guild.name);
            stream.writeInt(guild.prevId);
            stream.writeInt(guild.level);
        }
    }

    public static class Guild {

        private final int id;
        private final String name;
        private final int prevId;
        private final int level;

        public Guild(int id, String name, int prevId, int level) {
            this.id = id;
            this.name = name;
            this.prevId = prevId;
            this.level = level;
        }
    }
}
