package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class MagicInfoMessage implements ServerMessage {

    private final Collection<SpellInfo> spellInfos;

    public MagicInfoMessage(Collection<SpellInfo> spellInfos) {
        this.spellInfos = spellInfos;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.MagicInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.spellInfos.size());
        for (SpellInfo info : spellInfos) {
            stream.writeInt(info.spellId);
            stream.writeInt(info.spellType);
            stream.writeString(info.spellName);
            stream.writeInt(info.level);
        }
    }

    public static class SpellInfo {

        private final int spellId;
        private final int spellType;
        private final String spellName;
        private final int level;

        public SpellInfo(int spellId, int spellType, String spellName, int level) {
            this.spellId = spellId;
            this.spellType = spellType;
            this.spellName = spellName;
            this.level = level;
        }
    }
}
