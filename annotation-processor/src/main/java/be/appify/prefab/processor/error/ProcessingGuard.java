package be.appify.prefab.processor.error;

import java.util.function.Supplier;
import javax.lang.model.element.Element;

/**
 * Failure boundary that turns exceptions escaping from processor or plugin code into compiler diagnostics.
 *
 * <ul>
 *   <li>{@link PrefabProcessingException}: expected failure, reported as a single actionable error.</li>
 *   <li>Any other {@link RuntimeException}: unexpected failure, reported as an internal error naming the component
 *       (processor or plugin) and the cause chain.</li>
 *   <li>{@link Error}s (out of memory, stack overflow, linkage errors...) are never caught.</li>
 * </ul>
 *
 * <p>Boundaries nest: a failure is reported by the innermost boundary only; outer boundaries see a
 * {@link ProcessingAbortedException} and stay silent.
 */
public final class ProcessingGuard {
    private final ErrorReporter reporter;

    /**
     * Creates a guard.
     *
     * @param reporter the reporter used to emit diagnostics
     */
    public ProcessingGuard(ErrorReporter reporter) {
        this.reporter = reporter;
    }

    /**
     * Runs an action and reports failures, then lets the caller continue with independent work.
     *
     * @param component the component running the action, e.g. {@code "Prefab annotation processor"} or a plugin name
     * @param element   the element being processed, used when the failure carries none
     * @param action    the action to run
     * @return {@code true} when the action completed, {@code false} when a failure was reported
     */
    public boolean run(String component, Element element, Runnable action) {
        try {
            action.run();
            return true;
        } catch (ProcessingAbortedException e) {
            return false;
        } catch (RuntimeException e) {
            reporter.reportFailure(component, element, e);
            return false;
        }
    }

    /**
     * Runs an action, reports failures and rethrows a {@link ProcessingAbortedException} so that an outer boundary
     * stops work that depends on this action.
     *
     * @param component the component running the action
     * @param element   the element being processed
     * @param action    the action to run
     */
    public void runOrAbort(String component, Element element, Runnable action) {
        if (!run(component, element, action)) {
            throw new ProcessingAbortedException();
        }
    }

    /**
     * Like {@link #runOrAbort(String, Element, Runnable)} for actions returning a value.
     *
     * @param component the component running the action
     * @param element   the element being processed
     * @param action    the action to run
     * @param <T>       the result type
     * @return the action's result
     */
    public <T> T callOrAbort(String component, Element element, Supplier<T> action) {
        var result = new Object[1];
        runOrAbort(component, element, () -> result[0] = action.get());
        @SuppressWarnings("unchecked")
        var value = (T) result[0];
        return value;
    }
}
