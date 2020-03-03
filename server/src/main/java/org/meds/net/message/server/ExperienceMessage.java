package org.meds.net.message.server;

import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class ExperienceMessage implements ServerMessage {

    private final int exp;
    private final int religionExp;

    //
    // Extended message part
    //
    private final boolean extended;
    private final int level;
    /**
     * Always 0
     */
    private final int unk4 = 0;
    private final int religionLevel;

    public ExperienceMessage(int exp, int religionExp) {
        this.exp = exp;
        this.religionExp = religionExp;
        this.extended = false;
        this.level = 0;
        this.religionLevel = 0;
    }

    public ExperienceMessage(int exp, int religionExp, int level, int religionLevel) {
        this.exp = exp;
        this.religionExp = religionExp;
        this.extended = true;
        this.level = level;
        this.religionLevel = religionLevel;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.Experience;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.exp);
        stream.writeInt(this.religionExp);

        if (this.extended) {
            stream.writeInt(level);
            stream.writeInt(unk4);
            stream.writeInt(religionLevel);
        }
    }
}
