package be.appify.prefab.processor.eventhandler;

import be.appify.prefab.processor.PrefabPlugin;

import java.lang.annotation.Annotation;

public interface EventHandlerPlugin extends PrefabPlugin {
    Class<? extends Annotation> annotation();
}
