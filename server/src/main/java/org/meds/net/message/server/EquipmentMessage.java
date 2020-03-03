package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class EquipmentMessage implements ServerMessage {

    private final Collection<ItemInfo> items;

    public EquipmentMessage(Collection<ItemInfo> items) {
        this.items = items;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.EquipmentInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        for (ItemInfo item : this.items) {
            item.serialize(stream);
        }
    }
}
