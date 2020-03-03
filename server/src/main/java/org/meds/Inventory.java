package org.meds;

import org.meds.data.domain.CharacterInventoryItem;
import org.meds.item.*;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.ChatMessage;
import org.meds.net.message.server.EquipmentMessage;
import org.meds.net.message.server.InventoryMessage;
import org.meds.net.message.server.InventoryUpdateMessage;
import org.meds.net.message.server.ItemInfo;
import org.meds.util.Valued;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;

@Component
@Scope("prototype")
public class Inventory {

    public enum Slots implements Valued {

        /*
         * Slots error or slot can not be retrieved
         */
        None(-1),

        // Equipment slots
        Head(0),
        Neck(1),
        Back(2),
        Body(3),
        /**
         * Gloves
         */
        Hand(4),
        /**
         * Weapon
         */
        RightHand(5),
        /**
         * Shield, stick or even second weapon
         */
        LeftHand(6),
        Waist(7),
        Leg(8),
        Foot(9),
        RightHandRing(10),
        LeftHandRing(11),
        // Inventory slots
        Inventory1(12),
        Inventory2(13),
        Inventory3(14),
        Inventory4(15),
        Inventory5(16),
        Inventory6(17),
        Inventory7(18),
        Inventory8(19),
        Inventory9(20),
        Inventory10(21),
        Inventory11(22),
        Inventory12(23),
        Inventory13(24),
        Inventory14(25),
        Inventory15(26),
        Inventory16(27),
        Inventory17(28),
        Inventory18(29),
        Inventory19(30),
        Inventory20(31),
        Inventory21(32),
        Inventory22(33),
        Inventory23(34),
        Inventory24(35),
        Inventory25(36);

        private static final Slots[] values = new Slots[37];

        static {
            for (Slots slot : Slots.values()) {
                // HACK: exclude None(-1) value
                if (slot.value < 0) {
                    continue;
                }
                Slots.values[slot.value] = slot;
            }
        }

        public static Slots parse(int value)        {
            return Slots.values[value];
        }

        private final int value;

        Slots(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return this.value;
        }
    }

    public static final int InventorySlotCount = 25;
    public static final int EquipmentSlotCount = 12;

    public static boolean isEquipmentSlot(int slot) {
        return slot >= Slots.Head.getValue() && slot <= Slots.LeftHandRing.getValue();
    }

    public static Slots getEquipmentSlot(Item item) {
        return Inventory.getEquipmentSlot(item.getTemplate().getItemClass());
    }

    public static Slots getEquipmentSlot(ItemClasses itemClass) {
        switch (itemClass) {
            case Head:
                return Slots.Head;
            case Neck:
                return Slots.Neck;
            case Back:
                return Slots.Back;
            case Hands:
                return Slots.Hand;
            case Body:
                return Slots.Body;
            case Ring:
                // returns the first slot
                return Slots.RightHandRing;
            case Weapon:
                // Dual weapons should be checked manually
                return Slots.RightHand;
            case Shield:
                return Slots.LeftHand;
            case Waist:
                return Slots.Waist;
            case Legs:
                return Slots.Leg;
            case Foot:
                return Slots.Foot;
            default:
                return Slots.None;
        }
    }

    @Autowired
    private ItemFactory itemFactory;
    @Autowired
    private ItemTitleConstructor itemTitleConstructor;
    @Autowired
    private ItemUsageService itemUsageService;

    private int capacity;

    public int weight;

    private Item[] inventorySlots;

    private Player owner;
    private Map<Integer, CharacterInventoryItem> characterItems = new HashMap<>();


    public Inventory() {
        this.inventorySlots = new Item[EquipmentSlotCount + InventorySlotCount];
    }

    public void setOwner(Player player) {
        this.owner = player;
    }

    public Item get(int slot) {
        if (slot > this.inventorySlots.length)
            return null;
        return this.inventorySlots[slot];
    }

    public Item get(Slots slot) {
        return this.inventorySlots[slot.getValue()];
    }

    public int getCapacity() {
        return this.capacity;
    }

    public void load(Map<Integer, CharacterInventoryItem> items) {
        this.characterItems = items;
        if (this.characterItems.size() == 0) {
            return;
        }

        for (CharacterInventoryItem charItem : items.values()) {
            ItemPrototype prototype = new ItemPrototype(charItem.getItemTemplateId(), charItem.getModification(),
                    charItem.getDurability());
            Item item = itemFactory.create(prototype, charItem.getCount());
            // Item has a valid template
            if (item != null) {
                this.inventorySlots[charItem.getSlot()] = item;
            }
        }
        onEquipmentChanged();
    }

    /**
     * Saves all the inventory and equipment items into database.
     */
    public void save() {
        for (int i = 0; i < this.inventorySlots.length; ++i) {
            if (this.inventorySlots[i] == null) {
                    this.characterItems.remove(i);
            } else {
                Item item = this.inventorySlots[i];
                CharacterInventoryItem charItem = this.characterItems.get(i);
                if (item == null) {
                    if (charItem != null) {
                        this.characterItems.remove(i);
                    }
                    continue;
                }

                if (charItem == null) {
                    charItem = new CharacterInventoryItem(this.owner.getId(), i);
                    this.characterItems.put(i, charItem);
                }

                charItem.setItemTemplateId(item.getTemplate().getId());
                charItem.setModification(item.getModification().getValue());
                charItem.setDurability(item.getDurability());
                charItem.setCount(item.getCount());
            }
        }
    }

    public ServerMessage getInventoryData() {
        List<ItemInfo> items = new ArrayList<>(25);
        for (int i = Slots.Inventory1.getValue(); i < InventorySlotCount + Slots.Inventory1.getValue(); ++i) {
            Item item = this.inventorySlots[i];
            if (item == null) {
                items.add(new ItemInfo());
                continue;
            }
            items.add(new ItemInfo(
                    item.getTemplate().getId(),
                    item.getModification(),
                    item.getDurability(),
                    item.getCount()
            ));
        }
        return new InventoryMessage(items);
    }

    public ServerMessage getEquipmentData() {
        List<ItemInfo> items = new ArrayList<>(EquipmentSlotCount);
        for (int i = 0; i < EquipmentSlotCount; ++i) {
            Item item = this.inventorySlots[i];
            if (item == null) {
                items.add(new ItemInfo());
                continue;
            }
            items.add(new ItemInfo(
                    item.getTemplate().getId(),
                    item.getModification(),
                    item.getDurability(),
                    1 // Seems like equipment count is always 1.
            ));
        }
        return new EquipmentMessage(items);
    }

    public ServerMessage getUpdatedSlotData(int slot) {
        Item item = this.inventorySlots[slot];
        if (item == null) {
            return new InventoryUpdateMessage(slot);
        }

        return new InventoryUpdateMessage(slot, new ItemInfo(
                item.getTemplate().getId(),
                item.getModification(),
                item.getDurability(),
                item.getCount()
        ));
    }

    public void swapItem(int currentSlot, int newSlot, int count) {
        do {
            Item sourceItem = this.inventorySlots[currentSlot];
            Item targetItem =  this.inventorySlots[newSlot];
            // Source slot is empty
            if (sourceItem == null || sourceItem.getCount() < count) {
                break;
            }
            // TODO: better check for equipment
            // mb separate method EquipItem
            if (!canSlotStoreItem(newSlot, sourceItem)) {
                break;
            }
            // Equipments slot can take only 1 item
            if (isEquipmentSlot(newSlot)) {
                count = 1;
            }

            // Move item
            if (targetItem == null) {
                // move the whole stack
                if (sourceItem.getCount() == count) {
                    this.inventorySlots[currentSlot] = null;
                    this.inventorySlots[newSlot] = sourceItem;
                } else {
                    this.inventorySlots[newSlot] = sourceItem.unstackItem(count);
                }
            }
            // Stack items
            else if(ItemUtils.areStackable(targetItem, sourceItem) && !isEquipmentSlot(newSlot)) {
                // Swap the whole stack
                if (sourceItem.getCount() == count) {
                    this.inventorySlots[currentSlot] = null;
                    targetItem.stackItem(sourceItem);
                } else {
                    targetItem.stackItem(sourceItem.unstackItem(count));
                }
            }
            // Swap items
            else {
                // Not the whole stack -> cancel the swapping
                if (sourceItem.getCount() != count) {
                    break;
                }

                this.inventorySlots[currentSlot] = targetItem;
                this.inventorySlots[newSlot] = sourceItem;
            }

            // Update equipment
            if (isEquipmentSlot(currentSlot) || isEquipmentSlot(newSlot)) {
                onEquipmentChanged();
            }
        } while (false);

        // No matters the result of swapping - send current inventory data
        if (this.owner.getSession() != null) {
            this.owner.getSession().send(getUpdatedSlotData(currentSlot));
            this.owner.getSession().send(getUpdatedSlotData(newSlot));
        }
    }

    public boolean canStoreItems(Item... items) {
        // Calculate total weight
        int weight = 0;
        for (Item item : items) {
            if (item == null) {
                continue;
            }
            weight += ItemUtils.getWeight(item.getTemplate());
        }

        // TODO: Check weight after implementation

        // Count occupying slots
        int slots = 0;
        for (Item item : items) {
            if (item == null)
                continue;
            // Equipment cannot be stacked
            if (ItemUtils.isEquipment(item)) {
                slots += item.getCount();
                continue;
            }

            // Already contains the specified prototype
            if (findItem(item.getPrototype()) > 0) {
                // Just will be stacked
                continue;
            } else {
                slots++;
            }
        }

        // Calculate free slots count
        for (int i = Slots.Inventory1.getValue(); i <= Slots.Inventory25.getValue(); i++) {
            if (this.inventorySlots[i] == null)
                slots--;
        }

        return slots <= 0;
    }

    public boolean hasItem(int templateId) {
        return findItem(templateId) != -1;
    }

    public boolean hasItem(ItemPrototype prototype) {
        return findItem(prototype) != -1;
    }

    public boolean hasItem(Item item) {
        if (item == null)
            return false;

        Integer[] slots = findAllItems(item.getPrototype());
        if (slots.length == 0)
            return false;

        // Count having items count
        int count = 0;
        for (Integer slot : slots) {
            count += this.inventorySlots[slot].getCount();
        }

        return item.getCount() <= count;
    }

    /**
     * Returns slot number of the first item that has the specified template ID or returns -1 if no items are found.
     * @return The first slot index of the item if an item was found; otherwise, -1
     */
    public int findItem(int templateId) {
        for (int i = Slots.Inventory1.getValue(); i <= Slots.Inventory25.getValue(); ++i) {
            if (this.inventorySlots[i] == null) {
                continue;
            }
            if (this.inventorySlots[i].getTemplate().getId() != templateId) {
                continue;
            }
            return i;
        }
        return -1;
    }

    /**
     * Returns slot number of the first item that has the same specified basic parameters or -1 of no items are found.
     * @return The first slot index of the item if an item was found; otherwise, -1
     */
    public int findItem(ItemPrototype prototype) {
        if (prototype == null) {
            return -1;
        }
        for (int i = Slots.Inventory1.getValue(); i <= Slots.Inventory25.getValue(); ++i) {
            if (this.inventorySlots[i] != null && this.inventorySlots[i].equals(prototype))
                return i;
        }
        return -1;
    }

    public Integer[] findAllItems(int templateId) {
        List<Integer> slots = new ArrayList<>();
        for (int i = Slots.Inventory1.getValue(); i <= Slots.Inventory25.getValue(); ++i) {
            if (this.inventorySlots[i] == null) {
                continue;
            }
            if (this.inventorySlots[i].getTemplate().getId() != templateId) {
                continue;
            }
            slots.add(i);
        }

        return slots.toArray(new Integer[slots.size()]);
    }

    public Integer[] findAllItems(ItemPrototype prototype) {
        if (prototype == null)
            return new Integer[0];

        List<Integer> slots = new ArrayList<>(25);
        for (int i = Slots.Inventory1.getValue(); i <= Slots.Inventory25.getValue(); ++i) {
            if (this.inventorySlots[i] == null || !this.inventorySlots[i].equals(prototype))
                continue;
            slots.add(i);
        }

        return slots.toArray(new Integer[slots.size()]);
    }

    public boolean tryStoreItem(Item item) {
        return this.tryStoreItem(item, item.getCount());
    }

    public boolean tryStoreItem(Item item, int count) {
        // TODO: Check Weight

        // Find appropriate slot
        for (int i = Slots.Inventory1.getValue(); i <= Slots.Inventory25.getValue(); ++i) {
            if (this.inventorySlots[i] != null) {
                // Tries to stack (includes items comparability checking)
                if (ItemUtils.areStackable(this.inventorySlots[i], item)) {
                    this.inventorySlots[i].stackItem(item, count);
                    onInventoryChanged();
                    return true;
                }
            } else {
                this.inventorySlots[i] = item.unstackItem(count);
                onInventoryChanged();
                return true;
            }
        }

        return false;
    }

    private boolean canSlotStoreItem(int slot, Item item) {
        if (!isEquipmentSlot(slot)) {
            return true;
        }
        // Slot is equipment: check a compatibility between the slot type and the item type.
        // Also check the item requirements.

        // Check min level
        if (item.getTemplate().getLevel() > this.owner.getLevel()) {
            return false;
        }

        switch (Slots.parse(slot)) {
            case Head:
                return item.getTemplate().getItemClass() == ItemClasses.Head;
            case Neck:
                return item.getTemplate().getItemClass() == ItemClasses.Neck;
            case Back:
                return item.getTemplate().getItemClass() == ItemClasses.Back;
            case Body:
                return item.getTemplate().getItemClass() == ItemClasses.Body;
            case Hand:
                return item.getTemplate().getItemClass() == ItemClasses.Hands;
            case RightHand:
                return item.getTemplate().getItemClass() == ItemClasses.Weapon;
            case LeftHand:
                return item.getTemplate().getItemClass() == ItemClasses.Shield;
            case Waist:
                return item.getTemplate().getItemClass() == ItemClasses.Waist;
            case Leg:
                return item.getTemplate().getItemClass() == ItemClasses.Legs;
            case Foot:
                return item.getTemplate().getItemClass() == ItemClasses.Foot;
            case RightHandRing:
            case LeftHandRing:
                return item.getTemplate().getItemClass() == ItemClasses.Ring;
            default:
                // I don't know is this real but just in case
                return false;
        }
    }

    private void onEquipmentChanged() {
        // Clear and recalculate equipment bonus parameters
        this.owner.getParameters().equipment().clear();
        IntStream.range(0, EquipmentSlotCount)
                .mapToObj(slot -> this.inventorySlots[slot])
                .filter(Objects::nonNull)
                .flatMap(item -> item.getBonusParameters().entrySet().stream())
                .forEach(entry -> this.owner.getParameters().equipment().change(entry.getKey(), entry.getValue()));
    }

    private void onInventoryChanged() {
        this.owner.getSession().send(getInventoryData());
    }

    public Item takeItem(ItemPrototype prototype, int count) {
        if (prototype == null || prototype.getTemplateId() == 0) {
            return null;
        }

        // Try to find all the slots contain the specified prototype
        Integer[] itemSlots = this.findAllItems(prototype);

        if (itemSlots.length == 0) {
            return null;
        }

        int i = itemSlots.length - 1;
        Item item = null;
        do {
            if (item == null) {
                item = this.inventorySlots[itemSlots[i]].unstackItem(count);
            } else if (ItemUtils.areStackable(item, this.inventorySlots[itemSlots[i]])) {
                item.transfer(this.inventorySlots[itemSlots[i]], count - item.getCount());
            }

            if (this.inventorySlots[itemSlots[i]].getCount() == 0) {
                this.inventorySlots[itemSlots[i]] = null;
            }

            if (item.getCount() == count) {
                break;
            }

            --i;
        } while (i >= 0);

        if (item.getCount() != 0) {
            onInventoryChanged();
        }
        return item;
    }

    public Item takeItem(Item item) {
        if (item == null)
            return null;
        return takeItem(item.getPrototype(), item.getCount());
    }

    public Item takeItem(int slot, int count) {
        Item item = this.inventorySlots[slot];
        if (item == null) {
            return null;
        }

        if (item.getCount() == count) {
            this.inventorySlots[slot] = null;
        } else {
            item = item.unstackItem(count);
        }

        // Raise an event
        if (isEquipmentSlot(slot)) {
            onEquipmentChanged();
        } else {
            onInventoryChanged();
        }

        return item;
    }

    public boolean destroyItem(int slot, int count) {
        Item item = this.inventorySlots[slot];
        if (item == null) {
            return false;
        }
        if (count >= item.getCount()) {
            this.inventorySlots[slot] = null;
        }
        else
            item = item.unstackItem(count);

        // Send Message
        this.owner.getSession().send(new ChatMessage(
                1016, count > 1 ? count + " " : "", itemTitleConstructor.getTitle(item)
        ));

        // Personal items are destroyed completely
        // Others are thrown to the ground
        if (!item.hasFlag(ItemFlags.IsPersonal)) {
            this.owner.getPosition().addItem(item);
        }

        if (isEquipmentSlot(slot)) {
            onEquipmentChanged();
        } else {
            onInventoryChanged();
        }

        return true;
    }

    public void useItem(int slot) {
        Item item = this.inventorySlots[slot];
        if (item == null) {
            return;
        }

        // Using equipment items is like to equip it
        if (ItemUtils.isEquipment(item)) {
            swapItem(slot, getEquipmentSlot(item).getValue(), 1);
            return;
        }

        if (itemUsageService.processUsage(this.owner, item)) {
            item.use();
        }
        // Used the last item in the stack
        if (item.getCount() == 0) {
            this.inventorySlots[slot] = null;
        }

        // No matter whether the item was used or not
        // Send updated slot info
        if (this.owner.getSession() != null) {
            this.owner.getSession().send(getUpdatedSlotData(slot));
        }

    }
}
