package be.appify.prefab.processor.eventhandler;

import be.appify.prefab.processor.TypeManifest;

public record StaticEventHandlerManifest(
        String methodName,
        TypeManifest eventType,
        TypeManifest returnType
) {
}
