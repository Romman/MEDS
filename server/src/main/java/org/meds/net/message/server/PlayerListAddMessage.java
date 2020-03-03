package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class PlayerListAddMessage implements ServerMessage {

    private final PlayerOnlineInfo player;

    public PlayerListAddMessage(PlayerOnlineInfo player) {
        this.player = player;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.PlayersListAdd;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        this.player.serialize(stream);
    }
}
