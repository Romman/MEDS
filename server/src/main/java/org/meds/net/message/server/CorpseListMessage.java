package org.meds.net.message.server;

import java.util.Collection;
import org.meds.item.ItemModification;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class CorpseListMessage implements ServerMessage {

    private final Collection<CorpseLocationInfo> corpses;
    private final Collection<ItemLocationInfo> items;

    public CorpseListMessage(Collection<CorpseLocationInfo> corpses, Collection<ItemLocationInfo> items) {
        this.corpses = corpses;
        this.items = items;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.CorpseList;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.corpses.size() + this.items.size());
        for (CorpseLocationInfo corpse : this.corpses) {
            stream.writeInt(corpse.id);
            stream.writeString(corpse.isPlayer ? "user" : "npc");
            stream.writeString(corpse.ownerName);
        }
        for (ItemLocationInfo item : this.items) {
            stream.writeInt(item.templateId);
            stream.writeInt(item.modification);
            stream.writeInt(item.durability);
            stream.writeInt(item.count);
        }
    }

    public static class CorpseLocationInfo {

        private final int id;
        private final boolean isPlayer;
        private final String ownerName;

        public CorpseLocationInfo(int id, boolean isPlayer, String ownerName) {
            this.id = id;
            this.isPlayer = isPlayer;
            this.ownerName = ownerName;
        }
    }

    public static class ItemLocationInfo {

        private final int templateId;
        private final int modification;
        private final int durability;
        private final int count;

        public ItemLocationInfo(int templateId, ItemModification modification, int durability, int count) {
            this.templateId = templateId;
            this.modification = modification.getValue();
            this.durability = durability;
            this.count = count;
        }

    }
}
