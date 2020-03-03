package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class AutoSpellMessage implements ServerMessage {

    private final int spellId;

    public AutoSpellMessage(int spellId) {
        this.spellId = spellId;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.AutoSpell;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.spellId);
    }
}
