package org.meds.map;

import java.util.*;

import org.meds.Player;
import org.meds.Unit;
import org.meds.net.ServerOpcodes;
import org.meds.net.ServerPacket;
import org.meds.util.Random;

public class Region
{
    private class PlayerPositionChanged implements Unit.PositionChangedListener {
        @Override
        public void handleEvent(Unit.PositionEvent event) {
            Region newRegion = event.getNewLocation().getRegion();
            Player player = (Player)event.getSource();

            // Region changed
            if (!newRegion.equals(Region.this)) {
                // Relocate a player
                Region.this.removePlayer(player);
                newRegion.addPlayer(player);

                // Send "Region arrival" and "Region left" messages
                if (player.getSession() != null) {
                    player.getSession().addServerMessage(10, Region.this.getName());

                    ServerPacket arrivalMessage = new ServerPacket(ServerOpcodes.ServerMessage);
                    arrivalMessage.add(11);
                    arrivalMessage.add(newRegion.getName());
                    if (!newRegion.isRoad()) {
                        arrivalMessage.add(newRegion.getMinLevel());
                        arrivalMessage.add(newRegion.getMaxLevel());
                    }
                    player.getSession().addData(arrivalMessage);
                }

            // Otherwise just notify everyone about location changed
            } else {
                Region.this.addToAll(new ServerPacket(ServerOpcodes.PlayerLocation)
                        .add(player.getGuid())
                        .add(event.getNewLocation().getId()));
            }
        }
    }

    private org.meds.database.entity.Region entry;

    private Kingdom kingdom;
    /**
     * List of all the location the belong to this region.
     */
    private List<Location> locations;
    /**
     * Unmodifiable list of all locations
     */
    private List<Location> locationsView;
    /**
     * List of only non-special locations (i.e exclude shops, guilds, stars etc.; locations where creature are allowed to be)
     */
    private List<Location> ordinaryLocations;

    private ServerPacket locationListData;

    /**
     * Players that are located at the current location
     */
    private Set<Player> players;

    private PlayerPositionChanged positionChangedHandler;

    public Region(org.meds.database.entity.Region entry) {
        this.entry = entry;
        this.kingdom = Map.getInstance().getKingdom(entry.getKingdomId());
        if (this.kingdom == null) {
            throw new IllegalArgumentException(String.format("Region %d references to a non-existing kingdom %d",
                    entry.getId(), entry.getKingdomId()));
        }

        this.locations = new ArrayList<>();
        this.locationsView = Collections.unmodifiableList(this.locations);
        this.ordinaryLocations = new ArrayList<>();
        this.players = new HashSet<>();

        this.positionChangedHandler = new PlayerPositionChanged();
    }

    public int getId() {
        return this.entry.getId();
    }

    public String getName() {
        return this.entry.getName();
    }

    public Kingdom getKingdom() {
        return this.kingdom;
    }

    public boolean isRoad() {
        return this.entry.isRoad();
    }

    public int getMinLevel() {
        return this.entry.getMinLevel();
    }

    public int getMaxLevel() {
        return this.entry.getMaxLevel();
    }

    /**
     * Adds the specified location to this region.
     * @param location A location instance to add.
     */
    public void addLocation(Location location) {
        this.locations.add(location);
        if (!location.isSafeZone())
            this.ordinaryLocations.add(location);
    }

    public void addPlayer(Player player) {
        // Already contains the player
        if (!this.players.add(player))
            return;

        // Send the region content to the new player
        if (player.getSession() != null) {
            player.getSession().addData(getPlayersLocationData());
        }

        // Notify the region about player entered
        addToAll(new ServerPacket(ServerOpcodes.PlayerLocation)
                .add(player.getGuid())
                .add(player.getPosition().getId()));
        player.addPositionChangedListener(this.positionChangedHandler);
    }

    public void removePlayer(Player player) {
        // Doesn't contain the player
        if (!this.players.remove(player))
            return;

        // Notify the region about player left
        addToAll(new ServerPacket(ServerOpcodes.PlayerLocation)
                .add(player.getGuid())
                .add(0));
        player.removePositionChangedListener(this.positionChangedHandler);
    }

    public List<Location> getLocations() {
        return this.locationsView;
    }

    public Location getRandomLocation() {
        return this.getRandomLocation(true);
    }

    public Location getRandomLocation(boolean includeSpecial) {
        if (includeSpecial)
            return this.locations.get(Random.nextInt(0, this.locations.size()));

        return this.ordinaryLocations.get(Random.nextInt(0, this.ordinaryLocations.size()));
    }

    public void sendToAll(ServerPacket packet) {
        synchronized (this.players) {
            for (Player player : this.players) {
                if (player.getSession() != null)
                    player.getSession().send(packet);
            }
        }
    }

    public void addToAll(ServerPacket packet) {
        synchronized (this.players) {
            for (Player player : this.players) {
                if (player.getSession() != null)
                    player.getSession().addData(packet);
            }
        }
    }

    private ServerPacket getPlayersLocationData() {
        ServerPacket packet = new ServerPacket(ServerOpcodes.PlayersLocation);
        synchronized (this.players) {
            for (Player player : this.players) {
                packet.add(player.getGuid());
                packet.add(player.getPosition().getId());
            }
        }
        return packet;
    }

    public ServerPacket getLocationListData() {
        if (this.locationListData == null) {
            this.locationListData = new ServerPacket(ServerOpcodes.RegionLocations)
                .add(this.entry.getId());
            for (Location location : this.locations)
                this.locationListData.add(location.getId());
        }

        return this.locationListData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Region region = (Region) o;

        return this.entry.getId() == region.entry.getId();
    }

    @Override
    public int hashCode() {
        return this.entry.getId();
    }
}
