package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class PlayersLocationMessage implements ServerMessage {

    private final Collection<PlayerLocationMessage> playerLocations;

    public PlayersLocationMessage(Collection<PlayerLocationMessage> playerLocations) {
        this.playerLocations = playerLocations;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.playerLocations.size());
        for (PlayerLocationMessage playerLocation : this.playerLocations) {
            playerLocation.serialize(stream);
        }
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.PlayersLocation;
    }
}
