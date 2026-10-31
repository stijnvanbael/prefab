package be.appify.prefab.core.spring;

import be.appify.prefab.core.domain.EventType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Utility class for JSON serialization and deserialization. */
@Component
public class JsonUtil {
    private final JsonMapper jsonMapper;

    /**
     * Constructs a new JsonUtil with the given JsonMapper.
     * @param jsonMapper the JsonMapper to use for JSON operations
     */
    public JsonUtil(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Convert an object to its JSON string representation.
     * @param object the object to convert
     * @return JSON string representation of the object
     */
    public String toJson(Object object) {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Parse a JSON string into an object of the specified type.
     * @param json the JSON string to parse
     * @param type the class of the object to return
     * @param <T> the type of the object to return
     * @return the parsed object
     */
    public <T> T parseJson(String json, Class<T> type) {
        try {
            return jsonMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }

    }

    /**
     * Parses JSON while preserving a concrete event's generic arguments.
     *
     * @param json JSON payload
     * @param type concrete target token
     * @param <T> target event type
     * @return the bound payload
     */
    public <T> T parseJson(String json, EventType<T> type) {
        return jsonMapper.readValue(json, jsonMapper.getTypeFactory().constructType(type.type()));
    }
}
