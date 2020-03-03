package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class SkillInfoMessage implements ServerMessage {

    private final Collection<SkillInfo> skillInfos;

    public SkillInfoMessage(Collection<SkillInfo> skillInfos) {
        this.skillInfos = skillInfos;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.SkillInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.skillInfos.size());
        for (SkillInfo info : this.skillInfos) {
            stream.writeInt(info.skillId);
            stream.writeString(info.skillName);
            stream.writeInt(info.skillId);
        }
    }

    public static class SkillInfo {

        private final int skillId;
        private final String skillName;
        private final int level;

        public SkillInfo(int skillId, String skillName, int level) {
            this.skillId = skillId;
            this.skillName = skillName;
            this.level = level;
        }
    }
}
