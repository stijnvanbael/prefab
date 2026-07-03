package be.appify.prefab.processor.event.asyncapi;

import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private final ObjectMapper objectMapper;
    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    EventSchemaDocumentationWriter(PrefabContext context) {
        this.context = context;
        this.objectMapper = configureObjectMapper();
    }

    private static ObjectMapper configureObjectMapper() {
        var mapper = new ObjectMapper();
        var printer = new DefaultPrettyPrinter();
        var indenter = new DefaultIndenter("  ", "\n");
        printer.indentObjectsWith(indenter);
        // Don't indent arrays; keep them compact
        printer = printer.withArrayIndenter(new DefaultIndenter());
        mapper.setDefaultPrettyPrinter(printer);
        return mapper;
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
        var eventsByTopic = groupByTopic(events, consumedEventTypes);

        Map<String, ObjectNode> schemas = new LinkedHashMap<>();
        Map<String, ObjectNode> messages = new LinkedHashMap<>();
        eventsByTopic.values().stream()
                .flatMap(List::stream)
                .forEach(info -> {
                    collectSchemas(info.type(), schemas);
                    messages.put(info.type().simpleName(), buildMessageSchema(info.type()));
                });

        var asyncApiJson = buildAsyncApiJson(eventsByTopic, messages, schemas);
        writeResource(asyncApiJson);
    }

    private ObjectNode buildAsyncApiJson(
            Map<String, List<EventInfo>> eventsByTopic,
            Map<String, ObjectNode> messages,
            Map<String, ObjectNode> schemas
    ) {
        var root = factory.objectNode();
        root.put("asyncapi", "2.6.0");
        
        var info = factory.objectNode();
        info.put("title", "Application Events");
        info.put("version", "1.0.0");
        root.set("info", info);
        
        var channels = factory.objectNode();
        for (var entry : eventsByTopic.entrySet()) {
            channels.set(entry.getKey(), buildChannelNode(entry.getValue()));
        }
        root.set("channels", channels);
        
        var components = factory.objectNode();
        var messagesNode = factory.objectNode();
        for (var entry : messages.entrySet()) {
            messagesNode.set(entry.getKey(), entry.getValue());
        }
        components.set("messages", messagesNode);
        
        var schemasNode = factory.objectNode();
        for (var entry : schemas.entrySet()) {
            schemasNode.set(entry.getKey(), entry.getValue());
        }
        components.set("schemas", schemasNode);
        
        root.set("components", components);
        return root;
    }

    private Map<String, List<EventInfo>> groupByTopic(List<TypeManifest> events, Set<TypeManifest> consumed) {
        Map<String, List<EventInfo>> result = new LinkedHashMap<>();
        for (var event : events) {
            var annotation = event.inheritedAnnotationsOfType(Event.class).stream().findFirst().orElseThrow();
            var isConsumed = consumed.contains(event);
            for (var topic : annotation.topic()) {
                result.computeIfAbsent(topic, k -> new ArrayList<>()).add(new EventInfo(event, isConsumed));
            }
        }
        return result;
    }

    private ObjectNode buildMessageSchema(TypeManifest type) {
        var message = factory.objectNode();
        var payload = factory.objectNode();
        payload.put("$ref", "#/components/schemas/" + type.simpleName());
        message.set("payload", payload);
        return message;
    }

    private ObjectNode buildChannelNode(List<EventInfo> events) {
        var channel = factory.objectNode();
        var publishedEvents = events.stream().filter(e -> !e.consumed()).toList();
        var consumedEvents = events.stream().filter(EventInfo::consumed).toList();

        if (!publishedEvents.isEmpty()) {
            var publish = factory.objectNode();
            publish.set("message", buildMessageRefNode(publishedEvents));
            channel.set("publish", publish);
        }
        if (!consumedEvents.isEmpty()) {
            var subscribe = factory.objectNode();
            subscribe.set("message", buildMessageRefNode(consumedEvents));
            channel.set("subscribe", subscribe);
        }
        return channel;
    }

    private ObjectNode buildMessageRefNode(List<EventInfo> events) {
        var messageRef = factory.objectNode();
        if (events.size() == 1) {
            messageRef.put("$ref", "#/components/messages/" + events.getFirst().type().simpleName());
        } else {
            var oneOf = factory.arrayNode();
            for (var event : events) {
                var ref = factory.objectNode();
                ref.put("$ref", "#/components/messages/" + event.type().simpleName());
                oneOf.add(ref);
            }
            messageRef.set("oneOf", oneOf);
        }
        return messageRef;
    }

    private void collectSchemas(TypeManifest type, Map<String, ObjectNode> schemas) {
        if (schemas.containsKey(type.simpleName())) {
            return;
        }
        schemas.put(type.simpleName(), buildTypeSchema(type, schemas));
    }

    private ObjectNode buildTypeSchema(TypeManifest type, Map<String, ObjectNode> schemas) {
        if (type.isSealed()) {
            return buildSealedSchema(type, schemas);
        }
        if (type.isEnum()) {
            return buildEnumSchema(type);
        }
        if (type.isStandardType() || isTemporalType(type) || isSingleValueType(type)) {
            return buildScalarSchema(type);
        }
        return buildObjectSchema(type, schemas);
    }

    private ObjectNode buildSealedSchema(TypeManifest type, Map<String, ObjectNode> schemas) {
        var schema = factory.objectNode();
        var subtypes = type.permittedSubtypes();
        var oneOf = factory.arrayNode();
        
        for (var subtype : subtypes) {
            collectSchemas(subtype, schemas);
            var ref = factory.objectNode();
            ref.put("$ref", "#/components/schemas/" + subtype.simpleName());
            oneOf.add(ref);
        }
        schema.set("oneOf", oneOf);
        return schema;
    }

    private ObjectNode buildEnumSchema(TypeManifest type) {
        var schema = factory.objectNode();
        type.doc().ifPresent(d -> schema.put("description", d));
        schema.put("type", "string");
        
        var enumValues = factory.arrayNode();
        for (var value : type.enumValues()) {
            enumValues.add(value);
        }
        schema.set("enum", enumValues);
        return schema;
    }

    private ObjectNode buildObjectSchema(TypeManifest type, Map<String, ObjectNode> schemas) {
        if (type.asElement() == null) {
            return factory.objectNode();
        }
        
        var schema = factory.objectNode();
        type.doc().ifPresent(d -> schema.put("description", d));
        schema.put("type", "object");
        
        var fields = type.fields();
        var properties = factory.objectNode();
        
        for (var field : fields) {
            properties.set(field.name(), buildFieldSchema(field, schemas));
        }
        schema.set("properties", properties);
        
        var required = fields.stream()
                .filter(f -> !f.hasAnnotation(Nullable.class))
                .map(VariableManifest::name)
                .toList();
        
        if (!required.isEmpty()) {
            var requiredArray = factory.arrayNode();
            required.forEach(requiredArray::add);
            schema.set("required", requiredArray);
        }
        
        return schema;
    }

    private ObjectNode buildFieldSchema(VariableManifest field, Map<String, ObjectNode> schemas) {
        var type = field.type();
        
        ObjectNode schema;
        if (type.is(List.class)) {
            schema = factory.objectNode();
            schema.put("type", "array");
            var itemType = type.parameters().getFirst();
            schema.set("items", buildInlineOrRefSchema(itemType, schemas));
        } else {
            schema = buildInlineOrRefSchema(type, schemas);
        }
        
        field.getAnnotation(Example.class)
                .ifPresent(ann -> schema.put("example", ann.value().value()));
        field.getAnnotation(Doc.class)
                .ifPresent(ann -> schema.put("description", ann.value().value()));
        
        return schema;
    }

    private ObjectNode buildInlineOrRefSchema(TypeManifest type, Map<String, ObjectNode> schemas) {
        if (type.isStandardType() || isTemporalType(type) || isSingleValueType(type)) {
            return buildScalarSchema(type);
        }
        if (type.isEnum()) {
            return buildEnumSchema(type);
        }
        collectSchemas(type, schemas);
        var ref = factory.objectNode();
        ref.put("$ref", "#/components/schemas/" + type.simpleName());
        return ref;
    }

    private ObjectNode buildScalarSchema(TypeManifest type) {
        if (isSingleValueType(type)) {
            var innerSchema = buildScalarSchema(type.fields().getFirst().type());
            type.doc().ifPresent(d -> innerSchema.put("description", d));
            return innerSchema;
        }
        if (isTemporalType(type)) {
            return buildTemporalSchema(type);
        }
        var schema = factory.objectNode();
        schema.put("type", toJsonType(type));
        return schema;
    }

    private ObjectNode buildTemporalSchema(TypeManifest type) {
        var schema = factory.objectNode();
        schema.put("type", "string");
        
        if (type.is(LocalDate.class)) {
            schema.put("format", "date");
        } else if (type.is(Duration.class)) {
            schema.put("format", "duration");
        } else {
            schema.put("format", "date-time");
        }
        return schema;
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

    private void writeResource(ObjectNode asyncApiJson) {
        try {
            var resource = context.processingEnvironment().getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "META-INF/async-api/asyncapi.json"
            );
            try (var writer = resource.openWriter()) {
                var jsonString = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(asyncApiJson);
                // Remove spaces around colons that Jackson adds for compact formatting
                jsonString = jsonString.replaceAll("\"\\s*:\\s*", "\": ");
                writer.write(jsonString);
            }
        } catch (FilerException e) {
            // File already written in a previous processing round; skip.
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Failed to write asyncapi.json", e);
        }
    }

    private record EventInfo(TypeManifest type, boolean consumed) {
    }
}
