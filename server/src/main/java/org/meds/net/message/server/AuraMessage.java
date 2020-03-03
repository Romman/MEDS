package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class AuraMessage implements ServerMessage {

    private final int spellId;
    private final int level;
    private final int remainingTime;

    public AuraMessage(int spellId, int level, int remainingTime) {
        this.spellId = spellId;
        this.level = level;
        this.remainingTime = remainingTime;
    }

    /**
     * Create the message for a permanent aura that has no remaining time.
     */
    public AuraMessage(int spellId, int level) {
        this.spellId = spellId;
        this.level = level;
        this.remainingTime = -1;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.Aura;
    }

    @Override
    public void serialize(MessageWriteStream stream) {

    }
}
