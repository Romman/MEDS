package org.meds.net.message.server;

import org.meds.enums.SpecialLocationTypes;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class LocationMessage implements ServerMessage {

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
    /**
     * Continent ID should be here
     * TODO: Implement continents
     */
    private final String continentName = "Continent";
    private final String kingdomName;
    private final String regionName;
    private final SpecialLocationTypes specialLocationType;
    /**
     * There are 2 possible values as a string - "true" and "false"
     */
    private final boolean safeZone;
    private final int keeperType;
    private final String keeperName;
    private final int specialLocationId;
    private final int pictureId;
    private final boolean squared;
    private final boolean safeRegion;
    private final int pictureTime;
    private final int keeperTime;
    private final int regionId;
    /**
     * Looks like the item id as a key to get into this location.
     *  The same as in {@link LocationInfoMessage}
     */
    private final int unk26 = 0;

    public LocationMessage(int id, String title, int topId, int bottomId, int northId, int southId, int westId, int eastId,
                           int x, int y, int z, String kingdomName, String regionName,
                           SpecialLocationTypes specialLocationType, boolean safeZone, int keeperType,
                           String keeperName, int specialLocationId, int pictureId, boolean squared,
                           boolean safeRegion, int pictureTime, int keeperTime, int regionId) {
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
        this.kingdomName = kingdomName;
        this.regionName = regionName;
        this.specialLocationType = specialLocationType;
        this.safeZone = safeZone;
        this.keeperType = keeperType;
        this.keeperName = keeperName;
        this.specialLocationId = specialLocationId;
        this.pictureId = pictureId;
        this.squared = squared;
        this.safeRegion = safeRegion;
        this.pictureTime = pictureTime;
        this.keeperTime = keeperTime;
        this.regionId = regionId;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.Location;
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
        stream.writeString(continentName);
        stream.writeString(kingdomName);
        stream.writeString(regionName);
        stream.writeInt(this.specialLocationType.getValue());
        stream.writeString(this.safeZone ? "true" : "false");
        stream.writeInt(this.keeperType);
        stream.writeString(this.keeperName);
        stream.writeInt(this.specialLocationId);
        stream.writeInt(this.pictureId);
        stream.writeBoolean(this.squared);
        stream.writeBoolean(this.safeRegion);
        stream.writeInt(this.pictureTime);
        stream.writeInt(this.keeperTime);
        stream.writeInt(this.regionId);
        stream.writeInt(unk26);
    }
}
