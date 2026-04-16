package be.appify.prefab.processor.event.handler;

import be.appify.prefab.processor.TypeManifest;

record StaticEventHandlerManifest(
        String methodName,
        TypeManifest eventType,
        TypeManifest returnType,
        TypeManifest componentType,
        boolean instanceMethod
) {
    /**
     * Creates a manifest for a handler whose method lives directly on the aggregate root.
     */
    static StaticEventHandlerManifest ofOwnHandler(
            String methodName, TypeManifest eventType, TypeManifest returnType) {
        return new StaticEventHandlerManifest(methodName, eventType, returnType, null, false);
    }

    /**
     * Creates a manifest for a static handler method on a separate component that is merged into the aggregate
     * root's service.
     */
    static StaticEventHandlerManifest ofMergedStaticHandler(
            String methodName, TypeManifest eventType, TypeManifest returnType, TypeManifest componentType) {
        return new StaticEventHandlerManifest(methodName, eventType, returnType, componentType, false);
    }

    /**
     * Creates a manifest for an instance handler method on a Spring {@code @Component} that is merged into the
     * aggregate root's service. The component is injected as a dependency and the method is called on it.
     */
    static StaticEventHandlerManifest ofMergedInstanceHandler(
            String methodName, TypeManifest eventType, TypeManifest returnType, TypeManifest componentType) {
        return new StaticEventHandlerManifest(methodName, eventType, returnType, componentType, true);
    }

    /**
     * Returns {@code true} when this handler belongs to a separate component that is merged into the aggregate
     * root's service.
     */
    boolean isMerged() {
        return componentType != null;
    }
}
