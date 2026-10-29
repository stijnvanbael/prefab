package be.appify.prefab.core.versioning;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.generic.GenericRecord;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Converts Avro records to JSON trees so that older Avro messages can be bound to hand-written snapshot types.
 * Logical types are represented by their underlying Avro value.
 */
public final class AvroTrees {
    private AvroTrees() {
    }

    /**
     * Converts the record to a tree.
     *
     * @param record the record
     * @param mapper the mapper that creates the nodes
     * @return the tree
     */
    public static JsonNode toTree(GenericRecord record, JsonMapper mapper) {
        return toNode(record, mapper);
    }

    private static JsonNode toNode(Object value, JsonMapper mapper) {
        return switch (value) {
            case null -> mapper.nullNode();
            case GenericRecord record -> recordNode(record, mapper);
            case Map<?, ?> map -> mapNode(map, mapper);
            case Collection<?> collection -> arrayNode(collection, mapper);
            case ByteBuffer buffer -> mapper.valueToTree(toBytes(buffer));
            case GenericFixed fixed -> mapper.valueToTree(fixed.bytes());
            case GenericEnumSymbol<?> symbol -> mapper.valueToTree(symbol.toString());
            case CharSequence text -> mapper.valueToTree(text.toString());
            default -> mapper.valueToTree(value);
        };
    }

    private static ObjectNode recordNode(GenericRecord record, JsonMapper mapper) {
        var node = mapper.createObjectNode();
        record.getSchema().getFields()
                .forEach(field -> node.set(field.name(), toNode(record.get(field.pos()), mapper)));
        return node;
    }

    private static ObjectNode mapNode(Map<?, ?> map, JsonMapper mapper) {
        var node = mapper.createObjectNode();
        map.forEach((key, value) -> node.set(key.toString(), toNode(value, mapper)));
        return node;
    }

    private static ArrayNode arrayNode(Collection<?> collection, JsonMapper mapper) {
        var node = mapper.createArrayNode();
        collection.forEach(element -> node.add(toNode(element, mapper)));
        return node;
    }

    private static byte[] toBytes(ByteBuffer buffer) {
        var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }
}
