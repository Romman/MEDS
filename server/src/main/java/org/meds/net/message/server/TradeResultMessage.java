package org.meds.net.message.server;

import org.meds.Trade;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class TradeResultMessage implements ServerMessage {

    private final Trade.Results result;
    private final int unk2 = 0;
    private final int unk3 = 0;

    public TradeResultMessage(Trade.Results result) {
        this.result = result;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.TradeResult;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.result.getValue());
        stream.writeInt(this.unk2);
        stream.writeInt(this.unk3);
    }
}
