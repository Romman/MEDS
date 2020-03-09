package org.meds.net.handlers;

import org.meds.net.*;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.VersionMessage;
import org.meds.server.Server;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman
 */
@ClientCommand(ClientCommandTypes.Verification)
public class VerificationCommandHandler extends CommonClientCommandHandler {

    @Autowired
    private Server server;

    @Autowired
    private SessionContext sessionContext;

    @Override
    public boolean isAuthenticatedOnly() {
        return false;
    }

    @Override
    public void handle(ClientCommandData data) {
        ServerMessage message;
        // Checking Client version
        if (data.size() == 0) {
            message = new VersionMessage();
        } else {
            int clientBuild = data.getInt(0);
            if (clientBuild > server.getBuildVersion() || clientBuild < server.getMinSupportedVersion()) {
                message = new VersionMessage();
            } else {
                message = new VersionMessage(server.getBuildVersion(), sessionContext.getSession().getKey());
            }
        }

        sessionContext.getSession().send(message);
    }
}
