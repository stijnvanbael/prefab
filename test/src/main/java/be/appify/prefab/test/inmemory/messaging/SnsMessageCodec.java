package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import be.appify.prefab.core.serialization.ConcreteEventSupport;
import be.appify.prefab.core.sns.SnsMessageEncoder;
import be.appify.prefab.core.sns.SqsDeserializer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Encodes events like the SNS publisher does and wraps them in the notification envelope that SNS delivers to an SQS
 * queue, so subscribers read them with the production {@link SqsDeserializer}.
 * <p>
 * The message payload is the envelope body; the headers are the message attributes, such as version headers.
 */
public class SnsMessageCodec implements MessageCodec {
    private static final String MESSAGE = "Message";
    private static final String SUBJECT = "Subject";
    private static final String MESSAGE_ATTRIBUTES = "MessageAttributes";

    private final SnsMessageEncoder encoder;
    private final SqsDeserializer deserializer;
    private final EventRegistry eventRegistry;
    private final JsonMapper jsonMapper;

    /**
     * Constructs a codec.
     *
     * @param encoder       the encoder shared with the real SNS publisher
     * @param deserializer  the deserializer shared with the real SQS subscriber
     * @param eventRegistry the registry that knows the registered event types
     * @param jsonMapper    the mapper that builds and reads the notification envelope
     */
    public SnsMessageCodec(
            SnsMessageEncoder encoder,
            SqsDeserializer deserializer,
            EventRegistry eventRegistry,
            JsonMapper jsonMapper
    ) {
        this.encoder = encoder;
        this.deserializer = deserializer;
        this.eventRegistry = eventRegistry;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean supports(Class<?> eventType) {
        return eventRegistry.hasTopicForType(eventType)
                && ConcreteEventSupport.usesPlatform(eventType, Event.Platform.SNS_SQS);
    }

    @Override
    public boolean supports(EventType<?> eventType) {
        return !eventRegistry.topicsForType(eventType).isEmpty()
                && ConcreteEventSupport.usesPlatform(eventType, Event.Platform.SNS_SQS);
    }

    @Override
    public EncodedMessage encode(String topic, Object event) {
        var message = encoder.encode(topic, event);
        var attributes = new LinkedHashMap<String, Map<String, String>>();
        message.headers().forEach((name, value) -> attributes.put(name, Map.of("Type", "String", "Value", value)));
        var envelope = new HashMap<String, Object>();
        envelope.put("Type", "Notification");
        envelope.put(MESSAGE, message.payload());
        envelope.put(SUBJECT, message.subject());
        envelope.put(MESSAGE_ATTRIBUTES, attributes);
        var body = jsonMapper.writeValueAsString(envelope);
        return new EncodedMessage(topic, message.headers(), body.getBytes(StandardCharsets.UTF_8), this);
    }

    @Override
    public Object decode(EncodedMessage message) {
        var body = new String(message.payload(), StandardCharsets.UTF_8);
        var identity = message.header(EventType.HEADER);
        if (identity.isPresent()) {
            var type = eventRegistry.concreteTypeFor(message.topic(), identity.get());
            return deserializer.deserializeMatching(message.topic(), body, type).orElseThrow();
        }
        var typeName = jsonMapper.readTree(body).get(SUBJECT).asText();
        var type = eventRegistry.typeByClassName(typeName).orElseThrow(() -> new UnknownEventTypeException(typeName));
        return deserializer.deserialize(message.topic(), body, type);
    }
}
