package org.meds.net.message.server;

import org.meds.enums.Religions;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class PlayerListUpdateMessage implements ServerMessage {

    private final int playerId;
    private final int level;
    private final Religions religion;
    private final int religionLevel;
    /**
     * The same as in PlayerOnlineInfo
     */
    private final int unk5 = 4;
//    private final boolean isGroupLeader;
    private final int clanId;
    private final int clanMemberStatus;
    private final int religionStatus;
    private final int unk9 = 1;
    private final String unk10 = "";
    private final int unk11 = 0;

    public PlayerListUpdateMessage(int playerId, int level, Religions religion, int religionLevel,
                                   int clanId, int clanMemberStatus, int religionStatus) {
        this.playerId = playerId;
        this.level = level;
        this.religion = religion;
        this.religionLevel = religionLevel;
        this.clanId = clanId;
        this.clanMemberStatus = clanMemberStatus;
        this.religionStatus = religionStatus;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.PlayersListUpdate;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.playerId);
        stream.writeInt(this.level);
        stream.writeInt(this.religion.getValue());
        stream.writeInt(this.religionLevel);
        stream.writeInt(this.unk5);
        stream.writeInt(this.clanId);
        stream.writeInt(this.clanMemberStatus);
        stream.writeInt(this.religionStatus);
        stream.writeInt(this.unk9);
        stream.writeString(this.unk10);
        stream.writeInt(this.unk11);
    }
}
