package org.meds.database.entity;

import java.util.HashMap;
import java.util.Map;

import org.meds.enums.ItemBonusParameters;
import org.meds.enums.ItemClasses;
import org.meds.enums.ItemFlags;
import org.meds.logging.Logging;
import org.meds.util.EnumFlags;
import org.meds.util.SafeConvert;

@SuppressWarnings("unused")
public class ItemTemplate
{
    private int id;
    private String title;
    private String description;
    private int imageId;
    private ItemClasses itemClass;
    private int level;
    private int cost;
    private int currencyId;
    private int subClass;
    private EnumFlags<ItemFlags> flags;
    private String itemBonuses;
    private Map<ItemBonusParameters, Integer> bonusParameters = new HashMap<>();

    public int getId()
    {
        return id;
    }
    private void setId(int id)
    {
        this.id = id;
    }
    public String getTitle()
    {
        return title;
    }
    private void setTitle(String title)
    {
        this.title = title;
    }
    public String getDescription()
    {
        return description;
    }
    private void setDescription(String description)
    {
        this.description = description;
    }
    public int getImageId()
    {
        return imageId;
    }
    private void setImageId(int imageId)
    {
        this.imageId = imageId;
    }
    public int getItemClassInteger()
    {
        return itemClass.getValue();
    }
    private void setItemClassInteger(int itemClass)
    {
        this.itemClass = ItemClasses.parse(itemClass);
    }
    public ItemClasses getItemClass()
    {
        return this.itemClass;
    }
    public int getLevel()
    {
        return level;
    }
    private void setLevel(int level)
    {
        this.level = level;
    }
    public int getCost()
    {
        return cost;
    }
    private void setCost(int cost)
    {
        this.cost = cost;
    }
    public int getCurrencyId()
    {
        return currencyId;
    }
    private void setCurrencyId(int currencyId)
    {
        this.currencyId = currencyId;
    }
    public int getSubClass()
    {
        return subClass;
    }
    private void setSubClass(int subClass)
    {
        this.subClass = subClass;
    }
    public int getFlags()
    {
        return flags.getValue();
    }
    private void setFlags(int flags)
    {
        this.flags = new EnumFlags<>(flags);
    }
    public boolean hasFlag(ItemFlags flag)
    {
        return this.flags.has(flag);
    }
    public String getItemBonuses()
    {
        return itemBonuses;
    }
    private void setItemBonuses(String itemBonuses)
    {
        this.itemBonuses = itemBonuses;
        this.bonusParameters = new HashMap<>();
        String[] keyValues = itemBonuses.split(";");
        for (int i = 0; i < keyValues.length; ++i)
        {
            // Ignore empty strings
            // It may happen when the itemBonus string ends with ";"
            if (keyValues[i].length() == 0)
                continue;

            String[] keyValue = keyValues[i].split(":");
            if (keyValue.length != 2)
            {
                Logging.Warn.log("Item template " + this.id + " has a wrong bonus key-value pair \"" + keyValues[i] + "\". Skipped.");
                continue;
            }
            ItemBonusParameters parameter = ItemBonusParameters.parse(SafeConvert.toInt32(keyValue[0]));
            int value = SafeConvert.toInt32(keyValue[1]);
            if (parameter == null)
            {
                Logging.Warn.log("Item template " + this.id + " has a wrong ItemBonusParameter type:  \"" + keyValue[0] + "\". Skipped.");
                continue;
            }
            if (value <= 0)
            {
                Logging.Warn.log("Item template " + this.id + " has a wrong ItemBonusParameter value:  \"" + keyValue[1] + "\". Skipped.");
                continue;
            }
            this.bonusParameters.put(parameter, value);
        }
        this.bonusParameters = java.util.Collections.unmodifiableMap(this.bonusParameters);
    }
    public Map<ItemBonusParameters, Integer> getBonusParameters()
    {
        return this.bonusParameters;
    }

    @Override
    public int hashCode()
    {
        return this.id;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (!(obj instanceof ItemTemplate))
            return false;
        ItemTemplate cObj = (ItemTemplate)obj;
        return this.id == cObj.id;
    }
}
