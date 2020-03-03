package org.meds.net.message.server;

import java.util.Collection;
import org.meds.item.ItemClasses;
import org.meds.item.ItemModification;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class ItemMessage implements ServerMessage {

    private final int id;
    private final ItemModification modification;
    private final String name;
    private final int imageId;
    private final ItemClasses itemClass;
    private final int level;
    private final int cost;
    private final int currencyId;
    /**
     * A date??
     * When either the image or even Item itself created
     */
    private final int unk9 = 1187244746;
    private final int durability;
    private final int weight;
    private final Collection<Bonus> bonuses;

    public ItemMessage(int id, ItemModification modification, String name, int imageId, ItemClasses itemClass,
                       int level, int cost, int currencyId, int durability, int weight, Collection<Bonus> bonuses) {
        this.id = id;
        this.modification = modification;
        this.name = name;
        this.imageId = imageId;
        this.itemClass = itemClass;
        this.level = level;
        this.cost = cost;
        this.currencyId = currencyId;
        this.durability = durability;
        this.weight = weight;
        this.bonuses = bonuses;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.ItemInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.id);
        stream.writeInt(this.modification.getValue());
        stream.writeString(this.name);
        stream.writeInt(this.imageId);
        stream.writeInt(this.itemClass.getValue());
        stream.writeInt(this.level);
        stream.writeInt(this.cost);
        stream.writeInt(this.currencyId);
        stream.writeInt(this.unk9);
        stream.writeInt(this.durability);
        stream.writeInt(this.weight);

        for (Bonus bonus : this.bonuses) {
            stream.writeInt(bonus.parameter);
            stream.writeInt(bonus.value);
        }
    }

    public static class Bonus {

        private final int parameter;
        private final int value;

        public Bonus(int parameter, int value) {
            this.parameter = parameter;
            this.value = value;
        }
    }
}
