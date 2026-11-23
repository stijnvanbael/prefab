package be.appify.prefab.streams.postgres;

import be.appify.prefab.streams.StreamKeyEncoding;
import be.appify.prefab.streams.engine.StreamMessage;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Default codec: JSON with the concrete class name next to each value, so values are restored with their original
 * type. Keys use {@link StreamKeyEncoding}.
 */
public final class JsonStreamStateCodec implements StreamStateCodec {
    private static final String TYPE = "type";
    private static final String VALUE = "value";
    private static final String ENCODING = "encoding";
    private static final String NESTED_TYPES = "nested-types-v1";

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    private final JsonMapper stateMapper = JsonMapper.builder().findAndAddModules()
            .activateDefaultTypingAsProperty(
                    BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                    DefaultTyping.OBJECT_AND_NON_CONCRETE, "@prefab-type")
            .build();
    private final Map<Class<?>, StreamKeyEncoding<?>> keyEncodings = new ConcurrentHashMap<>();

    @Override
    public byte[] encodeValue(Object value) {
        return mapper.writeValueAsBytes(typed(value));
    }

    @Override
    public Object decodeValue(byte[] bytes) {
        return untyped(mapper.readTree(bytes));
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] encodeKey(Object key) {
        var encoding = (StreamKeyEncoding<Object>) keyEncodings.computeIfAbsent(
                key.getClass(), type -> new StreamKeyEncoding<>(type));
        return encoding.encode(key);
    }

    @Override
    public byte[] encodeMessage(StreamMessage message) {
        var node = mapper.createObjectNode();
        node.put("messageId", message.messageId());
        node.set("key", typed(message.key()));
        node.set("value", typed(message.value()));
        node.set("headers", mapper.valueToTree(new LinkedHashMap<>(message.headers())));
        node.put("eventTime", message.eventTime().toString());
        return mapper.writeValueAsBytes(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StreamMessage decodeMessage(byte[] bytes) {
        var node = mapper.readTree(bytes);
        return new StreamMessage(
                node.get("messageId").asString(),
                untyped(node.get("key")),
                untyped(node.get("value")),
                mapper.treeToValue(node.get("headers"), Map.class),
                Instant.parse(node.get("eventTime").asString()));
    }

    private JsonNode typed(Object value) {
        if (value == null) {
            return mapper.nullNode();
        }
        ObjectNode node = mapper.createObjectNode();
        node.put(TYPE, value.getClass().getName());
        node.put(ENCODING, NESTED_TYPES);
        node.set(VALUE, stateMapper.valueToTree(value));
        return node;
    }

    private Object untyped(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        var typeName = node.get(TYPE).asString();
        var encoding = node.path(ENCODING).asString();
        if (!encoding.isEmpty() && !NESTED_TYPES.equals(encoding)) {
            throw new IllegalStateException("Unknown stream state encoding " + encoding);
        }
        try {
            var type = Class.forName(typeName, true, Thread.currentThread().getContextClassLoader());
            return NESTED_TYPES.equals(encoding)
                    ? stateMapper.treeToValue(node.get(VALUE), type)
                    : mapper.treeToValue(node.get(VALUE), type);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Stored stream state has unknown type " + typeName, e);
        }
    }
}
