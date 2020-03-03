package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class _csMessage implements ServerMessage {

    private final int unk1;
    private final int unk2;

    public _csMessage(int unk1, int unk2) {
        this.unk1 = unk1;
        this.unk2 = unk2;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity._cs;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(unk1);
        stream.writeInt(unk2);
    }
}
