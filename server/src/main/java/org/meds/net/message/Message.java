package org.meds.net.message;

/**
 * Represents a message of communication between the server and a client.
 *
 * All implementations must be immutable to ensure thread-safety.
 */
public interface Message {

    MessageIdentity getIdentity();
}
