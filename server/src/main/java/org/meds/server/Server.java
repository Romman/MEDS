package org.meds.server;

import org.meds.Locale;
import org.meds.World;
import org.meds.database.DataStorage;
import org.meds.logging.Logging;
import org.meds.map.MapManager;
import org.meds.net.Session;
import org.meds.server.command.ServerCommandWorker;
import org.meds.util.DateFormatter;
import org.meds.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

@Component
public class Server {

    public interface StopListener extends EventListener {
        void stop();
    }

    private class SessionDisconnect implements Session.DisconnectListener {
        @Override
        public void disconnect(Session session) {
            // Ignore disconnection while stopping
            if (Server.this.isStopping())
                return;

            Logging.Debug.log("Session disconnected");
            Server.this.sessions.remove(session);
        }
    }

    public static final int BUILD = 33554443; // 2.0.0.11

    public static final int MAX_ALLOWED_BUILD = 33555200; // 2.0.3.0

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(ServerConfiguration.class);

        try {
            Random.initialize();

            // Load map data
            MapManager mapManager = applicationContext.getBean(MapManager.class);
            Logging.Info.log("Map is loaded");

            // Load locale
            applicationContext.getBean(Locale.class);

            Server server = applicationContext.getBean(Server.class);
            server.start();
        } catch (Exception ex) {
            Logging.Fatal.log("An exception has occurred while starting the Server", ex);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ServerCommandWorker serverCommandWorker;
    @Autowired
    private World world;

    @Autowired
    private DataStorage dataStorage;
    @Autowired
    private Locale locale;

    @Value("${server.ip}")
    private String serverIp;
    @Value("${server.port}")
    private Integer serverPort;

    private boolean stopping;
    private String serverStartTime;
    private int startTimeMillis;

    private java.util.Map<Session, Socket> sessions;
    private ServerSocket serverSocket;

    private SessionDisconnect sessionDisconnector;
    private Set<StopListener> stopListeners = new HashSet<>();

    public Server() {
        this.sessions = new HashMap<>(100);
        this.stopListeners = new HashSet<>();
        this.sessionDisconnector = new SessionDisconnect();
    }

    @PostConstruct
    public void init() {
        dataStorage.loadRepositories();
        locale.load();
        Logging.Info.log("Database is loaded.");
    }

    public boolean isStopping() {
        return this.stopping;
    }

    public String getServerStartTime() {
        return this.serverStartTime;
    }

    public int getServerTimeMillis() {
        return (int) System.currentTimeMillis() - this.startTimeMillis;
    }

    public void addStopListener(StopListener listener) {
        this.stopListeners.add(listener);
    }

    public void removeStopListener(StopListener listener) {
        this.stopListeners.remove(listener);
    }

    public void start() {
        this.startTimeMillis = (int) System.currentTimeMillis();
        this.serverStartTime = DateFormatter.format(new Date());

        world.createCreatures();

        new Thread(serverCommandWorker, "Server Commands worker").start();

        new Thread(world, "World updater").start();

        Logging.Info.log("Waiting for connections...");

        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            Logging.Fatal.log("An exception on creating the server socket", e);
            return;
        }

        while (true) {
            try {
                Socket clientSocket = this.serverSocket.accept();
                Session session = applicationContext.getBean(Session.class, clientSocket);
                session.addDisconnectListener(this.sessionDisconnector);
                Thread thread = new Thread(session, "Session " + clientSocket.getInetAddress().toString() + " Worker");
                Logging.Debug.log("New socket client: " + clientSocket.getInetAddress().toString());
                this.sessions.put(session, clientSocket);
                thread.start();

            } catch (IOException ex) {
                // Stopping the Server - the next exception is the expected
                if (!this.stopping) {
                    Logging.Error.log("IO Exception while accepting a socket", ex);
                }
                break;
            }
        }
    }

    public void shutdown() {
        this.stopping = true;

        // Stop server socket
        try {
            this.serverSocket.close();
        } catch (IOException ex) {
            Logging.Error.log("IOException while stopping the server socket", ex);
        }


        // Then close all the session sockets
        for (Socket socket : this.sessions.values()) {
            try {
                socket.close();
            } catch (IOException ex) {
                Logging.Error.log("IOException while closing the session socket", ex);
            }
        }

        // Stop all the listeners
        this.stopListeners.forEach(StopListener::stop);

        // 5 seconds is enough for World to stop all the updated and save all the players
        Logging.Info.log("The server will be shut down in 5 seconds...");
        try {
            Thread.sleep(5000);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
