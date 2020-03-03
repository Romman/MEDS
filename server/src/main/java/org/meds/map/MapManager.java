package org.meds.map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.Group;
import org.meds.Player;
import org.meds.Unit;
import org.meds.data.dao.DAOFactory;
import org.meds.data.dao.MapDAO;
import org.meds.enums.MovementDirections;
import org.meds.net.message.server.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Component
public class MapManager {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private DAOFactory daoFactory;

    private HashMap<Integer, Kingdom> kingdoms;
    private HashMap<Integer, Region> regions;
    private HashMap<Integer, Location> locations;

    private HashMap<Integer, Shop> shops;

    private HashMap<Unit, MovementDirections> unitMovement;

    private int corpseIdCounter;

    private HashSet<Location> updatedLocations;

    private MapManager() {
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

    @PostConstruct
    public void load() {
        MapDAO mapDAO = daoFactory.getMapDAO();
        List<org.meds.data.domain.Kingdom> kingdomEntries = mapDAO.getKingdoms();
        for (org.meds.data.domain.Kingdom entry : kingdomEntries) {
            this.kingdoms.put(entry.getId(), new Kingdom(entry));
        }
        logger.info("Loaded {} kingdoms", this.kingdoms.size());

        List<org.meds.data.domain.Region> regionEntries = mapDAO.getRegions();
        for (org.meds.data.domain.Region entry : regionEntries) {
            Region region = new Region(entry, this.kingdoms.get(entry.getKingdomId()));
            this.regions.put(region.getId(), region);
            region.getKingdom().addRegion(region);
        }
        logger.info("Loaded {} regions", this.regions.size());

        List<org.meds.data.domain.Location> locations = mapDAO.getLocations();
        for (org.meds.data.domain.Location entry : locations) {
            Location location = applicationContext.getBean(Location.class, entry, getRegion(entry.getRegionId()));
            this.locations.put(entry.getId(), location);
            location.getRegion().addLocation(location);
        }
        logger.info("Loaded {} locations", this.locations.size());

        // Filter duplicate values, that are the result of left outer join
        List<org.meds.data.domain.Shop> shops = mapDAO.getShops();
        for (org.meds.data.domain.Shop entry : shops) {
            Shop shop = applicationContext.getBean(Shop.class, entry);
            shop.load();
            this.shops.put(entry.getId(), shop);
        }
        logger.info("Loaded {} shops", this. shops.size());
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
                    logger.warn("{} tries to move but the current position is NULL. Skipped", mover);
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

                int messageId;
                switch (entry.getValue()) {
                    case Up:
                        messageId = 450;
                        break;
                    case Down:
                        messageId = 451;
                        break;
                    case North:
                        messageId = 452;
                        break;
                    case South:
                        messageId = 453;
                        break;
                    case West:
                        messageId = 454;
                        break;
                    case East:
                        messageId = 455;
                        break;
                    default:
                        throw new IllegalStateException("Attempt to move to unknown direction: " + entry.getValue());
                }
                prevLocation.send(mover, new ChatMessage(messageId, mover.getName()));
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
