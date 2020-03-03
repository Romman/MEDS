package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class HealthMessage implements ServerMessage {

    private final int health;
    private final int mana;
    /**
     * Unknown.
     * Always is 0.
     */
    private final int unk3 = 0;

    public HealthMessage(int health, int mana) {
        this.health = health;
        this.mana = mana;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.Health;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.health);
        stream.writeInt(this.mana);
        stream.writeInt(this.unk3);
    }
}
