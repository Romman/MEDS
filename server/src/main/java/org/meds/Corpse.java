package org.meds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.item.Item;
import org.meds.map.Location;

import java.util.HashSet;
import java.util.Set;

public class Corpse {

    private static Logger logger = LogManager.getLogger();

    private int id;
    private Set<Item> loot;
    private int gold;
    private Unit owner;
    private Location position;

    public Corpse(int corpseId, Unit owner) {
        logger.debug("Creating a corpse for {}", owner);
        this.owner = owner;
        this.id = corpseId;
        setPosition(owner.getPosition());
        this.loot = new HashSet<>();
    }

    public void fillWithLoot(Set<Item> items, int gold) {
        this.loot = items;
        this.gold = gold;
    }

    public Set<Item> getItems() {
        return this.loot;
    }

    public Location getPosition() {
        return this.position;
    }

    private void setPosition(Location location) {
        if (location == null && this.position != null) {
            this.position.removeCorpse(this);
        } else if (location != null) {
            location.addCorpse(this);
        }
        this.position = location;
    }

    public int getGold() {
        return this.gold;
    }

    public Unit getOwner() {
        return this.owner;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Corpse corpse = (Corpse) o;

        return this.id == corpse.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }
}
