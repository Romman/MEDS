package org.meds.net;

import org.meds.Player;
import org.meds.World;
import org.meds.data.dao.DAOFactory;
import org.meds.data.domain.Character;
import org.meds.logging.Logging;
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

    private ServerPacket packetBuffer;

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
        this.packetBuffer = new ServerPacket();

        this.key = Random.nextInt(2000000000) + 100000000;

        this.sessionToString = "Session [" + this.socket.getInetAddress().toString() + "]: ";

        this.sessionContext.setSession(this);
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
        Session.this.currentIp = Session.this.socket.getInetAddress().getHostAddress();
        if (characterData.getLastLoginDate() != 0) {
            Session.this.lastLoginDate = DateFormatter.format(characterData.getLastLoginDate() * 1000);
        } else {
            Session.this.lastLoginDate = "-";
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
        try {
            InputStream is = this.socket.getInputStream();
            int bufferSize = 1024;

            while (true) {
                int receivedSize = 0;
                String receivedString = "";
                byte[] buffer = new byte[bufferSize];
                do {
                    receivedSize = is.read(buffer);
                    // End Of Stream / Socket is closed
                    if (receivedSize == -1) {
                        Logging.Debug.log(toString() + "Received -1");
                        disconnect();
                        return;
                    }
                    receivedString += new String(Arrays.copyOf(buffer, receivedSize), "Unicode");
                }
                while (receivedSize == bufferSize);
                Logging.Debug.log(toString() + "Received string: " + receivedString);

                List<ClientCommandData> commandDataList;
                try {
                    commandDataList = packetParser.parse(receivedString);
                } catch (ClientPacketParseException ex) {
                    Logging.Warn.log(toString() + " :: Parsing exception :: " + ex.getMessage());
                    continue;
                }

                for (ClientCommandData clientCommandData : commandDataList) {
                    try {
                        commandHandlingManager.handle(clientCommandData);
                    } catch (ClientCommandHandleException ex) {
                        // Handling failed due to another exception
                        if (ex.getCause() != null) {
                            Logging.Error.log(toString() + " :: " + ex.getMessage(), ex.getCause());
                        } else {
                            Logging.Warn.log(toString() + " :: " + ex.getMessage());
                        }
                    }
                }
                Session.sendBuffers();
            }
        } catch (IOException e) {
            // This exception is expected on server shutdown
            if (!Server.isStopping()) {
                Logging.Error.log(toString() + "An exception while reading a socket.", e);
                if (e.getClass() == SocketException.class) {
                    disconnect();
                }
            }
        }
    }

    private void disconnect() {
        try {
            this.socket.close();
        } catch (IOException ex) {
            Logging.Error.log(toString() + "IOException while trying to close the Session socket", ex);
        }

        for (DisconnectListener listener : this.listeners)
            listener.disconnect(this);

        this.listeners.clear();
    }

    /**
     * Sends an accumulated packet buffer for the current session.
     */
    private void send() {
        if (this.packetBuffer == null || this.packetBuffer.isEmpty())
            return;

        OutputStream os;
        try {
            os = this.socket.getOutputStream();
            byte[] bytes = packetBuffer.getBytes();
            os.write(bytes);
            Logging.Debug.log(toString() + "Sending data: " + packetBuffer.toString().replace('\u0000', '\n'));
        } catch (IOException e) {
            Logging.Error.log(toString() + "IOException while writing to a socket: " + e.getMessage());
        } finally {
            // Clean it anyway
            this.packetBuffer.clear();
        }
    }

    public Session send(ServerPacket packet) {
        this.packetBuffer.add(packet);
        Session.sessionsToSend.add(this);
        return this;
    }

    public Session sendServerMessage(int messageId, String... values) {
        ServerPacket packet = new ServerPacket(ServerCommands.ServerMessage).add(messageId);
        for (String string : values)
            packet.add(string);
        this.send(packet);
        return this;
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
