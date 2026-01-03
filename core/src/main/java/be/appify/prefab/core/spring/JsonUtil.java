package be.appify.prefab.core.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Utility class for JSON serialization and deserialization. */
@Component
public class JsonUtil {
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new JsonUtil with the given ObjectMapper.
     * @param objectMapper the ObjectMapper to use for JSON operations
     */
    public JsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convert an object to its JSON string representation.
     * @param object the object to convert
     * @return JSON string representation of the object
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
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
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
