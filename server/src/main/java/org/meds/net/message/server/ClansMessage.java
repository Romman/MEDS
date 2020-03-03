package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

/**
 * A list of all clans
 * TODO: Rewrite after Clan functionality implementation
 */
public class ClansMessage implements ServerMessage {

    private final int unk1;
    private final int unk2;
    private final String name;

    public ClansMessage(int unk1, int unk2, String name) {
        this.unk1 = unk1;
        this.unk2 = unk2;
        this.name = name;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.ClanInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(1); // Clans count
        stream.writeInt(this.unk1); // Clans count
        stream.writeInt(this.unk2); // Clans count
        stream.writeString(this.name); // Clans count
    }
}
