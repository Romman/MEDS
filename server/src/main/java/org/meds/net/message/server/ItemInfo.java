package org.meds.net.message.server;

import org.meds.item.ItemModification;
import org.meds.net.message.MessageWriteStream;

/**
 * Represents serializable data of an item
 */
public class ItemInfo {

    private final int templateId;
    private final int modification;
    private final int durability;
    private final int count;

    public ItemInfo() {
        this.templateId = 0;
        this.modification = 0;
        this.durability = 0;
        this.count = 0;
    }

    public ItemInfo(int templateId, int modification, int durability, int count) {
        this.templateId = templateId;
        this.modification = modification;
        this.durability = durability;
        this.count = count;
    }

    public ItemInfo(int templateId, ItemModification modification, int durability, int count) {
        this.templateId = templateId;
        this.modification = modification.getValue();
        this.durability = durability;
        this.count = count;
    }

    protected void serialize(MessageWriteStream stream) {
        stream.writeInt(this.templateId);
        stream.writeInt(this.modification);
        stream.writeInt(this.durability);
        stream.writeInt(this.count);
    }
}
