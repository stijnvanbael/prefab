package be.appify.prefab.processor.event.handler;

import be.appify.prefab.processor.TypeManifest;

record StaticEventHandlerManifest(
        String methodName,
        TypeManifest eventType,
        TypeManifest returnType
) {
}
