package org.meds.server.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.Locale;
import org.meds.World;
import org.meds.database.DataStorage;
import org.meds.net.Session;
import org.meds.server.Server;
import org.meds.server.command.ServerCommandWorker;
import org.meds.util.DateFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * An implementation of the {@link Server} interface based on {@link java.net.Socket} class as a client
 * and {@link java.net.ServerSocket} as a server.
 */
public class SocketIOServer implements Server {

    private static Logger logger = LogManager.getLogger();

    private class SessionDisconnect implements Session.DisconnectListener {
        @Override
        public void disconnect(Session session) {
            // Ignore disconnection while stopping
            if (SocketIOServer.this.isStopping())
                return;

            SocketIOServer.this.sessions.remove(session);
            logger.debug("{} has been disconnected", session);
        }
    }

    private static final int BUILD = 33621507; // 2.1.6.3
    private static final int SUPPORTED_VERSION = 33620995; // 2.1.5.3

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

    public SocketIOServer() {
        this.sessions = new HashMap<>(100);
        this.stopListeners = new HashSet<>();
        this.sessionDisconnector = new SessionDisconnect();
    }

    @PostConstruct
    public void init() {
        dataStorage.loadRepositories();
        locale.load();
        logger.info("Database is loaded.");
    }

    @Override
    public int getBuildVersion() {
        return BUILD;
    }

    @Override
    public int getMinSupportedVersion() {
        return SUPPORTED_VERSION;
    }

    @Override
    public boolean isStopping() {
        return this.stopping;
    }

    @Override
    public String getFormattedStartTime() {
        return this.serverStartTime;
    }

    @Override
    public int getUptimeMillis() {
        return (int) System.currentTimeMillis() - this.startTimeMillis;
    }

    @Override
    public void addStopListener(StopListener listener) {
        this.stopListeners.add(listener);
    }

    @Override
    public void removeStopListener(StopListener listener) {
        this.stopListeners.remove(listener);
    }

    @Override
    public void start() {
        this.startTimeMillis = (int) System.currentTimeMillis();
        this.serverStartTime = DateFormatter.format(new Date());

        this.world.createCreatures();

        new Thread(this.serverCommandWorker, "Server Commands worker").start();

        new Thread(this.world, "World updater").start();

        logger.info("Waiting for connections...");

        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            logger.fatal("An exception on creating the server socket", e);
            return;
        }

        while (true) {
            try {
                Socket clientSocket = this.serverSocket.accept();
                Session session = applicationContext.getBean(Session.class, clientSocket);
                session.addDisconnectListener(this.sessionDisconnector);
                Thread thread = new Thread(session, "Session " + clientSocket.getInetAddress().toString() + " Worker");
                logger.debug("New socket client: {}", clientSocket.getInetAddress().toString());
                this.sessions.put(session, clientSocket);
                thread.start();

            } catch (IOException ex) {
                // Stopping the Server - the next exception is the expected
                if (!this.stopping) {
                    logger.error("IO Exception while accepting a socket", ex);
                }
                break;
            }
        }
    }

    @Override
    public void shutdown() {
        this.stopping = true;

        // Stop server socket
        try {
            this.serverSocket.close();
        } catch (IOException ex) {
            logger.error("IOException while stopping the server socket", ex);
        }


        // Then close all the session sockets
        for (Socket socket : this.sessions.values()) {
            try {
                socket.close();
            } catch (IOException ex) {
                logger.error("IOException while closing the session socket", ex);
            }
        }

        // Stop all the listeners
        this.stopListeners.forEach(StopListener::stop);

        // 5 seconds is enough for World to stop all the updated and save all the players
        logger.info("The server will be shut down in 5 seconds...");
        try {
            Thread.sleep(5000);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
