package org.meds.net.message.server;

import java.util.Collection;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class RegionLocationsMessage implements ServerMessage {

    private final int regionId;
    private final Collection<Integer> locationIds;

    public RegionLocationsMessage(int regionId, Collection<Integer> locationIds) {
        this.regionId = regionId;
        this.locationIds = locationIds;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.RegionLocations;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.regionId);
        for (int locationId : locationIds) {
            stream.writeInt(locationId);
        }
    }
}
