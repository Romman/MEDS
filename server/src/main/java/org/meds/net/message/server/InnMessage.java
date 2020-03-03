package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class InnMessage implements ServerMessage {

    private final int usedSLots;
    private final int maxSlots;
    private final int itemCount;
    private final int maxItemCount;
    private final Collection<ItemInfo> items;

    public InnMessage(Collection<ItemInfo> items, int maxSlots, int itemCount, int maxItemCount) {
        this.usedSLots = items.size();
        this.maxSlots = maxSlots;
        this.itemCount = itemCount;
        this.maxItemCount = maxItemCount;
        this.items = items;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.Inn;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(usedSLots);
        stream.writeInt(maxSlots);
        stream.writeInt(itemCount);
        stream.writeInt(maxItemCount);
        for (ItemInfo item : this.items) {
            item.serialize(stream);
        }

    }
}
