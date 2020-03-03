package org.meds.net.message.server;

import org.meds.enums.LoginResults;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;

public class LoginResultMessage implements ServerMessage {

    private final LoginResults result;

    public LoginResultMessage(LoginResults result) {
        this.result = result;
    }

    @Override
    public MessageIdentity getIdentity() {
        return ServerMessageIdentity.LoginResult;
    }

    @Override
    public void serialize(MessageWriteStream stream) {
        stream.writeInt(this.result.getValue());
    }
}
