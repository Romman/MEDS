package org.meds.net.message.server;

import org.meds.Group;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class GroupSettingsMessage implements ServerMessage {

    private final int leaderId;
    private final int minLevel;
    private final int maxLevel;
    private final boolean noReligionAllowed;
    private final boolean sunAllowed;
    private final boolean moonAllowed;
    private final boolean orderAllowed;
    private final boolean chaosAllowed;
    private final Group.ClanAccessModes clanAccessMode;
    private final boolean open;

    public GroupSettingsMessage(int leaderId, int minLevel, int maxLevel, boolean noReligionAllowed,
                                boolean sunAllowed, boolean moonAllowed, boolean orderAllowed,
                                boolean chaosAllowed, Group.ClanAccessModes clanAccessMode, boolean open) {
        this.leaderId = leaderId;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.noReligionAllowed = noReligionAllowed;
        this.sunAllowed = sunAllowed;
        this.moonAllowed = moonAllowed;
        this.orderAllowed = orderAllowed;
        this.chaosAllowed = chaosAllowed;
        this.clanAccessMode = clanAccessMode;
        this.open = open;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.GroupSettings;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.leaderId);
        stream.writeInt(this.minLevel);
        stream.writeInt(this.maxLevel);
        stream.writeBoolean(this.noReligionAllowed);
        stream.writeBoolean(this.sunAllowed);
        stream.writeBoolean(this.moonAllowed);
        stream.writeBoolean(this.orderAllowed);
        stream.writeBoolean(this.chaosAllowed);
        stream.writeObject(this.clanAccessMode);
        stream.writeBoolean(this.open);
    }
}
