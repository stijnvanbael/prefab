package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Thread-local execution scope for plugin-specific output routing.
 */
final class PluginOutputScope {
    private static final ThreadLocal<State> STATE = new ThreadLocal<>();

    private PluginOutputScope() {
    }

    static void run(PrefabContext context, OutputTarget target, Runnable action) {
        call(context, target, () -> {
            action.run();
            return null;
        });
    }

    static <T> T call(PrefabContext context, OutputTarget target, Supplier<T> action) {
        var previous = STATE.get();
        STATE.set(new State(context, target));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                STATE.remove();
            } else {
                STATE.set(previous);
            }
        }
    }

    static Optional<State> current() {
        return Optional.ofNullable(STATE.get());
    }

    record State(PrefabContext context, OutputTarget target) {
    }
}

