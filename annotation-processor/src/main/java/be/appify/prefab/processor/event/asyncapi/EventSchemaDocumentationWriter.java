package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;

/**
 * Writes an AsyncAPI 2.6.0 JSON documentation file describing all events in the application.
 * The file is written to {@code META-INF/async-api/asyncapi.json} in the class output directory.
 */
class EventSchemaDocumentationWriter {
    private final ProcessingEnvironment processingEnvironment;

    EventSchemaDocumentationWriter(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    void writeDocumentation(List<TypeManifest> events) {
        var document = buildDocument(events);
        try {
            var resource = processingEnvironment.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "META-INF/async-api/asyncapi.json"
            );
            try (var writer = resource.openWriter()) {
                writer.write(toJson(document, 0));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> buildDocument(List<TypeManifest> events) {
        var channels = new LinkedHashMap<String, Object>();
        var messages = new LinkedHashMap<String, Object>();
        var schemas = new LinkedHashMap<String, Object>();

        for (var event : events) {
            var eventAnnotation = event.inheritedAnnotationsOfType(Event.class).stream().findFirst().orElseThrow();
            var topic = eventAnnotation.topic();
            var contentType = eventAnnotation.serialization() == Event.Serialization.AVRO
                    ? "application/avro"
                    : "application/json";

            if (event.isSealed()) {
                var subtypeRefs = new ArrayList<Map<String, Object>>();
                for (var subtype : event.permittedSubtypes()) {
                    var schemaName = schemaName(subtype);
                    subtypeRefs.add(Map.of("$ref", "#/components/messages/" + schemaName));
                    messages.put(schemaName, buildMessage(schemaName, contentType));
                    addSchemas(schemas, subtype);
                }
                channels.put(topic, buildChannel(linkedMap("oneOf", subtypeRefs)));
            } else {
                var schemaName = schemaName(event);
                channels.put(topic, buildChannel(linkedMap("$ref", "#/components/messages/" + schemaName)));
                messages.put(schemaName, buildMessage(schemaName, contentType));
                addSchemas(schemas, event);
            }
        }

        var components = new LinkedHashMap<String, Object>();
        if (!messages.isEmpty()) {
            components.put("messages", messages);
        }
        if (!schemas.isEmpty()) {
            components.put("schemas", schemas);
        }

        var info = new LinkedHashMap<String, Object>();
        info.put("title", "Events");
        info.put("version", "1.0.0");

        var doc = new LinkedHashMap<String, Object>();
        doc.put("asyncapi", "2.6.0");
        doc.put("info", info);
        if (!channels.isEmpty()) {
            doc.put("channels", channels);
        }
        if (!components.isEmpty()) {
            doc.put("components", components);
        }
        return doc;
    }

    private static Map<String, Object> buildChannel(Object messageRef) {
        return linkedMap("publish", linkedMap("message", messageRef));
    }

    private Map<String, Object> buildMessage(String name, String contentType) {
        var message = new LinkedHashMap<String, Object>();
        message.put("name", name);
        message.put("contentType", contentType);
        message.put("payload", linkedMap("$ref", "#/components/schemas/" + name));
        return message;
    }

    private void addSchemas(Map<String, Object> schemas, TypeManifest type) {
        schemas.put(schemaName(type), buildSchema(type));
        for (var field : type.fields()) {
            if (isNestedRecord(field.type())) {
                addSchemas(schemas, field.type());
            } else if (field.type().is(List.class) && isNestedRecord(field.type().parameters().getFirst())) {
                addSchemas(schemas, field.type().parameters().getFirst());
            }
        }
    }

    private Map<String, Object> buildSchema(TypeManifest type) {
        var properties = new LinkedHashMap<String, Object>();
        for (var field : type.fields()) {
            properties.put(field.name(), fieldSchema(field));
        }
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> fieldSchema(VariableManifest field) {
        var schema = typeSchema(field.type());
        if (field.nullable()) {
            var withNullable = new LinkedHashMap<>(schema);
            withNullable.put("nullable", true);
            return withNullable;
        }
        return schema;
    }

    private Map<String, Object> typeSchema(TypeManifest type) {
        if (type.is(List.class)) {
            var itemType = type.parameters().getFirst();
            return linkedMap("type", "array", "items", typeSchema(itemType));
        }
        if (type.isEnum()) {
            return linkedMap("type", "string", "enum", type.enumValues());
        }
        if (type.is(String.class)) {
            return linkedMap("type", "string");
        }
        if (type.is(Instant.class)) {
            return linkedMap("type", "string", "format", "date-time");
        }
        if (type.is(LocalDate.class)) {
            return linkedMap("type", "string", "format", "date");
        }
        if (type.is(Duration.class)) {
            return linkedMap("type", "string", "format", "duration");
        }
        if (type.isSingleValueType()) {
            // Reference<T> and similar single-value wrapper types
            return linkedMap("type", "string");
        }
        // Handle primitive and boxed numeric/boolean types by simple name
        return switch (type.simpleName()) {
            case "int", "Integer" -> linkedMap("type", "integer", "format", "int32");
            case "long", "Long" -> linkedMap("type", "integer", "format", "int64");
            case "double", "Double" -> linkedMap("type", "number", "format", "double");
            case "float", "Float" -> linkedMap("type", "number", "format", "float");
            case "boolean", "Boolean" -> linkedMap("type", "boolean");
            default -> isNestedRecord(type)
                    ? linkedMap("$ref", "#/components/schemas/" + schemaName(type))
                    : linkedMap("type", "object");
        };
    }

    private static boolean isNestedRecord(TypeManifest type) {
        return !type.isStandardType() && !type.isEnum()
                && !type.is(Instant.class) && !type.is(LocalDate.class)
                && !type.is(Duration.class) && !type.isSingleValueType()
                && !type.is(List.class);
    }

    private static String schemaName(TypeManifest type) {
        return type.simpleName().replace('.', '_');
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object value, int indent) {
        var indentStr = "  ".repeat(indent);
        var innerIndent = "  ".repeat(indent + 1);
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                return "{}";
            }
            var sb = new StringBuilder("{\n");
            var entries = new ArrayList<>(((Map<String, Object>) map).entrySet());
            for (int i = 0; i < entries.size(); i++) {
                var entry = entries.get(i);
                sb.append(innerIndent)
                        .append(jsonString(entry.getKey().toString()))
                        .append(": ")
                        .append(toJson(entry.getValue(), indent + 1));
                if (i < entries.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append(indentStr).append("}");
            return sb.toString();
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                return "[]";
            }
            var sb = new StringBuilder("[\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append(innerIndent).append(toJson(list.get(i), indent + 1));
                if (i < list.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append(indentStr).append("]");
            return sb.toString();
        } else if (value instanceof String s) {
            return jsonString(s);
        } else if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        return jsonString(String.valueOf(value));
    }

    private static String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private static LinkedHashMap<String, Object> linkedMap(String key, Object value) {
        var map = new LinkedHashMap<String, Object>();
        map.put(key, value);
        return map;
    }

    private static LinkedHashMap<String, Object> linkedMap(String k1, Object v1, String k2, Object v2) {
        var map = new LinkedHashMap<String, Object>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
}
