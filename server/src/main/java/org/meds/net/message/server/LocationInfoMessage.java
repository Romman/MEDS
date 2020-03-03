package org.meds.net.message.server;

import org.meds.enums.SpecialLocationTypes;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class LocationInfoMessage implements ServerMessage {

    private final int id;
    private final String title;
    private final int topId;
    private final int bottomId;
    private final int northId;
    private final int southId;
    private final int westId;
    private final int eastId;
    private final int x;
    private final int y;
    private final int z;
    private final SpecialLocationTypes specialLocationType;
    /**
     * All "square-shape" location have this flag
     */
    private final boolean squared;
    private final String regionName;
    private final String kingdomName;
    private final String continentName;
    private final int regionId;
    /**
     * TODO: Determine the source of this value.
     *  The same as in {@link LocationMessage}.
     */
    private final int unk18 = 0;

    public LocationInfoMessage(int id, String title, int topId, int bottomId, int northId, int southId, int westId, int eastId,
                               int x, int y, int z, SpecialLocationTypes specialLocationType, boolean squared,
                               String regionName, String kingdomName, String continentName,
                               int regionId) {
        this.id = id;
        this.title = title;
        this.topId = topId;
        this.bottomId = bottomId;
        this.northId = northId;
        this.southId = southId;
        this.westId = westId;
        this.eastId = eastId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.specialLocationType = specialLocationType;
        this.squared = squared;
        this.regionName = regionName;
        this.kingdomName = kingdomName;
        this.continentName = continentName;
        this.regionId = regionId;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.LocationInfo;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.id);
        stream.writeString(this.title);
        stream.writeInt(this.topId);
        stream.writeInt(this.bottomId);
        stream.writeInt(this.northId);
        stream.writeInt(this.southId);
        stream.writeInt(this.westId);
        stream.writeInt(this.eastId);
        stream.writeInt(this.x);
        stream.writeInt(this.y);
        stream.writeInt(this.z);
        stream.writeInt(this.specialLocationType.getValue());
        stream.writeBoolean(this.squared);
        stream.writeString(this.regionName);
        stream.writeString(this.kingdomName);
        stream.writeString(this.continentName);
        stream.writeInt(this.regionId);
        stream.writeInt(unk18);
    }
}
