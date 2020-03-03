package org.meds.net.message.server;

import org.meds.Group;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class GroupLootMessage implements ServerMessage {

    private final Group.TeamLootModes mode;

    public GroupLootMessage(Group.TeamLootModes mode) {
        this.mode = mode;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.TeamLoot;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.mode.getValue());
    }
}
