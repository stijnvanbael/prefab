package be.appify.prefab.asyncapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes AsyncAPI documentation endpoints.
 * <ul>
 *   <li>{@code GET /async-api/asyncapi.json} — the machine-readable AsyncAPI 2.6.0 document</li>
 *   <li>{@code GET /async-api} — a human-readable HTML viewer backed by AsyncAPI Studio</li>
 * </ul>
 *
 * <p>The JSON document is generated at compile time by the Prefab annotation processor and
 * embedded in the application JAR at {@code META-INF/async-api/asyncapi.json}.
 * Spring property placeholders (e.g. {@code ${topics.orders}}) used as topic names in
 * {@code @Event} annotations are resolved at runtime against the application {@link Environment}.
 */
@RestController
@RequestMapping("/async-api")
public class AsyncApiController {

    private static final Logger log = LoggerFactory.getLogger(AsyncApiController.class);
    private static final String ASYNCAPI_JSON_PATH = "META-INF/async-api/asyncapi.json";

    private final Environment environment;

    /**
     * Constructs a new AsyncApiController.
     *
     * @param environment
     *         the Spring environment used to resolve property placeholders in topic names
     */
    public AsyncApiController(Environment environment) {
        this.environment = environment;
    }

    /**
     * Returns the machine-readable AsyncAPI 2.6.0 JSON document with all Spring property
     * placeholders in channel names resolved against the application environment.
     *
     * @return the AsyncAPI JSON document, or {@code 404} if the file was not generated,
     *         or {@code 500} if a placeholder cannot be resolved
     * @throws IOException
     *         if the document cannot be read from the classpath
     */
    @GetMapping(value = "/asyncapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> asyncApiJson() throws IOException {
        var resource = new ClassPathResource(ASYNCAPI_JSON_PATH);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        try (InputStream in = resource.getInputStream()) {
            var raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(resolvePlaceholders(raw));
        }
    }

    private String resolvePlaceholders(String raw) {
        try {
            return environment.resolveRequiredPlaceholders(raw);
        } catch (IllegalArgumentException e) {
            log.error("Failed to resolve Spring property placeholder in asyncapi.json. "
                    + "Ensure all topics configured as property placeholders are defined in the application properties.", e);
            throw e;
        }
    }
}
