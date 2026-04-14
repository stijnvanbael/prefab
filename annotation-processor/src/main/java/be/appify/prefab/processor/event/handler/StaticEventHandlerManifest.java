package be.appify.prefab.processor.event.handler;

import be.appify.prefab.processor.TypeManifest;

record StaticEventHandlerManifest(
        String methodName,
        TypeManifest eventType,
        TypeManifest returnType,
        TypeManifest componentType
) {
    /**
     * Creates a manifest for a handler whose method lives directly on the aggregate root.
     */
    static StaticEventHandlerManifest ofOwnHandler(
            String methodName, TypeManifest eventType, TypeManifest returnType) {
        return new StaticEventHandlerManifest(methodName, eventType, returnType, null);
    }

    /**
     * Returns {@code true} when this handler belongs to a separate component that is merged into the aggregate
     * root's service.
     */
    boolean isMerged() {
        return componentType != null;
    }
}
