package be.appify.prefab.processor.eventhandler.broadcast;

import be.appify.prefab.processor.TypeManifest;

public record BroadcastEventHandlerManifest(
        String methodName,
        TypeManifest eventType,
        TypeManifest returnType
) {
}
