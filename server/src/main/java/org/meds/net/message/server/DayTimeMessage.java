package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class DayTimeMessage implements ServerMessage {

    private final boolean isNight;
    /**
     * Current time of a day(or a night) in seconds
     */
    private final int time;

    public DayTimeMessage(boolean isNight, int time) {
        this.isNight = isNight;
        this.time = time;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.DayTime;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeBoolean(this.isNight);
        stream.writeInt(this.time);
    }
}
