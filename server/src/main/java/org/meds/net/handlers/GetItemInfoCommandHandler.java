package org.meds.net.handlers;

import java.util.ArrayList;
import java.util.List;
import org.meds.item.ItemMessagePacketFactory;
import org.meds.net.ClientCommandData;
import org.meds.net.ClientCommandTypes;
import org.meds.net.SessionContext;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.server.ItemMessage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
@ClientCommand(ClientCommandTypes.GetItemInfo)
public class GetItemInfoCommandHandler extends CommonClientCommandHandler {

    @Autowired
    private SessionContext sessionContext;
    @Autowired
    private ItemMessagePacketFactory itemMessagePacketFactory;

    @Override
    public void handle(ClientCommandData data) {
        List<ServerMessage> messages = new ArrayList<>();
        for (int i = 1; i < data.size(); i += 2) {
            int templateId = data.getInt(i - 1);
            int modification = data.getInt(i);
            ItemMessage message = itemMessagePacketFactory.create(templateId, modification);
            if (message != null) {
                messages.add(message);
            }
        }
        sessionContext.getSession().send(messages);
    }
}
