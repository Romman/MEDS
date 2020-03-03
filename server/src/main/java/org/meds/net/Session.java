package org.meds.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.meds.Player;
import org.meds.World;
import org.meds.data.dao.DAOFactory;
import org.meds.data.domain.Character;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;
import org.meds.net.message.StringDelimitedMessageWriteStream;
import org.meds.server.Server;
import org.meds.util.DateFormatter;
import org.meds.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

@Component
@Scope("prototype")
public class Session implements Runnable {

    private static final int SOCKET_BUFFER_SIZE = 1024;

    private static Logger logger = LogManager.getLogger();

    public interface DisconnectListener extends EventListener {
        public void disconnect(Session session);
    }

    private static Set<Session> sessionsToSend;

    static {
        sessionsToSend = new HashSet<>();
    }

    public static void sendBuffers() {
        synchronized (Session.sessionsToSend) {
            Iterator<Session> iterator = Session.sessionsToSend.iterator();
            while (iterator.hasNext()) {
                iterator.next().send();
                iterator.remove();
            }
        }
    }

    @Autowired
    private Server server;
    @Autowired
    private DAOFactory daoFactory;
    @Autowired
    private World world;
    @Autowired
    private ThreadSessionContext sessionContext;
    @Autowired
    private ClientPacketParser packetParser;
    @Autowired
    private ClientCommandHandlingManager commandHandlingManager;

    /**
     * Related Socket for this session.
     */
    private Socket socket;

    private Player player;

    /**
     * Current message stream as a buffer.
     *
     * @todo Insure thread-safety of this buffer
     */
    private StringDelimitedMessageWriteStream messageWriteStream;

    /**
     * Indicating whether the player passed login verification successful and loaded from DB.
     */
    private boolean authenticated;

    /**
     * Session key.
     */
    private int key;

    private Set<DisconnectListener> listeners;

    private String sessionToString;

    private String lastLoginIp;
    private String lastLoginDate;
    private String currentIp;

    public Session(Socket socket) {
        this.socket = socket;

        this.listeners = new HashSet<>();
        this.messageWriteStream = new StringDelimitedMessageWriteStream();

        this.key = Random.nextInt(2000000000) + 100000000;

        this.sessionToString = "Session [" + this.socket.getInetAddress().toString() + "]: ";
    }

    public int getKey() {
        return this.key;
    }

    public boolean isAuthenticated() {
        return this.authenticated;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public String getLastLoginDate() {
        return lastLoginDate;
    }

    public String getCurrentIp() {
        return currentIp;
    }

    public void addDisconnectListener(DisconnectListener listener) {
        this.listeners.add(listener);
    }

    public void removeDisconnectListener(DisconnectListener listener) {
        this.listeners.remove(listener);
    }

    public void authenticate(Character characterData) {
        // Create a Player instance with found id
        Player player = world.getOrCreatePlayer(characterData.getId());
        if (player == null) {
            throw new IllegalArgumentException(toString() + " cannot authenticated a player as null");
        }

        // Save the last login Ip
        this.lastLoginIp = characterData.getLastLoginIp();
        Date now = new Date();
        Session.this.currentIp = this.socket.getInetAddress().getHostAddress();
        if (characterData.getLastLoginDate() != 0) {
            this.lastLoginDate = DateFormatter.format(characterData.getLastLoginDate() * 1000);
        } else {
            this.lastLoginDate = "-";
        }

        characterData.setLastLoginIp(Session.this.currentIp);
        characterData.setLastLoginDate((int) (now.getTime() / 1000));
        daoFactory.getCharacterDAO().update(characterData);

        this.player = player;
        this.authenticated = true;
        // Change the String representation of the session
        this.sessionToString = "Session [" + this.player + "]: ";
        this.sessionContext.setPlayer(player);
    }

    @Override
    public void run() {
        this.sessionContext.setSession(this);
        try {
            while (true) {
                String receivedString = readSocket();
                if (receivedString == null) {
                    disconnect();
                    return;
                }
                logger.debug("{} Received string: {}", toString(), receivedString);

                List<ClientCommandData> commandDataList;
                try {
                    commandDataList = packetParser.parse(receivedString);
                } catch (ClientPacketParseException ex) {
                    logger.warn("{} :: Parsing exception :: {}", toString(), ex.getMessage());
                    continue;
                }

                for (ClientCommandData clientCommandData : commandDataList) {
                    try {
                        commandHandlingManager.handle(clientCommandData);
                    } catch (ClientCommandHandleException ex) {
                        // Handling failed due to another exception
                        if (ex.getCause() != null) {
                            logger.error(
                                    new ParameterizedMessage("{} :: Packet handling failed due to inner exception",
                                            toString()), ex);
                        } else {
                            logger.warn("{} :: Packet handling failed to to exception ({})",
                                    toString(), ex.getMessage());
                        }
                    }
                }
                Session.sendBuffers();
            }
        } catch (IOException e) {
            // This exception is expected on server shutdown
            if (!server.isStopping()) {
                logger.error(toString() + ":: An exception while reading a socket.", e);
                if (e.getClass() == SocketException.class) {
                    disconnect();
                }
            }
        }
    }

    private String readSocket() throws IOException {
        InputStream is = this.socket.getInputStream();

        int receivedSize = 0;
        String receivedString = "";
        byte[] buffer = new byte[SOCKET_BUFFER_SIZE];
        do {
            receivedSize = is.read(buffer);
            // End Of Stream / Socket is closed
            if (receivedSize == -1) {
                logger.debug("{} :: Receeved -1. Closing the socket.", toString());
                return null;
            }
            receivedString += new String(Arrays.copyOf(buffer, receivedSize), "Unicode");
        }
        while (receivedSize == SOCKET_BUFFER_SIZE);
        return receivedString;
    }

    private void disconnect() {
        try {
            this.socket.close();
        } catch (IOException ex) {
            logger.error(toString() + " :: IOException while trying to close the Session socket", ex);
        }

        for (DisconnectListener listener : this.listeners) {
            listener.disconnect(this);
        }
        this.listeners.clear();
    }

    /**
     * Sends an accumulated packet buffer for the current session.
     */
    private void send() {
        if (this.messageWriteStream == null || this.messageWriteStream.isEmpty()) {
            return;
        }

        OutputStream os;
        try {
            os = this.socket.getOutputStream();
            byte[] bytes = messageWriteStream.getBytes();
            os.write(bytes);
            logger.debug("{} :: Sending data: {}",
                    this::toString, () -> messageWriteStream.toString().replace('\u0000', '\n'));
        } catch (IOException e) {
            logger.error(toString() + " :: IOException writing to a socket:", e);
        } finally {
            // Clean it anyway
            this.messageWriteStream = new StringDelimitedMessageWriteStream();
        }
    }

    public void send(ServerMessage message) {
        this.messageWriteStream.newMessage(message.getIdentity());
        Session.sessionsToSend.add(this);
        message.serialize(this.messageWriteStream);
    }

    public void send(Iterable<ServerMessage> messages) {
        messages.forEach(this::send);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Session session = (Session) o;

        return this.key == session.key
                && socket != null ? socket.equals(session.socket) : session.socket != null;

    }

    @Override
    public int hashCode() {
        return this.socket != null ? socket.hashCode() + this.key : this.key;
    }

    @Override
    public String toString() {
        return this.sessionToString;
    }
}
