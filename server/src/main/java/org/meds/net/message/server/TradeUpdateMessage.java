package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class TradeUpdateMessage implements ServerMessage {

    private final int playerId;
    private final Iterable<ItemInfo> items;
    private final int gold;
    private final int platinum;
    private final boolean agreed;
    /**
     * ???
     * Always 0
     */
    private final int unk6 = 0;

    public TradeUpdateMessage(int playerId, Iterable<ItemInfo> items, int gold, int platinum, boolean agreed) {
        this.playerId = playerId;
        this.items = items;
        this.gold = gold;
        this.platinum = platinum;
        this.agreed = agreed;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.TradeUpdate;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.playerId);
        for (ItemInfo item : this.items) {
            item.serialize(stream);
        }
        stream.writeInt(this.gold);
        stream.writeInt(this.playerId);
        stream.writeBoolean(this.agreed);
        stream.writeInt(this.unk6);

    }
}
