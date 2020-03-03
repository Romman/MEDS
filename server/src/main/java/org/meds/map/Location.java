package org.meds.map;

import org.meds.*;
import org.meds.enums.CreatureBossTypes;
import org.meds.enums.MovementDirections;
import org.meds.enums.Parameters;
import org.meds.enums.SpecialLocationTypes;
import org.meds.item.Item;
import org.meds.item.ItemPrototype;
import org.meds.item.ItemUtils;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.CorpseListMessage;
import org.meds.net.message.server.LocationInfoMessage;
import org.meds.net.message.server.LocationMessage;
import org.meds.net.message.server.PositionUnitListMessage;
import org.meds.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component // Possibly it shouldn't be a bean
@Scope("prototype")
public class Location {

    @Autowired
    private MapManager mapManager;

    private org.meds.data.domain.Location entry;

    private Set<Unit> units;
    private Set<Unit> unitsView;
    private java.util.Map<Integer, Corpse> corpses;
    private java.util.Map<ItemPrototype, Item> items;
    private Region region;

    private ServerMessage locationMessage;
    private ServerMessage locationInfoMessage;
    private List<ServerMessage> neighborsInfoMessages;

    /**
     * Indicating whether the location will be updated at the next tick. Use a Setter for setting a value for this field!
     */
    private boolean updatable;

    /**
     * Reference to a unit which visual update cause location update request
     */
    private Unit updatedUnit;

    public Location(org.meds.data.domain.Location entry, Region region) {
        this.entry = entry;
        this.region = region;
        this.units = new HashSet<>();
        this.unitsView = Collections.unmodifiableSet(this.units);
        this.corpses = new HashMap<>();
        this.items = new HashMap<>();
    }

    public int getId() {
        return this.entry.getId();
    }

    public Region getRegion() {
        return region;
    }

    public boolean isSafeZone() {
        return this.entry.isSafeZone();
    }

    public SpecialLocationTypes getSpecialLocationType() {
        return this.entry.getSpecialLocationType();
    }

    public int getSpecialLocationId() {
        return this.entry.getSpecialLocationId();
    }

    public String getTitle() {
        return this.entry.getTitle();
    }

    private void setUpdatable(boolean updatable) {
        if (this.updatable == updatable) {
            return;
        }
        this.updatable = updatable;
        if (updatable) {
            mapManager.addLocationUpdate(this);
        }
    }

    public Location getRandomNeighbour() {
        return this.getRandomNeighbour(true, false);
    }

    public Location getRandomNeighbour(boolean includeSpecial) {
        return this.getRandomNeighbour(includeSpecial, false);
    }

    public Location getRandomNeighbour(boolean includeSpecial, boolean stayInRegion) {
        return getNeighbourLocation(getRandomDirection(includeSpecial, stayInRegion));
    }

    public MovementDirections getRandomDirection() {
        return this.getRandomDirection(true, false);
    }

    public MovementDirections getRandomDirection(boolean includeSpecial) {
        return this.getRandomDirection(includeSpecial, false);
    }

    public MovementDirections getRandomDirection(boolean includeSpecial, boolean stayInRegion) {
        int startIndex = Random.nextInt(MovementDirections.Up.getValue(), MovementDirections.West.getValue() + 1);
        // Increase or Decrease the index
        int bypassDirection = Random.nextInt(0, 2) == 0 ? -1 : 1;
        int index = startIndex;
        Location location;
        do {
            location = getNeighbourLocation(MovementDirections.parse(index));
            // Neighbour exists and it is not a special location.
            if (location != null && (includeSpecial || !location.entry.isSafeZone()) &&
                    (!stayInRegion || location.region == this.region)) {
                return MovementDirections.parse(index);
            }
            index += bypassDirection;
            // Keep boundaries
            if (index < 0) {
                index = 5;
            } else if (index > 5) {
                index = 0;
            }
        } while (index != startIndex);

        return MovementDirections.None;
    }

    public Location getNeighbourLocation(MovementDirections direction) {
        int neighbourId;
        switch (direction) {
            case Up:
                neighbourId = entry.getTopId();
                break;
            case Down:
                neighbourId = entry.getBottomId();
                break;
            case North:
                neighbourId = entry.getNorthId();
                break;
            case South:
                neighbourId = entry.getSouthId();
                break;
            case West:
                neighbourId = entry.getWestId();
                break;
            case East:
                neighbourId = entry.getEastId();
                break;
            default:
                return null;
        }

        return mapManager.getLocation(neighbourId);
    }


    /**
     * Gets the shorter location info with ServerCommands.LocationInfo
     */
    public ServerMessage getInfoData() {
        if (this.locationInfoMessage == null) {
            this.locationInfoMessage = new LocationInfoMessage(
                this.entry.getId(),
                this.entry.getTitle(),
                this.entry.getTopId(),
                this.entry.getBottomId(),
                this.entry.getNorthId(),
                this.entry.getSouthId(),
                this.entry.getWestId(),
                this.entry.getEastId(),
                this.entry.getxCoord(),
                this.entry.getyCoord(),
                this.entry.getzCoord(),
                this.entry.getSpecialLocationType(),
                this.entry.isSquare(),
                this.region.getName(),
                this.region.getKingdom().getEntry().getName(),
                // TODO: Implement Continents
                "Continent",
                this.entry.getRegionId()
            );
        }
        return this.locationInfoMessage;
    }

    public List<ServerMessage> getNeighborsInfoData() {
        if (this.neighborsInfoMessages == null) {
            this.neighborsInfoMessages = new ArrayList<>();
            Location neighbor;

            for (MovementDirections direction : MovementDirections.values()) {
                if ((neighbor = getNeighbourLocation(direction)) != null) {
                    this.neighborsInfoMessages.add(neighbor.getInfoData());
                }
            }
        }
        return this.neighborsInfoMessages;
    }

    public ServerMessage getData() {
        if (this.locationMessage == null) {
            this.locationMessage = new LocationMessage(
                this.entry.getId(),
                this.entry.getTitle(),
                this.entry.getTopId(),
                this.entry.getBottomId(),
                this.entry.getNorthId(),
                this.entry.getSouthId(),
                this.entry.getWestId(),
                this.entry.getEastId(),
                this.entry.getxCoord(),
                this.entry.getyCoord(),
                this.entry.getzCoord(),
                this.region.getKingdom().getEntry().getName(),
                this.region.getName(),
                this.entry.getSpecialLocationType(),
                this.entry.isSafeZone(),
                this.entry.getKeeperType(),
                this.entry.getKeeperName(),
                this.entry.getSpecialLocationId(),
                this.entry.getPictureId(),
                this.entry.isSquare(),
                this.entry.isSafeRegion(),
                this.entry.getPictureTime(),
                this.entry.getKeeperTime(),
                this.entry.getRegionId()
            );
        }
        return this.locationMessage;
    }

    /**
     * Gets a value indicating whether this location doesn't contain any units.
     */
    public boolean isEmpty() {
        return this.units.isEmpty();
    }

    public Set<Unit> getUnits() {
        return this.unitsView;
    }

    public void addCorpse(Corpse corpse) {
        this.corpses.put(corpse.getId(), corpse);
        send(getCorpseData());
    }

    /**
     * Gets a Corpse instance with the specified ID value.
     * @param corpseId Corpse ID to find.
     * @return Reference to a Corpse class instance.
     */
    public Corpse getCorpse(int corpseId) {
        return this.corpses.get(corpseId);
    }

    public void addItem(Item item) {
        Item _item = this.items.get(item.getPrototype());
        if (_item != null && ItemUtils.areStackable(_item, item)) {
            _item.stackItem(item);
        } else {
            this.items.put(item.getPrototype(), item);
        }
        // Synchronize Items/Corpses data
        send(getCorpseData());
    }

    public Item getItem(ItemPrototype proto) {
        return this.items.get(proto);
    }

    public void removeItem(Item item) {
        if (this.items.remove(item.getPrototype()) != null) {
            send(getCorpseData());
        }
    }

    public void removeCorpse(Corpse corpse) {
        if (this.corpses.remove(corpse.getId()) != null) {
            send(getCorpseData());
        }
    }

    public void unitEntered(Unit unit) {
        synchronized (this.units) {
            this.units.add(unit);
        }
        if (unit.getUnitType() == UnitTypes.Player) {
            Player player = (Player)unit;
            if (player.getSession() != null) {
                player.getSession().send(getData());
                player.getSession().send(getNeighborsInfoData());
                player.getSession().send(getCorpseData());
            }
        }

        setUpdatable(true);
    }

    public void unitLeft(Unit unit) {
        synchronized (this.units) {
            if (!this.units.remove(unit)) {
                return;
            }
        }
        setUpdatable(true);
    }

    public void unitVisualChanged(Unit unit) {
        this.setUpdatable(true);
        if (!this.updatable) {
            this.updatedUnit = unit;
        }
        // Will be updated already
        else {
            // updatable == true AND updatedUnit == null means that "pss" command should be sent to all units
            if (this.updatedUnit == null) {
                return;
            }

            // Exclude double unit changing as the same unit
            if (this.updatedUnit != unit) {
                this.updatedUnit = null;
            }
        }
    }

    /**
     * Sends the specified message to all players at this location
     */
    public void send(ServerMessage message) {
        this.send(null, null, message);
    }

    /**
     * Sends the specified message to all players at this location
     * except the specified unit (but this unit should be a Player class instance).
     */
    public void send(Unit exception, ServerMessage message) {
        this.send(exception, null, message);
    }

    public void send(Unit exception1, Unit exception2, ServerMessage message) {
        for (Unit unit : this.units) {
            if (unit.getUnitType() == UnitTypes.Player) {
                Player pl = (Player) unit;
                // Except Player
                if (pl == exception1 || pl == exception2) {
                    continue;
                }

                if (pl.getSession() == null) {
                    continue;
                }

                pl.getSession().send(message);
            }
        }
    }

    public ServerMessage getCorpseData() {
        // Corpses
        List<CorpseListMessage.CorpseLocationInfo> corpses = new ArrayList<>();
        for (Corpse corpse : this.corpses.values()) {
            corpses.add(new CorpseListMessage.CorpseLocationInfo(
                    corpse.getId(),
                    corpse.getOwner().getUnitType() == UnitTypes.Player,
                    corpse.getOwner().getName()
            ));
        }
        // Items
        List<CorpseListMessage.ItemLocationInfo> items = new ArrayList<>();
        for (Item item : this.items.values()) {
            items.add(new CorpseListMessage.ItemLocationInfo(
                    item.getTemplate().getId(),
                    item.getModification(),
                    item.getDurability(),
                    item.getCount()
            ));
        }

        return new CorpseListMessage(corpses, items);
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>(this.units.size());
        synchronized (this.units) {
            for (Unit unit : this.units) {
                if (unit.getUnitType() == UnitTypes.Player) {
                    players.add((Player) unit);
                }
            }
        }
        return players;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Location location = (Location) o;

        return this.entry.getId() == location.entry.getId();
    }

    @Override
    public int hashCode() {
        return this.entry.hashCode();
    }

    public void update(int time) {
        this.updatable = false;

        if (this.units.size() == 0) {
            this.updatedUnit = null;
            return;
        }

        synchronized (this.units) {
            for (Unit unit : this.units) {
                // The next part is for players only
                if (unit.getUnitType() != UnitTypes.Player) continue;

                Player player = (Player) unit;

                // Send new Units list
                // At least 1 unit should be changed
                // and this unit should not be an updatable unit
                if (this.updatedUnit == player) continue;
                if (player.getSession() == null) continue;
                // Size - 1 == exclude itself
                List<PositionUnitListMessage.UnitInfo> unitInfos = new ArrayList<>(this.units.size() - 1);
                for (Unit _unit : this.units) {
                    if (_unit == unit) continue;

                    int health = (int) (73d * _unit.getHealth() / _unit.getParameters().value(Parameters.Health));
                    int mana = (int) (73d * _unit.getMana() / _unit.getParameters().value(Parameters.Mana));
                    boolean isGroupLeader = false;
                    int groupLeaderId = 0;
                    if (_unit.isPlayer() && ((Player) _unit).getGroup() != null) {
                        Group group = ((Player) _unit).getGroup();
                        isGroupLeader = group.getLeader() == _unit;
                        groupLeaderId = group.getLeader().getId();
                    }
                    CreatureBossTypes bossType = CreatureBossTypes.Normal;
                    if (_unit.getUnitType() == UnitTypes.Creature) {
                        bossType = ((Creature) _unit).getBossType();
                    }

                    unitInfos.add(new PositionUnitListMessage.UnitInfo(
                            _unit.getName(),
                            _unit.getId(),
                            _unit.getAvatar(),
                            health,
                            _unit.getLevel(),
                            _unit.getReligion(),
                            _unit.getReligLevel(),
                            isGroupLeader,
                            groupLeaderId,
                            _unit.getTarget() == null ? 0 : _unit.getTarget().getId(),
                            mana,
                            bossType
                    ));
                }

                player.getSession().send(new PositionUnitListMessage(unitInfos));
            }
        }

        this.updatedUnit = null;
    }
}
