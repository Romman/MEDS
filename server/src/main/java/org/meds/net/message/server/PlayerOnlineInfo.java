package org.meds.net.message.server;

import org.meds.enums.Religions;
import org.meds.net.message.MessageWriteStream;

public class PlayerOnlineInfo {

    private final int playerId;
    private final String name;
    private final int level;
    private final Religions religion;
    private final int religionLevel;
    private final boolean isGroupLeader;
    private final int statuses;
    private final int clanId;
    private final int clanMemberStatus;
    private final int religionStatus;

    public PlayerOnlineInfo(int playerId, String name, int level, Religions religion,
                            int religionLevel, boolean isGroupLeader, int statuses, int clanId, int clanMemberStatus,
                            int religionStatus) {
        this.playerId = playerId;
        this.name = name;
        this.level = level;
        this.religion = religion;
        this.religionLevel = religionLevel;
        this.isGroupLeader = isGroupLeader;
        this.statuses = statuses;
        this.clanId = clanId;
        this.clanMemberStatus = clanMemberStatus;
        this.religionStatus = religionStatus;
    }

    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.playerId);
        stream.writeString(this.name);
        stream.writeInt(this.level);
        stream.writeInt(this.religion.getValue());
        stream.writeInt(this.religionLevel);
        stream.writeBoolean(this.isGroupLeader);
        stream.writeInt(this.statuses);
        stream.writeInt(this.clanId);
        stream.writeInt(this.clanMemberStatus);
        stream.writeInt(this.religionStatus);
    }
}
