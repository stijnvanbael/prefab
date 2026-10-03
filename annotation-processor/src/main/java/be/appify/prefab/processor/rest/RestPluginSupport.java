package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import java.util.Objects;

/**
 * Shared lifecycle support for REST operation plugins.
 */
abstract class RestOperationPluginSupport implements PrefabPlugin {
    private PrefabContext context;

    @Override
    public final void initContext(PrefabContext context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        onContextInitialized(context);
    }

    /**
     * Hook for subclasses that need to react when the context becomes available.
     *
     * @param context
     *         the initialized processing context
     */
    protected void onContextInitialized(PrefabContext context) {
    }

    /**
     * Returns the initialized processing context.
     *
     * @return the current {@link PrefabContext}
     */
    protected final PrefabContext context() {
        return Objects.requireNonNull(context, "PrefabContext has not been initialised");
    }
}



