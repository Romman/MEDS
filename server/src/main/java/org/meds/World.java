package org.meds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.data.dao.DAOFactory;
import org.meds.data.domain.CreatureTemplate;
import org.meds.database.Repository;
import org.meds.enums.CreatureTypes;
import org.meds.map.MapManager;
import org.meds.net.message.ServerMessage;
import org.meds.net.Session;
import org.meds.net.message.server.DayTimeMessage;
import org.meds.net.message.server.PlayerListAddMessage;
import org.meds.net.message.server.PlayerListDeleteMessage;
import org.meds.net.message.server.PlayerListUpdateMessage;
import org.meds.net.message.server.PlayerOnlineInfo;
import org.meds.net.message.server.PlayersOnlineMessage;
import org.meds.net.message.server.ServerTimeMessage;
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

    private final List<ServerMessage> addPlayersMessages;
    private final List<ServerMessage> updatePlayersMessages;
    private final List<ServerMessage> deletePlayersMessages;

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
        this.addPlayersMessages = new ArrayList<>();
        this.updatePlayersMessages = new ArrayList<>();
        this.deletePlayersMessages = new ArrayList<>();

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
        PlayerOnlineInfo playerInfo = new PlayerOnlineInfo(player.getId(),
                player.getName(),
                player.getLevel(),
                player.getReligion(),
                player.getReligLevel(),
                player.getGroup() != null && player.getGroup().getLeader() == player,
                player.getStatuses().getValue(),
                player.getClanId(),
                player.getClanMemberStatus().getValue(),
                0 // Religion Status. Always 0 while religion is not implemented
        );
        this.addPlayersMessages.add(new PlayerListAddMessage(playerInfo));
        logger.debug("World: addPlayersPacket has been updated");
    }

    public void playerLoggedOut(Player player) {
        this.units.remove(player.getId());
        this.players.remove(player.getId());
        this.deletePlayersMessages.add(
                new PlayerListDeleteMessage(player.getId())
        );
        logger.debug("{} just logged out.", player);
    }

    public void playerUpdated(Player player) {
        this.updatePlayersMessages.add(new PlayerListUpdateMessage(
                player.getId(),
                player.getLevel(),
                player.getReligion(),
                player.getReligLevel(),
                player.getGroup() != null && player.getGroup().getLeader() == player,
                player.getClanId(),
                player.getClanMemberStatus().getValue(),
                0
        ));
    }

    public ServerMessage getOnlineData() {
        List<PlayerOnlineInfo> playerInfos;
        synchronized (this.players) {
            playerInfos = new ArrayList<>(this.players.size());
            for (Player player : this.players.values()) {
                playerInfos.add(new PlayerOnlineInfo(player.getId(),
                        player.getName(),
                        player.getLevel(),
                        player.getReligion(),
                        player.getReligLevel(),
                        player.getGroup() != null && player.getGroup().getLeader() == player,
                        player.getStatuses().getValue(),
                        player.getClanId(),
                        player.getClanMemberStatus().getValue(),
                        0
                ));
            }
        }
        return new PlayersOnlineMessage(playerInfos);
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

    public ServerMessage getDayTimeData() {
        boolean isDaytime = this.dayTime < 360000;
        int time = isDaytime ? this.dayTime / 1000 : this.dayTime / 1000 - 360;

        return new DayTimeMessage(!isDaytime, time);
    }

    /**
     * Sends the specified packet to all players in the game.
     */
    public void send(ServerMessage message) {
        synchronized (this.players) {
            for (Player player : this.players.values()) {
                if (player.getSession() != null) {
                    player.getSession().send(message);
                }
            }
        }
    }

    public void send(Iterable<ServerMessage> messages) {
        synchronized (this.players) {
            for (Player player : this.players.values()) {
                if (player.getSession() != null) {
                    player.getSession().send(messages);
                }
            }
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
        synchronized (this.deletePlayersMessages) {
            if (!this.deletePlayersMessages.isEmpty()) {
                send(this.deletePlayersMessages);
                this.deletePlayersMessages.clear();
            }
        }

        synchronized (this.updatePlayersMessages) {
            if (!this.updatePlayersMessages.isEmpty()) {
                send(this.updatePlayersMessages);
                this.updatePlayersMessages.clear();
            }
        }

        synchronized (this.addPlayersMessages) {
            if (!this.addPlayersMessages.isEmpty()) {
                send(this.addPlayersMessages);
                this.addPlayersMessages.clear();
            }
        }

        // Update locations data (Movement, Unit list, etc.)
        mapManager.update(time);

        // Add Server Time Data and send the packet
        synchronized (this.players) {
            for (java.util.Map.Entry<Integer, Player> entry : this.players.entrySet()) {
                Session session = entry.getValue().getSession();
                if (session != null) {
                    session.send(new ServerTimeMessage(server.getUptimeMillis()));
                }
            }
        }

        org.meds.net.Session.sendBuffers();

        logger.debug("World update ends. New server Time: {}", server.getUptimeMillis());
    }
}
