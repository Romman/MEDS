package org.meds.net.handlers;

import org.meds.Locale;
import org.meds.net.*;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.LearnGuildInfoMessage;
import org.meds.player.LevelCost;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
@ClientCommand(ClientCommandTypes.LearnGuildInfo)
public class LearnGuildInfoCommandHandler extends CommonClientCommandHandler {

    @Autowired
    private SessionContext sessionContext;
    @Autowired
    private Locale locale;
    @Autowired
    private LevelCost levelCost;

    @Override
    public void handle(ClientCommandData data) {
        int availableCount = sessionContext.getPlayer().getLevel() - sessionContext.getPlayer().getGuildLevel();

        ServerMessage message = new LearnGuildInfoMessage(
                availableCount,
                -sessionContext.getPlayer().getGuildLevel(),
                availableCount,
                levelCost.getLevelGold(sessionContext.getPlayer().getGuildLevel() + 1),
                levelCost.getTotalGold(sessionContext.getPlayer().getGuildLevel() + 1, sessionContext.getPlayer().getLevel() + 1),
                locale.getString(3)
        );

        sessionContext.getSession().send(message);
    }
}
