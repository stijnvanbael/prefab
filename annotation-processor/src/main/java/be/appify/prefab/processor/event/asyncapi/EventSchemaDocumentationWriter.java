package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.FilerException;
import javax.tools.StandardLocation;

/**
 * Writes AsyncAPI 2.6.0 documentation for all consumed and published events to
 * {@code META-INF/async-api/asyncapi.json}.
 */
class EventSchemaDocumentationWriter {
    private final PrefabContext context;

    EventSchemaDocumentationWriter(PrefabContext context) {
        this.context = context;
    }

    /**
     * Writes the AsyncAPI documentation.
     *
     * @param events
     *         all event types annotated with {@code @Event}
     * @param consumedEventTypes
     *         event types that are consumed (i.e., have an {@code @EventHandler} method)
     */
    void writeDocumentation(List<TypeManifest> events, Set<TypeManifest> consumedEventTypes) {
        // Group events by topic
        Map<String, List<EventInfo>> eventsByTopic = groupByTopic(events, consumedEventTypes);

        // Collect all schemas (events + nested types)
        Map<String, String> schemas = new LinkedHashMap<>();
        Map<String, String> messages = new LinkedHashMap<>();
        for (var entry : eventsByTopic.entrySet()) {
            for (var info : entry.getValue()) {
                collectSchemas(info.type(), schemas);
                messages.put(info.type().simpleName(),
                        buildMessageSchema(info.type()));
            }
        }

        String json = buildAsyncApiJson(eventsByTopic, messages, schemas);
        writeResource(json);
    }

    private Map<String, List<EventInfo>> groupByTopic(List<TypeManifest> events, Set<TypeManifest> consumed) {
        Map<String, List<EventInfo>> result = new LinkedHashMap<>();
        for (var event : events) {
            var annotation = event.inheritedAnnotationsOfType(Event.class).stream().findFirst().orElseThrow();
            var topic = annotation.topic();
            var isConsumed = consumed.contains(event);
            result.computeIfAbsent(topic, k -> new ArrayList<>()).add(new EventInfo(event, isConsumed));
        }
        return result;
    }

    private String buildAsyncApiJson(
            Map<String, List<EventInfo>> eventsByTopic,
            Map<String, String> messages,
            Map<String, String> schemas
    ) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"asyncapi\": \"2.6.0\",\n");
        sb.append("  \"info\": {\n");
        sb.append("    \"title\": \"Application Events\",\n");
        sb.append("    \"version\": \"1.0.0\"\n");
        sb.append("  },\n");

        // Channels
        sb.append("  \"channels\": {\n");
        var topics = new ArrayList<>(eventsByTopic.entrySet());
        for (int i = 0; i < topics.size(); i++) {
            var entry = topics.get(i);
            sb.append("    ").append(jsonString(entry.getKey())).append(": {\n");

            var publishedEvents = entry.getValue().stream().filter(e -> !e.consumed()).toList();
            var consumedEvents = entry.getValue().stream().filter(EventInfo::consumed).toList();

            boolean hasPublish = !publishedEvents.isEmpty();
            boolean hasSubscribe = !consumedEvents.isEmpty();

            if (hasPublish) {
                sb.append("      \"publish\": {\n");
                sb.append("        \"message\": ").append(buildMessageRef(publishedEvents)).append("\n");
                sb.append("      }");
                if (hasSubscribe) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            if (hasSubscribe) {
                sb.append("      \"subscribe\": {\n");
                sb.append("        \"message\": ").append(buildMessageRef(consumedEvents)).append("\n");
                sb.append("      }\n");
            }

            sb.append("    }");
            if (i < topics.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  },\n");

        // Components
        sb.append("  \"components\": {\n");

        // Messages
        sb.append("    \"messages\": {\n");
        var messageEntries = new ArrayList<>(messages.entrySet());
        for (int i = 0; i < messageEntries.size(); i++) {
            var entry = messageEntries.get(i);
            sb.append("      ").append(jsonString(entry.getKey())).append(": ");
            sb.append(entry.getValue());
            if (i < messageEntries.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("    },\n");

        // Schemas
        sb.append("    \"schemas\": {\n");
        var schemaEntries = new ArrayList<>(schemas.entrySet());
        for (int i = 0; i < schemaEntries.size(); i++) {
            var entry = schemaEntries.get(i);
            sb.append("      ").append(jsonString(entry.getKey())).append(": ");
            sb.append(entry.getValue());
            if (i < schemaEntries.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("    }\n");

        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String buildMessageRef(List<EventInfo> events) {
        if (events.size() == 1) {
            return "{\n          \"$ref\": \"#/components/messages/" + events.getFirst().type().simpleName() + "\"\n        }";
        }
        var sb = new StringBuilder();
        sb.append("{\n          \"oneOf\": [\n");
        for (int i = 0; i < events.size(); i++) {
            sb.append("            {\"$ref\": \"#/components/messages/").append(events.get(i).type().simpleName()).append("\"}");
            if (i < events.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("          ]\n        }");
        return sb.toString();
    }

    private String buildMessageSchema(TypeManifest type) {
        return "{\n        \"payload\": {\n          \"$ref\": \"#/components/schemas/" + type.simpleName() + "\"\n        }\n      }";
    }

    private void collectSchemas(TypeManifest type, Map<String, String> schemas) {
        if (schemas.containsKey(type.simpleName())) {
            return;
        }
        // Build schema for this type
        schemas.put(type.simpleName(), buildTypeSchema(type, "      ", schemas));
    }

    private String buildTypeSchema(TypeManifest type, String indent, Map<String, String> schemas) {
        if (type.isSealed()) {
            return buildSealedSchema(type, indent, schemas);
        }
        if (type.isEnum()) {
            return buildEnumSchema(type, indent);
        }
        if (type.isStandardType() || isTemporalType(type) || isSingleValueType(type)) {
            return buildScalarSchema(type, indent);
        }
        return buildObjectSchema(type, indent, schemas);
    }

    private String buildSealedSchema(TypeManifest type, String indent, Map<String, String> schemas) {
        var subtypes = type.permittedSubtypes();
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append(indent).append("  \"oneOf\": [\n");
        for (int i = 0; i < subtypes.size(); i++) {
            var subtype = subtypes.get(i);
            collectSchemas(subtype, schemas);
            sb.append(indent).append("    {\"$ref\": \"#/components/schemas/").append(subtype.simpleName()).append("\"}");
            if (i < subtypes.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(indent).append("  ]\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    private String buildEnumSchema(TypeManifest type, String indent) {
        var values = type.enumValues();
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append(indent).append("  \"type\": \"string\",\n");
        sb.append(indent).append("  \"enum\": [");
        for (int i = 0; i < values.size(); i++) {
            sb.append(jsonString(values.get(i)));
            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    private String buildObjectSchema(TypeManifest type, String indent, Map<String, String> schemas) {
        List<VariableManifest> fields = getFields(type);
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append(indent).append("  \"type\": \"object\",\n");

        // Properties
        sb.append(indent).append("  \"properties\": {\n");
        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);
            sb.append(indent).append("    ").append(jsonString(field.name())).append(": ");
            sb.append(buildFieldSchema(field, indent + "    ", schemas));
            if (i < fields.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(indent).append("  }");

        // Required fields (non-nullable)
        var required = fields.stream()
                .filter(f -> !f.hasAnnotation(Nullable.class))
                .toList();
        if (!required.isEmpty()) {
            sb.append(",\n");
            sb.append(indent).append("  \"required\": [");
            for (int i = 0; i < required.size(); i++) {
                sb.append(jsonString(required.get(i).name()));
                if (i < required.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        sb.append("\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    private List<VariableManifest> getFields(TypeManifest type) {
        if (type.asElement() == null) {
            return List.of();
        }
        return type.fields();
    }

    private String buildFieldSchema(VariableManifest field, String indent, Map<String, String> schemas) {
        var type = field.type();
        if (type.is(List.class)) {
            return buildArraySchema(type, indent, schemas);
        }
        return buildInlineOrRefSchema(type, indent, schemas);
    }

    private String buildArraySchema(TypeManifest listType, String indent, Map<String, String> schemas) {
        var itemType = listType.parameters().getFirst();
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append(indent).append("  \"type\": \"array\",\n");
        sb.append(indent).append("  \"items\": ").append(buildInlineOrRefSchema(itemType, indent + "  ", schemas)).append("\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    private String buildInlineOrRefSchema(TypeManifest type, String indent, Map<String, String> schemas) {
        if (type.isStandardType() || isTemporalType(type) || isSingleValueType(type)) {
            return buildScalarSchema(type, indent);
        }
        if (type.isEnum()) {
            return buildEnumSchema(type, indent);
        }
        // Complex type - use $ref
        collectSchemas(type, schemas);
        return "{\"$ref\": \"#/components/schemas/" + type.simpleName() + "\"}";
    }

    private String buildScalarSchema(TypeManifest type, String indent) {
        if (isSingleValueType(type)) {
            // Delegate to the single field type
            return buildScalarSchema(type.fields().getFirst().type(), indent);
        }
        if (isTemporalType(type)) {
            return buildTemporalSchema(type, indent);
        }
        var sb = new StringBuilder();
        sb.append("{\n");
        var jsonType = toJsonType(type);
        sb.append(indent).append("  \"type\": ").append(jsonString(jsonType)).append("\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    private String buildTemporalSchema(TypeManifest type, String indent) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append(indent).append("  \"type\": \"string\",\n");
        if (type.is(LocalDate.class)) {
            sb.append(indent).append("  \"format\": \"date\"\n");
        } else if (type.is(Duration.class)) {
            sb.append(indent).append("  \"format\": \"duration\"\n");
        } else {
            sb.append(indent).append("  \"format\": \"date-time\"\n");
        }
        sb.append(indent).append("}");
        return sb.toString();
    }

    private static boolean isTemporalType(TypeManifest type) {
        return type.is(Instant.class) || type.is(LocalDate.class)
                || type.is(LocalDateTime.class) || type.is(Duration.class);
    }

    private static boolean isSingleValueType(TypeManifest type) {
        return type.isRecord() && !type.isStandardType() && type.fields().size() == 1;
    }

    private static String toJsonType(TypeManifest type) {
        var simpleName = type.simpleName();
        return switch (simpleName) {
            case "String" -> "string";
            case "int", "Integer", "long", "Long" -> "integer";
            case "float", "Float", "double", "Double" -> "number";
            case "boolean", "Boolean" -> "boolean";
            default -> "string";
        };
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void writeResource(String content) {
        try {
            var resource = context.processingEnvironment().getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "META-INF/async-api/asyncapi.json"
            );
            try (var writer = resource.openWriter()) {
                writer.write(content);
            }
        } catch (FilerException e) {
            // File already written in a previous processing round; skip.
        } catch (IOException e) {
            throw new RuntimeException("Failed to write asyncapi.json", e);
        }
    }

    private record EventInfo(TypeManifest type, boolean consumed) {
    }
}
