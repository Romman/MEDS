package org.meds.net.message;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * An implementation of MessageWriteStream based on "string-delimited" format of data.
 */
public class StringDelimitedMessageWriteStream implements MessageWriteStream {

    public static final char MESSAGE_DELIMITER = '\u0000';
    public static final char VALUE_DELIMITER = '\u0001';

    private enum StreamState {
        Value,
        InterValue,
        InterMessage,
    }

    private StreamState state;
    private StringBuilder data;

    public StringDelimitedMessageWriteStream() {
        this.data = new StringBuilder();
        this.state = StreamState.InterMessage;
    }

    public void newMessage(MessageIdentity identity) {
        if (this.state != StreamState.InterMessage) {
            this.data.append(MESSAGE_DELIMITER);
        }
        this.data.append(identity.identity()).append(VALUE_DELIMITER);
        this.state = StreamState.InterValue;
    }

    @Override
    public void writeInt(int value) {
        writeString(Integer.toString(value));
    }

    @Override
    public void writeBoolean(boolean value) {
        writeString(value ? "1" : "0");
    }

    @Override
    public void writeString(String str) {
        if (this.state == StreamState.Value) {
            this.data.append(VALUE_DELIMITER);
        }
        this.data.append(str);
        this.state = StreamState.Value;
    }

    @Override
    public void writeObject(Object object) {
        // NULL values should be replaced with the empty value
        if (object == null) {
            writeString("");
        } else {
            writeString(object.toString());
        }
    }

    public boolean isEmpty() {
        return this.data.length() == 0;
    }

    @Override
    public String toString() {
        if (this.state != StreamState.InterMessage) {
            this.data.append(MESSAGE_DELIMITER);
        }
        return this.data.toString();
    }

    public byte[] getBytes() {
        String string = this.toString();

        byte[] bytes;
        try {
            bytes = string.getBytes("Unicode");
            bytes = Arrays.copyOfRange(bytes, 2, bytes.length);
        } catch (UnsupportedEncodingException e) {
            bytes = new byte[0];
        }
        return bytes;
    }
}
