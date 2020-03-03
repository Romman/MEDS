package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class DeleteAuraMessage implements ServerMessage {

    private final int spellId;

    public DeleteAuraMessage(int spellId) {
        this.spellId = spellId;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.DeleteAura;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.spellId);
    }
}
