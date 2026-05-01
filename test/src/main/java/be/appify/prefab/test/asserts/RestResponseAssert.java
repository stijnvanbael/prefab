package be.appify.prefab.test.asserts;

import org.springframework.test.web.servlet.ResultActions;

import java.util.function.Consumer;

/**
 * Fluent assertion object returned by generated test client operations.
 * <p>
 * Wraps the underlying {@link ResultActions} so MockMvc status and header assertions remain accessible,
 * and provides convenience accessors for common response data.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * productClient.createProduct("Laptop")
 *     .andAssert(result -> assertThat(result.id()).isNotBlank());
 *
 * productClient.getProductById(id)
 *     .andAssert(result -> assertThat(result.response().name()).isEqualTo("Laptop"));
 * }
 * </pre>
 *
 * @param <R> the type of the response body
 */
public class RestResponseAssert<R> {

    private final ResultActions resultActions;
    private final String id;
    private final R response;

    public RestResponseAssert(ResultActions resultActions, String id, R response) {
        this.resultActions = resultActions;
        this.id = id;
        this.response = response;
    }

    /**
     * Returns the underlying {@link ResultActions} for further MockMvc assertions.
     *
     * @return the result actions
     */
    public ResultActions resultActions() {
        return resultActions;
    }

    /**
     * Returns the ID extracted from the Location header of the response.
     *
     * @return the resource ID
     */
    public String id() {
        return id;
    }

    /**
     * Returns the deserialized response body.
     *
     * @return the response body
     */
    public R response() {
        return response;
    }

    /**
     * Applies the given custom assertion on this response assert and returns this object for chaining.
     *
     * @param assertion the custom assertion to apply
     * @return this assertion object for chaining
     */
    public RestResponseAssert<R> andAssert(Consumer<RestResponseAssert<R>> assertion) {
        assertion.accept(this);
        return this;
    }
}
