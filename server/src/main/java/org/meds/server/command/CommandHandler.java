package org.meds.server.command;

import java.util.Set;

/**
 * A strategy for handling server managing commands. Those commands can be entered with
 * a standard console input of the application.
 */
interface CommandHandler {

    /**
     * Gets the Set of available subCommands of the command
     * @return Set of subCommands names or <code>null</code> if the command doesn't have any subCommands.
     */
    Set<String> getSubCommands();

    void handle(String[] args);
}
