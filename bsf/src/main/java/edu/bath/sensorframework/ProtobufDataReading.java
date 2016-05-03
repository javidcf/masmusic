package edu.bath.sensorframework;

import com.google.protobuf.Message;

/**
 * A data reading encapsulating a protocol buffers message.
 *
 * @author Javier Dehesa
 *
 * @param <E>
 *            Protocol buffers message type.
 */
public class ProtobufDataReading<E extends Message> {

    /** Encapsulated protocol buffers message. */
    private final E message;

    /**
     * Create a data reading with an empty message.
     */
    @SuppressWarnings({ "unchecked", "null" })
    public ProtobufDataReading() {
        E e = null;
        message = (E) e.newBuilderForType().build();
    }

    /**
     * Create a data reading with a message.
     *
     * @param message
     *            The encapsulated message
     */
    public ProtobufDataReading(E message) {
        this.message = message;
    }

    /**
     * Get the encapsulated message.
     *
     * @return The encapsulated message.
     */
    public E getMessage() {
        return message;
    }
}
