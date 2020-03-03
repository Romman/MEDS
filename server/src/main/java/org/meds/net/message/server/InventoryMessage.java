package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class InventoryMessage implements ServerMessage {

    /**
     * Count of bought additional slots
     */
    private final int slotsPurchased = 0;
    /**
     * Cost of new slots
     */
    private final String newSlotCost = "5 platinum";
    /**
     *  Current available count of slots
     */
    private final int currentSlots = 25;

    private final Collection<ItemInfo> items;

    public InventoryMessage(Collection<ItemInfo> items) {
        this.items = items;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.InventoryInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.slotsPurchased);
        stream.writeString(this.newSlotCost);
        stream.writeInt(this.currentSlots);

        for (ItemInfo item : this.items) {
            item.serialize(stream);
        }
    }
}
