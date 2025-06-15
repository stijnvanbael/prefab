package be.appify.prefab.processor.eventhandler.byreference;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.processor.TypeManifest;

public record ByReferenceEventHandlerManifest(
        String methodName,
        EventHandler.ByReference annotation,
        TypeManifest eventType,
        TypeManifest referencedType
) {
}
