package org.meds.net.message.server;

import org.meds.enums.BattleStates;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class BattleMessage implements ServerMessage {

    private final BattleStates state;

    public BattleMessage(BattleStates state) {
        this.state = state;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.BattleState;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.state.getValue());
    }
}
