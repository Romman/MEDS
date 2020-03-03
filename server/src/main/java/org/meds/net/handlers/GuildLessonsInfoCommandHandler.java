package org.meds.net.handlers;

import org.meds.database.DataStorage;
import org.meds.net.ClientCommandData;
import org.meds.net.ClientCommandTypes;
import org.meds.net.SessionContext;
import org.meds.net.message.ServerMessage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
@ClientCommand(ClientCommandTypes.GuildLessonsInfo)
public class GuildLessonsInfoCommandHandler extends CommonClientCommandHandler {

    @Autowired
    private SessionContext sessionContext;
    @Autowired
    private DataStorage dataStorage;

    @Override
    public int getMinDataLength() {
        return 1;
    }

    @Override
    public void handle(ClientCommandData data) {
        ServerMessage lessonMessage = dataStorage.getGuildLessonInfo(data.getInt(0, -1));
        if (lessonMessage != null) {
            sessionContext.getSession().send(lessonMessage);
        }
    }
}
