package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class VersionMessage implements ServerMessage {

    private final int version;
    private final int sessionKey;

    public VersionMessage(int version, int sessionKey) {
        this.version = version;
        this.sessionKey = sessionKey;
    }

    /**
     * Version compatibility failed
     */
    public VersionMessage() {
        this.version = 0;
        this.sessionKey = 0;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.Version;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        if (this.version == 0) {
            stream.writeInt(0);
            return;
        }

        stream.writeInt(this.version);
        stream.writeInt(this.sessionKey);
    }
}
