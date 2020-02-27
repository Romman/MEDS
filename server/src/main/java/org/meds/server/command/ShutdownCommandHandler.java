package org.meds.server.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.server.Server;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@ServerCommand("shutdown")
class ShutdownCommandHandler implements CommandHandler {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    public Server server;

    @Override
    public Set<String> getSubCommands() {
        return null;
    }

    @Override
    public void handle(String[] args) {
        logger.info("Shutting down...");
        server.shutdown();
    }
}
