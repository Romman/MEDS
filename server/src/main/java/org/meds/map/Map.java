package org.meds.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.meds.Group;
import org.meds.Player;
import org.meds.Unit;
import org.meds.data.dao.DAOFactory;
import org.meds.data.dao.MapDAO;
import org.meds.enums.MovementDirections;
import org.meds.logging.Logging;

import org.meds.net.ServerCommands;
import org.meds.net.ServerPacket;

public class Map {

    private static Map instance;

    public static Map getInstance() {
        if (Map.instance == null) {
            Map.instance = new Map();
        }

        return Map.instance;
    }

    private HashMap<Integer, Kingdom> kingdoms;
    private HashMap<Integer, Region> regions;
    private HashMap<Integer, Location> locations;

    private HashMap<Integer, Shop> shops;

    private HashMap<Unit, MovementDirections> unitMovement;

    private int corpseIdCounter;

    private HashSet<Location> updatedLocations;

    private Map() {
        this.kingdoms = new HashMap<>();
        this.regions = new HashMap<>();
        this.locations = new HashMap<>();
        this.updatedLocations = new HashSet<>();
        this.shops = new HashMap<>();
        this.unitMovement = new HashMap<>();

        this.corpseIdCounter = 1;
    }

    public Location getLocation(int locationId) {
        return this.locations.get(locationId);
    }

    public Region getRegion(int regionId) {
        return this.regions.get(regionId);
    }

    public Kingdom getKingdom(int kingdomId) {
        return this.kingdoms.get(kingdomId);
    }

    public Shop getShop(int shopId) {
        return this.shops.get(shopId);
    }

    public void load() {
        MapDAO mapDAO = DAOFactory.getFactory().getMapDAO();
        List<org.meds.data.domain.Kingdom> kingdomEntries = mapDAO.getKingdoms();
        for (org.meds.data.domain.Kingdom entry : kingdomEntries) {
            this.kingdoms.put(entry.getId(), new Kingdom(entry));
        }
        Logging.Info.log("Loaded " + this.kingdoms.size() + " kingdoms");

        List<org.meds.data.domain.Region> regionEntries = mapDAO.getRegions();
        for (org.meds.data.domain.Region entry : regionEntries) {
            Region region = new Region(entry);
            this.regions.put(region.getId(), region);
            region.getKingdom().addRegion(region);
        }
        Logging.Info.log("Loaded " + this.regions.size() + " regions");

        List<org.meds.data.domain.Location> locations = mapDAO.getLocations();
        for (org.meds.data.domain.Location entry : locations) {
            Location location = new Location(entry);
            this.locations.put(entry.getId(), location);
            location.getRegion().addLocation(location);
        }
        Logging.Info.log("Loaded " + this.locations.size() + " locations");

        // Filter duplicate values, that are the result of left outer join
        List<org.meds.data.domain.Shop> shops = mapDAO.getShops();
        for (org.meds.data.domain.Shop entry : shops) {
            Shop shop = new Shop(entry);
            shop.load();
            this.shops.put(entry.getId(), shop);
        }
        Logging.Info.log("Loaded " + this.shops.size() + " shops");
    }

    public void addLocationUpdate(Location location) {
        this.updatedLocations.add(location);
    }

    public void registerMovement(Unit unit, MovementDirections direction) {
        this.unitMovement.put(unit, direction);
    }

    public int getNextCorpseId() {
        return this.corpseIdCounter++;
    }

    public void update(int time) {
        // Move all moving units
        synchronized (this.unitMovement) {
            for (java.util.Map.Entry<Unit, MovementDirections> entry : this.unitMovement.entrySet()) {
                Unit mover = entry.getKey();

                // Current position doesn't exist
                if (mover.getPosition() == null) {
                    Logging.Warn.log(mover + " tries to move but the current position is not set.");
                    continue;
                }

                // While fighting cancel the moving by putting at the same location
                if (mover.isInCombat()) {
                    mover.setPosition(mover.getPosition());
                }

                if (!mover.canMove()) {
                    continue;
                }

                if (mover.isPlayer()) {
                    Player player = (Player) mover;
                    // In a group but not a leader
                    if (player.getGroup() != null && player.getGroup().getLeader() != player) {
                        continue;
                    }
                }

                // Does neighbour location exist?
                Location location = mover.getPosition().getNeighbourLocation(entry.getValue());
                if (location == null) {
                    continue;
                }

                Location prevLocation = mover.getPosition();
                // Move the unit
                if (mover.isPlayer()) {
                    Player player = (Player) mover;
                    Group group = player.getGroup();
                    // Move the whole group to this location
                    if (group != null) {
                        for (Player member : group) {
                            member.setPosition(location);
                        }
                    } else {
                        player.setPosition(location);
                    }
                } else {
                    mover.setPosition(location);
                }

                ServerPacket packet = new ServerPacket(ServerCommands.ServerMessage);
                switch (entry.getValue()) {
                    case Up:
                        packet.add(450);
                        break;
                    case Down:
                        packet.add(451);
                        break;
                    case North:
                        packet.add(452);
                        break;
                    case South:
                        packet.add(453);
                        break;
                    case West:
                        packet.add(454);
                        break;
                    case East:
                        packet.add(455);
                        break;
                    default:
                        break;
                }
                packet.add(mover.getName());
                prevLocation.send(mover, packet);
            }

            this.unitMovement.clear();
        }

        synchronized (this.updatedLocations) {
            for (Location location : this.updatedLocations) {
                location.update(time);
            }
            this.updatedLocations.clear();
        }
    }
}
