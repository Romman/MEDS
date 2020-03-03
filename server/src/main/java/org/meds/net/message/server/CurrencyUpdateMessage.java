package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class CurrencyUpdateMessage implements ServerMessage {

    private final int id;
    private final int amount;

    public CurrencyUpdateMessage(int id, int amount) {
        this.id = id;
        this.amount = amount;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.Currency;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.id);
        stream.writeInt(this.amount);
    }
}
