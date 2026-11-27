package be.appify.prefab.processor.rest;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.processor.ClassManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Derives the HTTP endpoints Prefab exposes for an aggregate from its REST annotations, without generating anything.
 *
 * <p>Shared by the agent context file and the hand-written code check so that both describe the same surface.
 */
public final class AggregateEndpoints {

    /**
     * One HTTP endpoint exposed for an aggregate.
     *
     * @param method
     *         the HTTP method
     * @param path
     *         the full path, starting with {@code /}
     * @param description
     *         a short description of the operation, such as {@code create} or {@code update via rename}
     */
    public record Endpoint(String method, String path, String description) {
    }

    private AggregateEndpoints() {
    }

    /**
     * Lists the endpoints of an aggregate in declaration order.
     *
     * @param manifest
     *         the aggregate
     * @return the endpoints; empty when the aggregate exposes none
     */
    public static List<Endpoint> of(ClassManifest manifest) {
        var basePath = "/" + ControllerUtil.pathOf(manifest);
        var endpoints = new ArrayList<Endpoint>();
        var type = manifest.element();
        manifest.constructorsWith(Create.class).forEach(constructor -> {
            var create = constructor.getAnnotation(Create.class);
            endpoints.add(endpoint(create.method(), basePath + create.path(), "create"));
        });
        manifest.methodsWith(Create.class).forEach(method -> {
            var create = method.getAnnotation(Create.class);
            endpoints.add(endpoint(create.method(), basePath + create.path(),
                    "create via " + method.getSimpleName()));
        });
        Optional.ofNullable(type.getAnnotation(GetList.class))
                .ifPresent(getList -> endpoints.add(endpoint(getList.method(), basePath + getList.path(), "list")));
        Optional.ofNullable(type.getAnnotation(GetById.class))
                .ifPresent(getById -> endpoints.add(
                        endpoint(getById.method(), basePath + getById.path(), "get by id")));
        manifest.methodsWith(Update.class).forEach(method -> {
            var update = method.getAnnotation(Update.class);
            endpoints.add(endpoint(update.method(), basePath + "/{id}" + update.path(),
                    "update via " + method.getSimpleName()));
        });
        Optional.ofNullable(type.getAnnotation(Delete.class))
                .ifPresent(delete -> endpoints.add(endpoint(delete.method(), basePath + delete.path(), "delete")));
        manifest.methodsWith(Delete.class).forEach(method -> {
            var delete = method.getAnnotation(Delete.class);
            endpoints.add(endpoint(delete.method(), basePath + delete.path(),
                    "delete via " + method.getSimpleName()));
        });
        return List.copyOf(endpoints);
    }

    private static Endpoint endpoint(String method, String path, String description) {
        return new Endpoint(method, path, description);
    }
}
