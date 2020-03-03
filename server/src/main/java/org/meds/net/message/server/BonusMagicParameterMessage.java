package org.meds.net.message.server;

import org.meds.enums.Parameters;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class BonusMagicParameterMessage implements ServerMessage {

    private final Parameters parameter;
    private final int value;

    public BonusMagicParameterMessage(Parameters parameter, int value) {
        this.parameter = parameter;
        this.value = value;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.BonusMagicParameter;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.parameter.getValue());
        stream.writeInt(this.value);
    }
}
