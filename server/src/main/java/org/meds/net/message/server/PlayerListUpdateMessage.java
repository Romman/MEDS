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
    private final boolean isGroupLeader;
    private final int clanId;
    private final int clanMemberStatus;
    private final int religionStatus;

    public PlayerListUpdateMessage(int playerId, int level, Religions religion, int religionLevel,
                                   boolean isGroupLeader, int clanId, int clanMemberStatus, int religionStatus) {
        this.playerId = playerId;
        this.level = level;
        this.religion = religion;
        this.religionLevel = religionLevel;
        this.isGroupLeader = isGroupLeader;
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
        stream.writeBoolean(this.isGroupLeader);
        stream.writeInt(this.clanId);
        stream.writeInt(this.clanMemberStatus);
        stream.writeInt(this.religionStatus);
    }
}
