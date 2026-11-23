package be.appify.prefab.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * Transactional scope for generated files.
 *
 * <p>All files written by the file writers while an action runs in {@link #buffered} are held back and only handed
 * to the compiler when the action completes. If the action fails, nothing is written: generated sources of a
 * half-processed aggregate would otherwise reference types that were never generated and bury the real
 * diagnostic under consequent "cannot find symbol" errors.
 */
final class GeneratedFiles {
    private static final Map<ProcessingEnvironment, List<Runnable>> ACTIVE = new WeakHashMap<>();

    private GeneratedFiles() {
    }

    /**
     * Runs an action and writes the files it generated only if it completed successfully.
     *
     * @param environment the processing environment
     * @param action      the generation action
     */
    static void buffered(ProcessingEnvironment environment, Runnable action) {
        List<Runnable> pending = new ArrayList<>();
        synchronized (ACTIVE) {
            ACTIVE.put(environment, pending);
        }
        try {
            action.run();
        } finally {
            synchronized (ACTIVE) {
                ACTIVE.remove(environment);
            }
        }
        pending.forEach(Runnable::run);
    }

    /**
     * Defers a file write when a buffered scope is active.
     *
     * @param environment the processing environment
     * @param write       the write to defer
     * @return {@code true} if the write was deferred, {@code false} if the caller must write immediately
     */
    static boolean defer(ProcessingEnvironment environment, Runnable write) {
        synchronized (ACTIVE) {
            var pending = ACTIVE.get(environment);
            if (pending == null) {
                return false;
            }
            pending.add(write);
            return true;
        }
    }
}
