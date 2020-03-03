package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class PlayersOnlineMessage implements ServerMessage {

    private final Collection<PlayerOnlineInfo> players;

    public PlayersOnlineMessage(Collection<PlayerOnlineInfo> players) {
        this.players = players;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.OnlineList;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.players.size());
        for (PlayerOnlineInfo player : this.players) {
            player.serialize(stream);
        }
    }
}
