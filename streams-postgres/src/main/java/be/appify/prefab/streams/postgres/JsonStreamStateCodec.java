package be.appify.prefab.streams.postgres;

import be.appify.prefab.streams.StreamKeyEncoding;
import be.appify.prefab.streams.engine.StreamMessage;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Default codec: JSON with the concrete class name next to each value, so values are restored with their original
 * type. Keys use {@link StreamKeyEncoding}.
 */
public final class JsonStreamStateCodec implements StreamStateCodec {
    private static final String TYPE = "type";
    private static final String VALUE = "value";

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
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
        node.set(VALUE, mapper.valueToTree(value));
        return node;
    }

    private Object untyped(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        var typeName = node.get(TYPE).asString();
        try {
            var type = Class.forName(typeName, true, Thread.currentThread().getContextClassLoader());
            return mapper.treeToValue(node.get(VALUE), type);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Stored stream state has unknown type " + typeName, e);
        }
    }
}
