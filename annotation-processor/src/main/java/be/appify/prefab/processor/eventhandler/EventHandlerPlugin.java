package be.appify.prefab.processor.eventhandler;

import be.appify.prefab.processor.PrefabPlugin;

import java.lang.annotation.Annotation;

/** Interface for event handler plugins. */
public interface EventHandlerPlugin extends PrefabPlugin {
    /**
     * The annotation class that this plugin handles.
     *
     * @return The annotation class that this plugin handles.
     */
    Class<? extends Annotation> annotation();
}
