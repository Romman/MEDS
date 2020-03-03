package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class InventoryUpdateMessage implements ServerMessage {

    private final int slotNumber;
    private final ItemInfo item;

    public InventoryUpdateMessage(int slotNumber, ItemInfo item) {
        this.slotNumber = slotNumber;
        this.item = item;
    }

    public InventoryUpdateMessage(int slotNumber) {
        this.slotNumber = slotNumber;
        this.item = new ItemInfo();
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.InventoryUpdate;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(slotNumber);
        item.serialize(stream);
    }
}
