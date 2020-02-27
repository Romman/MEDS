package org.meds.server.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.server.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ServerCommandWorker implements Runnable {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    public Server server;

    @Autowired
    public ApplicationContext applicationContext;

    private Map<String, CommandHandler> commands;

    private Scanner scanner;

    @PostConstruct
    public void init() {
        this.commands = this.applicationContext.getBeansWithAnnotation(ServerCommand.class)
                .entrySet()
                .stream()
                .filter(entry -> {
                    // Must implement CommandHandler interface
                    return entry.getValue() instanceof CommandHandler;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (CommandHandler) entry.getValue()));
        logger.info("ServerCommandWorker: found {} command handlers.", this.commands.size());

        server.addStopListener(() -> {
            // Set isStopping value and the World.stop() method
            // will be called just before the next update.
            ServerCommandWorker.this.scanner.close();
        });
    }

    @Override
    public void run() {
        logger.debug("ServerCommandWorker has been started");
        this.scanner = new Scanner(System.in);
        String line;
        try {
            while (this.scanner.hasNext()) {
                line = this.scanner.nextLine();
                if (line.isEmpty())
                    continue;
                line = line.toLowerCase();
                handleLine(line);
            }
        } catch (Exception ex) {
            // While stopping the Server any Exception is expected here
            if (server.isStopping()) {
                logger.error("An exception had occurred while reading the Server Command.", ex);
            }
        }
    }

    public Collection<String> getAvailableCommands() {
        return this.commands.keySet();
    }

    private void handleLine(String line) {
        logger.debug("Server command entered: {}", line);
        String[] commandArgs = line.split(" ");

        CommandHandler handler = this.commands.get(commandArgs[0]);
        if (handler == null) {
            System.out.println("Command \"" + commandArgs[0] + "\" not found. To see the list of all available handlers type \"help\"");
            return;
        }

        // Check subCommands
        Set<String> subCommands = handler.getSubCommands();
        if (subCommands != null) {
            if (commandArgs.length == 1) {
                notifySubCommands(commandArgs[0], subCommands);
                return;
            }
            if (!subCommands.contains(commandArgs[1])) {
                System.out.println("Unknown subcommand \"" + commandArgs[1] + "\".");
                notifySubCommands(commandArgs[0], subCommands);
                return;
            }
        }
        handler.handle(Arrays.copyOfRange(commandArgs, 1, commandArgs.length));
    }

    private void notifySubCommands(String command, Set<String> subCommands) {
        System.out.println("Use the following subcommands for the command \"" + command + "\":");
        subCommands.forEach(subCommand -> {
            System.out.println(" " + subCommand);
        });
    }
}
