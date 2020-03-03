package org.meds.net.message;

public interface MessageWriteStream {

    void writeInt(int value);

    void writeBoolean(boolean value);

    void writeString(String str);

    void writeObject(Object object);
}
