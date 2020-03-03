package org.meds.net.message.server;

import java.util.Collection;
import org.meds.enums.ShopTypes;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class ShopInfoMessage implements ServerMessage {

    private final ShopTypes type;
    private final Collection<ShopItemInfo> items;
    private final int unk3 = 0;

    public ShopInfoMessage(ShopTypes type, Collection<ShopItemInfo> items) {
        this.type = type;
        this.items = items;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.ShopInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.items.size());
        stream.writeInt(this.type.getValue());
        stream.writeInt(this.unk3);

        for (ShopItemInfo item : this.items) {
            item.serialize(stream);
        }
    }

    public static class ShopItemInfo {

        private final int templateId;
        private final int modification;
        private final int durability;
        private final int count;
        private final int cost;
        private final int unk6 = 0;

        public ShopItemInfo(int templateId, int modification, int durability, int count, int cost) {
            this.templateId = templateId;
            this.modification = modification;
            this.durability = durability;
            this.count = count;
            this.cost = cost;
        }

        private void serialize(MessageWriteStream stream) {
            stream.writeInt(this.templateId);
            stream.writeInt(this.modification);
            stream.writeInt(this.durability);
            stream.writeInt(this.count);
            stream.writeInt(this.cost);
            stream.writeInt(this.unk6);
        }
    }
}
