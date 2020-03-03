package org.meds.item;

import java.util.ArrayList;
import java.util.List;
import org.meds.net.message.server.ItemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Romman
 */
@Component
public class ItemMessagePacketFactory {

    @Autowired
    private ItemFactory itemFactory;
    @Autowired
    private ItemTitleConstructor itemTitleConstructor;

    /**
     * Gets the ServerPacket object that contains item info based on the specified template and modification data
     *
     * @param templateId
     * @param modification
     * @return
     */
    public ItemMessage create(int templateId, int modification) {
        ItemPrototype prototype = new ItemPrototype(templateId, modification, 0);
        Item item = itemFactory.create(prototype);
        if (item == null) {
            // TODO: Throw IllegalArgumentException.
            //  The only reason why item is null is because the templateId arguments is wrong.
            //  Need to log it
            return null;
        }

        String name = itemTitleConstructor.getTitleAndDescription(item);
        int maxDurability = ItemUtils.getMaxDurability(item.getTemplate());
        int weight = ItemUtils.getWeight(item.getTemplate());

        List<ItemMessage.Bonus> bonuses = new ArrayList<>(item.getBonusParameters().size());
        for (Map.Entry<ItemBonusParameters, Integer> entry : item.getBonusParameters().entrySet()) {
            bonuses.add(new ItemMessage.Bonus(entry.getKey().getValue(), entry.getValue()));
        }

        return new ItemMessage(
                item.getTemplate().getId(),
                item.getModification(),
                name,
                item.getTemplate().getImageId(),
                item.getTemplate().getItemClass(),
                item.getTemplate().getLevel(),
                item.getTemplate().getCost(),
                item.getTemplate().getCurrencyId(),
                maxDurability,
                weight,
                bonuses
        );
    }
}
