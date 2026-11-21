package benchmark.support;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP helper for the benchmark's hidden acceptance tests. Acceptance tests talk to the application over HTTP only,
 * so they do not depend on how the agent named Java parameters or methods, just on the API contract in the prompt.
 */
public final class Api {
    private final MockMvc mockMvc;
    private final List<RequestPostProcessor> postProcessors;

    public Api(MockMvc mockMvc) {
        this(mockMvc, List.of());
    }

    private Api(MockMvc mockMvc, List<RequestPostProcessor> postProcessors) {
        this.mockMvc = mockMvc;
        this.postProcessors = List.copyOf(postProcessors);
    }

    /** Applies request post-processors (authentication, headers) to every request. */
    public Api with(RequestPostProcessor... processors) {
        return new Api(mockMvc, List.of(processors));
    }

    /** POSTs JSON, expects 201 Created and returns the id from the Location header. */
    public String create(String path, String json, Object... uriVariables) throws Exception {
        var location = post(path, json, uriVariables)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        if (location == null) {
            throw new AssertionError("POST " + path + " returned no Location header");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    public ResultActions post(String path, String json, Object... uriVariables) throws Exception {
        return perform(MockMvcRequestBuilders.post(path, uriVariables)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions postEmpty(String path, Object... uriVariables) throws Exception {
        return perform(MockMvcRequestBuilders.post(path, uriVariables));
    }

    public ResultActions put(String path, String json, Object... uriVariables) throws Exception {
        return perform(MockMvcRequestBuilders.put(path, uriVariables)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions get(String path, Object... uriVariables) throws Exception {
        return perform(MockMvcRequestBuilders.get(path, uriVariables)
                .accept(MediaType.APPLICATION_JSON, MediaType.ALL));
    }

    public ResultActions delete(String path, Object... uriVariables) throws Exception {
        return perform(MockMvcRequestBuilders.delete(path, uriVariables));
    }

    /** POSTs a multipart form with text fields (name/value pairs) and files. */
    public ResultActions multipart(String path, List<MockMultipartFile> files, String... fields) throws Exception {
        var request = MockMvcRequestBuilders.multipart(path);
        files.forEach(request::file);
        for (int i = 0; i + 1 < fields.length; i += 2) {
            request.param(fields[i], fields[i + 1]);
        }
        return perform(request);
    }

    /** POSTs a multipart request with a JSON part named {@code body} and file parts. */
    public ResultActions multipartWithJson(String path, String json, List<MockMultipartFile> files) throws Exception {
        var request = MockMvcRequestBuilders.multipart(path);
        files.forEach(request::file);
        request.part(new org.springframework.mock.web.MockPart("body", null, json.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                MediaType.APPLICATION_JSON));
        return perform(request);
    }

    public ResultActions perform(AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        postProcessors.forEach(request::with);
        return mockMvc.perform(request);
    }

    /** Reads a JSON path from a response body. */
    public static <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    /** Reads a numeric JSON path (integers and decimals alike) as a double. */
    public static double number(ResultActions result, String path) throws Exception {
        Number value = read(result, path);
        if (value == null) {
            throw new AssertionError("No number at " + path);
        }
        return value.doubleValue();
    }

    /** Assertion code that may throw. */
    @FunctionalInterface
    public interface Check {
        void run() throws Exception;
    }

    /** Retries a check until it passes or the timeout expires (for asynchronous event handling). */
    public static void eventually(java.time.Duration timeout, Check check) throws Exception {
        var deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            try {
                check.run();
                return;
            } catch (AssertionError | Exception e) {
                if (System.nanoTime() > deadline) {
                    throw e;
                }
                Thread.sleep(200);
            }
        }
    }

    /** The id from the Location header of a 201 response. */
    public static String idOf(ResultActions result) throws Exception {
        var location = result.andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return location == null ? null : location.substring(location.lastIndexOf('/') + 1);
    }
}
