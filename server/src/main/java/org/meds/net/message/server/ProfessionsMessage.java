package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class ProfessionsMessage implements ServerMessage {

    private final Collection<ProfessionInfo> infos;

    public ProfessionsMessage(Collection<ProfessionInfo> infos) {
        this.infos = infos;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.Professions;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.infos.size());

        for (ProfessionInfo info : this.infos) {
            stream.writeString(info.title);
            stream.writeInt(info.level);
            stream.writeInt(info.exp);
        }
    }

    public static class ProfessionInfo {

        private final String title;
        private final int level;
        private final int exp;

        public ProfessionInfo(String title, int level, int exp) {
            this.title = title;
            this.level = level;
            this.exp = exp;
        }
    }
}
