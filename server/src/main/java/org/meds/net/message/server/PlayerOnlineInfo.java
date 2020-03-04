package org.meds.net.message.server;

import org.meds.enums.Religions;
import org.meds.net.message.MessageWriteStream;

public class PlayerOnlineInfo {

    private final int playerId;
    private final String name;
    private final int level;
    private final Religions religion;
    private final int religionLevel;

    // one of it
    private final int unk6 = 4;

    private final int statuses;
    private final int clanId;
    private final int clanMemberStatus;
    private final int religionStatus;

    /**
     * 0 - EN
     * 2 - (All?)
     * 1 - RU
     * 3 - All
     */
    private final int unk11 = 1;
    // Title?
    // Popup text
    private final String unk12 = "";
    // It has value "0d1d4e" when the previous field is set
    private final int unk13 = 0;

    public PlayerOnlineInfo(int playerId, String name, int level, Religions religion,
                            int religionLevel, int statuses, int clanId, int clanMemberStatus,
                            int religionStatus) {
        this.playerId = playerId;
        this.name = name;
        this.level = level;
        this.religion = religion;
        this.religionLevel = religionLevel;
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
        stream.writeInt(this.unk6);
        stream.writeInt(this.statuses);
        stream.writeInt(this.clanId);
        stream.writeInt(this.clanMemberStatus);
        stream.writeInt(this.religionStatus);
        stream.writeInt(this.unk11);
        stream.writeString(this.unk12);
        stream.writeInt(this.unk13);
    }
}
