package be.appify.prefab.processor.error;

/**
 * Thrown after a failure has already been reported as a compiler diagnostic, to unwind to an outer
 * {@link ProcessingGuard} without reporting the same failure twice.
 */
public final class ProcessingAbortedException extends RuntimeException {
    ProcessingAbortedException() {
        super("Processing aborted after a reported error", null, false, false);
    }
}
