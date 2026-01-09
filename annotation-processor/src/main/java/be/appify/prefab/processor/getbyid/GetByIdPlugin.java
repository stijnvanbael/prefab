package be.appify.prefab.processor.getbyid;

import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;

import java.util.Optional;

/**
 * Prefab plugin that generates getById controller, service, and test client methods based on the @GetById annotation.
 */
public class GetByIdPlugin implements PrefabPlugin {
    private final GetByIdControllerWriter controllerWriter = new GetByIdControllerWriter();
    private final GetByIdServiceWriter serviceWriter = new GetByIdServiceWriter();
    private final GetByIdTestClientWriter testClientWriter = new GetByIdTestClientWriter();

    /** Creates a new instance of GetByIdPlugin. */
    public GetByIdPlugin() {
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        getByIdAnnotation(manifest).ifPresent(getById ->
                builder.addMethod(controllerWriter.getByIdMethod(manifest, getById)));
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        getByIdAnnotation(manifest).ifPresent(ignored ->
                builder.addMethod(serviceWriter.getByIdMethod(manifest)));
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder, PrefabContext context) {
        getByIdAnnotation(manifest).ifPresent(ignored ->
                builder.addMethod(testClientWriter.getByIdMethod(manifest)));
    }

    private Optional<GetById> getByIdAnnotation(ClassManifest manifest) {
        return manifest.annotationsOfType(GetById.class)
                .stream()
                .findFirst();
    }
}
