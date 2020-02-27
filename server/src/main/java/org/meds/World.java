package org.meds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.data.dao.DAOFactory;
import org.meds.data.domain.CreatureTemplate;
import org.meds.database.Repository;
import org.meds.enums.CreatureTypes;
import org.meds.map.MapManager;
import org.meds.net.ServerCommands;
import org.meds.net.ServerPacket;
import org.meds.server.Server;
import org.meds.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class World implements Runnable {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Server server;
    @Autowired
    private Repository<CreatureTemplate> creatureTemplateRepository;
    @Autowired
    private DAOFactory daoFactory;
    @Autowired
    private MapManager mapManager;

    private int dayTime;

    private int tickTime;

    private HashMap<Integer, Unit> units;
    private HashMap<Integer, Player> players;

    private ServerPacket addPlayersPacket;
    private ServerPacket updatePlayersPacket;
    private ServerPacket deletePlayersPacket;

    private final List<Battle> battles;
    private final LinkedList<Battle> newBattles;

    private HashMap<Integer, CreatureTypes> creatureTypes;

    /**
     * Indicating whether the world is in stopping process
     */
    private boolean isStopping;

    private World() {
        this.players = new HashMap<>();
        this.units = new HashMap<>();
        this.addPlayersPacket = new ServerPacket();
        this.updatePlayersPacket = new ServerPacket();
        this.deletePlayersPacket = new ServerPacket();

        this.battles = new ArrayList<>();
        this.newBattles = new LinkedList<>();

        this.dayTime = 0;
    }

    public void playerLoggedIn(Player player) {
        // Due to possible delay this may happen
        if (this.isStopping) {
            return;
        }

        // Already in game
        if (this.players.containsKey(player.getId())) {
            return;
        }

        this.players.put(player.getId(), player);
        this.units.put(player.getId(), player);

        logger.debug("World: adding a new {}", player);
        this.addPlayersPacket.add(ServerCommands.PlayersListAdd)
            .add(player.getId())
            .add(player.getName())
            .add(player.getLevel())
            .add(player.getReligion())
            .add(player.getReligLevel())
            .add(player.getGroup() != null && player.getGroup().getLeader() == player ? "1" : "0")
            .add(player.getStatuses())
            .add(player.getClanId())
            .add(player.getClanMemberStatus())
            .add("0"); // Religion Status
        logger.debug("World: addPlayersPacket has been updated");
    }

    public void playerLoggedOut(Player player) {
        this.units.remove(player.getId());
        this.players.remove(player.getId());
        this.deletePlayersPacket.add(ServerCommands.PlayersListDelete).add(player.getId());
        logger.debug("{} just logged out.", player);
    }

    public void playerUpdated(Player player) {
        this.updatePlayersPacket.add(ServerCommands.PlayersListUpdate)
            .add(player.getId())
            .add(player.getLevel())
            .add(player.getReligion())
            .add(player.getReligLevel())
            // Next value sometimes is 4 or 5 (possibly related to pets)
            .add(player.getGroup() != null && player.getGroup().getLeader() == player ? "1" : "0")
            .add(player.getClanId())
            .add(player.getClanMemberStatus())
            .add("0");
    }

    public ServerPacket getOnlineData() {
        ServerPacket packet = new ServerPacket(ServerCommands.OnlineList);
        packet.add(this.players.size());
        synchronized (this.players) {
            for (Player player : this.players.values()) {
                packet.add(player.getId())
                    .add(player.getName())
                    .add(player.getLevel())
                    .add(player.getReligion())
                    .add(player.getReligLevel())
                    .add(player.getGroup() != null && player.getGroup().getLeader() == player ? "1" : "0")
                    .add(player.getStatuses())
                    .add(player.getClanId())
                    .add(player.getClanMemberStatus())
                    .add("0");
            }
        }
        return packet;
    }

    public Player getPlayer(int id) {
        return this.players.get(id);
    }

    public Unit getUnit(int id) {
        return this.units.get(id);
    }

    public CreatureTypes getCreatureType(int creatureTemplateId) {
        CreatureTypes type = this.creatureTypes.get(creatureTemplateId);
        if (type == null)
            return CreatureTypes.Normal;

        return type;
    }

    public Battle createBattle() {
        Battle newBattle = new Battle();
        this.newBattles.add(newBattle);
        return newBattle;
    }

    /**
     * Gets a Player if it was already logged or creates a new instance for further logging.
     * This method is called when new instance of Session class tries to find its Player instance.
     */
    public Player getOrCreatePlayer(int playerId) {
        Player player = this.players.get(playerId);
        if (player != null) {
            return player;
        }

        player = applicationContext.getBean(Player.class, playerId);

        // Error occurred while player was creating or loading
        if (player.create() == 0) {
            return null;
        }

        return player;
    }

    public void createCreatures() {

        // Generate CreatureTypes
        this.creatureTypes = new HashMap<>(creatureTemplateRepository.size());
        for (CreatureTemplate creatureTemplate : creatureTemplateRepository) {
            // Level 30 and higher
            if (creatureTemplate.getLevel() < 30)
                continue;

            // a random CreatureTypes value excluding CreatureTypes.Normal
            CreatureTypes type = CreatureTypes.values()[Random.nextInt(CreatureTypes.values().length - 1) + 1];

            this.creatureTypes.put(creatureTemplate.getTemplateId(), type);
        }


        // Load and spawn all the Creatures
        List<org.meds.data.domain.Creature> creatures = daoFactory.getWorldDAO().getCreatures();
        for (org.meds.data.domain.Creature entry : creatures) {
            Creature creature = applicationContext.getBean(Creature.class, entry);
            // For some reasons can not create this creature
            if (creature.create() == 0) {
                continue;
            }

            creature.spawn();
        }
        logger.info("Creatures have been created and spawned. Creatures count = {}", this.units.size());
    }

    public void unitCreated(Unit unit) {
        this.units.put(unit.getId(), unit);
    }

    public ServerPacket getDayTimeData() {
        String day = this.dayTime < 360000 ? "0" : "1";
        String time = this.dayTime < 360000 ? Integer.toString(this.dayTime / 1000) : Integer.toString(this.dayTime / 1000 - 360);

        return new ServerPacket(ServerCommands.DayTime)
            .add(day)
            .add(time);
    }

    /**
     * Sends the specified packet to all players in the game.
     */
    public void send(ServerPacket packet) {
        synchronized (this.players) {
            for (Player player : this.players.values())
                if (player.getSession() != null)
                    player.getSession().send(packet);
        }
    }

    @Override
    public void run() {
        server.addStopListener(() -> {
            // Set isStopping value and the World.stop() method
            // will be called just before the next update.
            World.this.isStopping = true;
        });

        long lastTickDuration = 0;
        long sleepTime = 0;

        do {
            // Stop the thread for (2000 - world update time)
            // As a result the whole update-sleep cycle takes exactly 2 seconds
            sleepTime = 2000 - lastTickDuration;
            // Minimal sleep time is 50 ms
            if (sleepTime < 50)
                sleepTime = 50;

//            Logging.Debug.log("World sleeping time: " + sleepTime);

            try {
                Thread.sleep(sleepTime);

                if (this.isStopping) {
                    stop();
                    return;
                }

                this.tickTime = (int)sleepTime;

                // Assign last tick to the current system time
                lastTickDuration = server.getUptimeMillis();

                this.update(this.tickTime);
            } catch(InterruptedException ex) {
                logger.error("A Thread error in World run ", ex);
            } catch(Exception ex) {
                logger.error("An error while updating server Tact " + this.tickTime, ex);
            }

            // How much time takes the world update
            lastTickDuration = server.getUptimeMillis() - lastTickDuration;
        } while(true);
    }

    private void stop() {
        logger.info("Stopping the World");
        // Save of the players to DB
        this.players.values().forEach(Player::save);
    }

    public void update(int time) {
//        Logging.Debug.log("World update starts; Diff time: %d", time);

        // Set new Day Time
        this.dayTime += time;
        // Night begins
        if (this.dayTime - time < 360000 && this.dayTime >= 360000) {
            send(getDayTimeData());
        }
        // New day begins
        else if (this.dayTime >= 720000) {
            this.dayTime -= 720000;
            send(getDayTimeData());
        }

        // Update all units (Creatures and Players)
        synchronized (this.units) {
            for (java.util.Map.Entry<Integer, Unit> entry : this.units.entrySet()) {
                entry.getValue().update(time);
            }
        }

        // Add new battles
        synchronized (this.newBattles) {
            this.battles.addAll(this.newBattles);
            this.newBattles.clear();
        }

        // Update battle process
        synchronized (this.battles) {
            Iterator<Battle> iterator = this.battles.iterator();
            while (iterator.hasNext()) {
                Battle battle = iterator.next();
                battle.update(time);
                if (!battle.isActive()) {
                    iterator.remove();
                }
            }
        }

        // Update online lists
        synchronized (this.deletePlayersPacket) {
            if (!this.deletePlayersPacket.isEmpty()) {
                send(this.deletePlayersPacket);
                this.deletePlayersPacket.clear();
            }
        }

        synchronized (this.updatePlayersPacket) {
            if (!this.updatePlayersPacket.isEmpty()) {
                send(this.updatePlayersPacket);
                this.updatePlayersPacket.clear();
            }
        }

        synchronized (this.addPlayersPacket) {
            if (!this.addPlayersPacket.isEmpty()) {
                send(this.addPlayersPacket);
                this.addPlayersPacket.clear();
            }
        }

        // Update locations data (Movement, Unit list, etc.)
        mapManager.update(time);

        // Add Server Time Data and send the packet
        synchronized (this.players) {
            for (java.util.Map.Entry<Integer, Player> entry : this.players.entrySet())
                if (entry.getValue().getSession() != null)
                    entry.getValue().getSession().send(new ServerPacket(ServerCommands.ServerTime).add(server.getUptimeMillis()));
        }

        org.meds.net.Session.sendBuffers();

        logger.debug("World update ends. New server Time: {}", server.getUptimeMillis());
    }
}
