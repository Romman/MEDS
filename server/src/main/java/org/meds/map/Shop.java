package org.meds.map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.Player;
import org.meds.data.domain.ItemTemplate;
import org.meds.data.domain.ShopItem;
import org.meds.database.Repository;
import org.meds.item.*;
import org.meds.net.ServerCommands;
import org.meds.net.ServerPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@Scope("prototype")
public class Shop {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    private Repository<ItemTemplate> itemTemplateRepository;
    @Autowired
    private ItemFactory itemFactory;

    private org.meds.data.domain.Shop entry;
    private Map<ItemTemplate, Integer> items = new java.util.HashMap<>();

    public Shop(org.meds.data.domain.Shop entry) {
        this.entry = entry;
    }

    public void load() {
        for (ShopItem item : this.entry.getItems()) {
            ItemTemplate template = itemTemplateRepository.get(item.getItemTemplateId());
            if (template == null) {
                logger.warn("Shop {} has an item with a templateId={} which does not exist. Skipped.",
                        this.entry.getId(), item.getItemTemplateId());
                continue;
            }
            this.items.put(template, item.getCount());
        }
        logger.info("Shop {}: loaded with {} items.", this.entry.getId(), this.items.size());
    }

    public Set<ShopItem> getItems() {
        return null;
    }

    public ServerPacket getData() {
        ServerPacket packet = new ServerPacket(ServerCommands.ShopInfo);
        packet.add(this.items.size());
        packet.add(this.entry.getType());
        packet.add(0);
        for (Map.Entry<ItemTemplate, Integer> entry : this.items.entrySet()) {
            packet.add(entry.getKey().getId())
                .add("0") // Modification (Standard shops have only default items
                .add(ItemUtils.getMaxDurability(entry.getKey()));
            // -1 in DB mean an infinite count
            // But for the client this number is 999,999
            if (entry.getValue() == -1) {
                packet.add("999999");
            } else {
                packet.add(entry.getValue());
            }
            packet.add(entry.getKey().getCost());
            packet.add(0);
        }

        return packet;
    }

    /**
     * The shop buys items from a player.
     * @param seller The Player seller instance
     * @param prototype Item prototype for sale
     * @param count Count of items to buy
     * @return A boolean value, indicating whether the transaction was completed
     */
    public boolean buyItem(Player seller, ItemPrototype prototype, int count) {
        if (!this.isAppropriateItem(prototype)) {
            return false;
        }

        Item item = seller.getInventory().takeItem(prototype, count);
        if (item == null) {
            return false;
        }

        // Add money
        // Cost is 20% less than the item cost
        seller.changeCurrency(this.entry.getCurrencyId(), (int)(item.getTemplate().getCost() * 0.8) * item.getCount());

        return true;
    }

    /**
     * The shop sells items to a player
     * @param buyer A Player buyer instance.
     * @param prototype ItemPrototype to sell
     * @param count How much items to sell
     * @return A boolean value indicating whether the transaction was completed.
     */
    public boolean sellItem(Player buyer, ItemPrototype prototype, int count) {
        ItemTemplate template = itemTemplateRepository.get(prototype.getTemplateId());
        if (template == null)
            return false;

        Integer shopItemCount = this.items.get(template);
        // Shop does not have the item
        if (shopItemCount == null) {
            return false;
        }

        // Correct count by source count
        if (count > shopItemCount && shopItemCount != -1) {
            count = shopItemCount;
        }

        // Correct count by payable ability
        if (buyer.getCurrencyAmount(this.entry.getCurrencyId()) < template.getCost() * count) {
            count = buyer.getCurrencyAmount(this.entry.getCurrencyId()) / template.getCost();
        }

        Item item = itemFactory.create(template, count);
        if (!buyer.getInventory().tryStoreItem(item)) {
            return false;
        }

        // Real stored count
        count = count - item.getCount();
        buyer.changeCurrency(this.entry.getCurrencyId(), - template.getCost() * count);

        // Subtract the bought count if not an infinite item stack
        if (shopItemCount != -1) {
            shopItemCount -= count;
            // The last items
            if (shopItemCount == 0) {
                // Remove from the shop
                this.items.remove(template);
            } else {
                this.items.put(template, shopItemCount);
            }
        }

        return true;
    }

    public boolean isAppropriateItem(ItemPrototype prototype) {
        return this.isAppropriateItem(itemTemplateRepository.get(prototype.getTemplateId()));
    }

    public boolean isAppropriateItem(ItemTemplate template) {
        if (template == null) {
            return false;
        }

        ItemClasses itemClass = template.getItemClass();

        switch (this.entry.getType()) {
            case AlchemicalShop:
                return itemClass == ItemClasses.Usable || itemClass == ItemClasses.Gemm;
            case ArmourShop:
                return itemClass == ItemClasses.Hands || itemClass == ItemClasses.Back ||
                    itemClass == ItemClasses.Waist || itemClass == ItemClasses.Legs ||
                    itemClass == ItemClasses.Head || itemClass == ItemClasses.Body ||
                    itemClass == ItemClasses.Shield || itemClass == ItemClasses.Foot ||
                    itemClass == ItemClasses.Ring || itemClass == ItemClasses.Neck ||
                    itemClass == ItemClasses.Weapon;
            case Dump:
                return itemClass == ItemClasses.Component;
            default:
                return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Shop shop = (Shop) o;

        return this.entry.getId() == shop.entry.getId();
    }

    @Override
    public int hashCode() {
        return this.entry.hashCode();
    }
}
