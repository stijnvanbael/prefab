package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method on an aggregate to generate a Server-Sent Events (SSE) streaming endpoint.
 *
 * <p>Two usage models are supported:
 * <ul>
 *   <li><strong>Pull model</strong>: annotate an instance method returning
 *       {@code java.util.stream.Stream<T>} or {@code Flux<T>}. The processor generates a
 *       controller endpoint that consumes the stream on a virtual thread (for
 *       {@code Stream<T>}) or via reactive subscribe (for {@code Flux<T>}) and forwards each
 *       element as an SSE frame.</li>
 *   <li><strong>Push model</strong>: annotate an {@code @EventHandler @ByReference} method.
 *       Each time the event handler fires for a given aggregate instance, the event payload is
 *       pushed to any SSE client currently connected for that instance via a generated
 *       {@code {Aggregate}SseRegistry}.</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Streaming {

    /**
     * Path suffix appended after {@code /{id}}.
     * For example, {@code "/stream"} produces {@code GET /my-aggregates/{id}/stream}.
     *
     * @return the path suffix
     */
    String path() default "/stream";

    /**
     * SSE event name sent in the {@code event:} field of each SSE frame.
     *
     * @return the SSE event name
     */
    String event() default "message";

    /**
     * Interval in seconds between keepalive {@code event: ping} frames sent while the emitter
     * is open. A value of {@code 0} disables the heartbeat entirely.
     *
     * @return heartbeat interval in seconds
     */
    int heartbeatSeconds() default 15;

    /**
     * Push model only: name of a {@code boolean} field on the event record. When that field is
     * {@code true}, {@code emitter.complete()} is called after sending the final frame, cleanly
     * closing the SSE connection. An empty string disables automatic stream termination.
     *
     * @return the terminal field name, or {@code ""} to disable
     */
    String terminal() default "";

    /**
     * Security settings for the generated SSE connect endpoint.
     *
     * @return security settings
     */
    Security security() default @Security;
}

