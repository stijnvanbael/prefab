package be.appify.prefab.test.inmemory.messaging;

import java.util.Map;
import java.util.Optional;

/**
 * A message as it travels over the in-memory bus: the serialized payload plus its transport headers.
 * <p>
 * Every subscriber decodes its own copy through {@link #decode()}, exactly like consumers of a real broker.
 *
 * @param topic   the topic the message was published on
 * @param headers the transport headers, such as version and event type headers
 * @param payload the serialized event
 * @param codec   the codec that encoded the message and decodes it again
 */
public record EncodedMessage(String topic, Map<String, String> headers, byte[] payload, MessageCodec codec) {

    /**
     * Constructs a message, copying the headers.
     *
     * @param topic   the topic the message was published on
     * @param headers the transport headers
     * @param payload the serialized event
     * @param codec   the codec that encoded the message
     */
    public EncodedMessage {
        headers = Map.copyOf(headers);
    }

    /**
     * Decodes a fresh copy of the event.
     *
     * @return the decoded event
     */
    public Object decode() {
        return codec.decode(this);
    }

    /**
     * Looks up a transport header.
     *
     * @param name the header name
     * @return the header value, if present
     */
    public Optional<String> header(String name) {
        return Optional.ofNullable(headers.get(name));
    }
}
