package org.meds.net.message;

public interface ServerMessage extends Message {

    void serialize(MessageWriteStream stream);
}
