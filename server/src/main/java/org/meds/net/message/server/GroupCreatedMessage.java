package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class GroupCreatedMessage implements ServerMessage {

    private final boolean isLeader;
    private final int leaderId;

    public GroupCreatedMessage() {
        this.isLeader = false;
        this.leaderId = 0;
    }

    public GroupCreatedMessage(boolean isLeader, int leaderId) {
        this.isLeader = isLeader;
        this.leaderId = leaderId;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeBoolean(this.isLeader);
        stream.writeInt(this.leaderId);
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.GroupCreated;
    }
}
