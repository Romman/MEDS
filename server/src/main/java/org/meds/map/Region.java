package org.meds.map;

import org.meds.Player;
import org.meds.Unit;
import org.meds.net.Session;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.ChatMessage;
import org.meds.net.message.server.PlayerLocationMessage;
import org.meds.net.message.server.PlayersLocationMessage;
import org.meds.net.message.server.QuestListRegionMessage;
import org.meds.net.message.server.RegionLocationsMessage;
import org.meds.util.Random;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class Region {

    private class PlayerPositionChanged implements Unit.PositionChangedListener {
        @Override
        public void handleEvent(Unit.PositionEvent event) {
            Region newRegion = event.getNewLocation().getRegion();
            Player player = (Player)event.getSource();

            // Send Region quests
            Session session = player.getSession();
            if (session != null) {
                session.send(newRegion.getQuestListData());
            }

            // Region changed
            if (!newRegion.equals(Region.this)) {
                // Relocate a player
                Region.this.removePlayer(player);
                newRegion.addPlayer(player);

                // Send "Region arrival" and "Region left" messages
                if (session != null) {
                    session.send(new ChatMessage(10, Region.this.getName()));

                    // Arrival message is not for a road
                    if (!newRegion.isRoad()) {
                        ChatMessage arrivalMessage = new ChatMessage(
                                11,
                                newRegion.getName(),
                                newRegion.getMinLevel(),
                                newRegion.getMaxLevel()
                        );
                        session.send(arrivalMessage);
                    }
                }

            // Otherwise just notify everyone about location changed
            } else {
                Region.this.send(new PlayerLocationMessage(player.getId(), event.getNewLocation().getId()));
            }
        }
    }

    @Autowired
    private MapManager mapManager;

    private org.meds.data.domain.Region entry;

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

    private ServerMessage locationsMessage;

    /**
     * Players that are located at the current location
     */
    private Set<Player> players;

    private PlayerPositionChanged positionChangedHandler;

    public Region(org.meds.data.domain.Region entry, Kingdom kingdom) {
        this.entry = entry;
        this.kingdom = kingdom;
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
            player.getSession().send(getPlayersLocationData());
        }

        // Notify the region about player entered
        send(new PlayerLocationMessage(player.getId(), player.getPosition().getId()));
        player.addPositionChangedListener(this.positionChangedHandler);
    }

    public void removePlayer(Player player) {
        // Doesn't contain the player
        if (!this.players.remove(player))
            return;

        // Notify the region about player left
        send(new PlayerLocationMessage(player.getId(), 0));
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

    /**
     * Sends the specified packet to all players in this region
     */
    public void send(ServerMessage message) {
        synchronized (this.players) {
            for (Player player : this.players) {
                if (player.getSession() != null)
                    player.getSession().send(message);
            }
        }
    }

    private ServerMessage getPlayersLocationData() {
        ServerMessage playersMessage;
        synchronized (this.players) {
            List<PlayerLocationMessage> playersLocation = new ArrayList<>(this.players.size());
            for (Player player : this.players) {
                playersLocation.add(new PlayerLocationMessage(player.getId(), player.getPosition().getId()));
            }
            playersMessage = new PlayersLocationMessage(playersLocation);
        }
        return playersMessage;
    }

    public ServerMessage getLocationListData() {
        if (this.locationsMessage == null) {
            List<Integer> locationIds = new ArrayList<>(this.locations.size());
            for (Location location : this.locations) {
                locationIds.add(location.getId());
            }
            this.locationsMessage = new RegionLocationsMessage(this.entry.getId(), locationIds);
        }

        return this.locationsMessage;
    }

    public ServerMessage getQuestListData() {
        // TODO: Implement Region quests
        return new QuestListRegionMessage();
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
        return this.entry.hashCode();
    }
}
