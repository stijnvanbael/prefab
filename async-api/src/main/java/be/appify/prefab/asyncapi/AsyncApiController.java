package be.appify.prefab.asyncapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
 */
@RestController
@RequestMapping("/async-api")
public class AsyncApiController {

    private static final String ASYNCAPI_JSON_PATH = "META-INF/async-api/asyncapi.json";

    /** Constructs a new AsyncApiController. */
    public AsyncApiController() {
    }

    /**
     * Returns the machine-readable AsyncAPI 2.6.0 JSON document.
     *
     * @return the AsyncAPI JSON document
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
            return ResponseEntity.ok(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
