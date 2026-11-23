package be.appify.prefab.processor.rest.binary;

import be.appify.prefab.core.annotations.Exposure;
import be.appify.prefab.core.annotations.rest.Download;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.validation.ContentType;
import be.appify.prefab.core.annotations.validation.FileSize;
import be.appify.prefab.core.domain.Binary;
import be.appify.prefab.core.spring.StorageService;
import be.appify.prefab.processor.AnnotationManifest;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.ExposureResolver;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/** Plugin to handle Binary fields in Prefab. */
public class BinaryPlugin implements PrefabPlugin {
    private final BinaryControllerWriter binaryControllerWriter = new BinaryControllerWriter();
    private final BinaryTestClientWriter binaryTestClientWriter = new BinaryTestClientWriter();

    private PrefabContext context;

    /** Creates a new instance of BinaryPlugin. */
    public BinaryPlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public Optional<ParameterSpec> requestMethodParameter(VariableManifest parameter) {
        if (parameter.type().is(Binary.class)) {
            return Optional.of(ParameterSpec.builder(MultipartFile.class, parameter.name())
                    .addAnnotations(fileConstraintsOf(parameter))
                    .build());
        } else {
            return Optional.empty();
        }
    }

    // Spring MVC validates constrained controller parameters, which rejects an invalid upload before it is stored
    private static List<AnnotationSpec> fileConstraintsOf(VariableManifest parameter) {
        return parameter.annotations().stream()
                .filter(annotation -> annotation.type().is(ContentType.class) || annotation.type().is(FileSize.class))
                .map(AnnotationManifest::asSpec)
                .toList();
    }

    @Override
    public Optional<ParameterSpec> requestBodyParameter(VariableManifest parameter) {
        if (parameter.type().is(Binary.class)) {
            return Optional.of(ParameterSpec.builder(MultipartFile.class, parameter.name())
                    .addAnnotation(AnnotationSpec.builder(RequestPart.class)
                            .addMember("name", "$S", parameter.name())
                            .build())
                    .addAnnotation(JsonIgnore.class)
                    .build());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CodeBlock> mapRequestParameter(VariableManifest parameter) {
        if (parameter.type().is(Binary.class)) {
            return Optional.of(CodeBlock.of("storageService.store(request.$N())", parameter.name()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest classManifest) {
        if (classManifest.fields().stream().anyMatch(f -> f.type().is(Binary.class))) {
            return Set.of(ClassName.get(StorageService.class));
        }
        return Collections.emptySet();
    }

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        if (!hasPublicAggregateExposure(manifest)) {
            return;
        }
        var downloads = manifest.fields().stream()
                .filter(f -> f.type().is(Binary.class) && f.hasAnnotation(Download.class))
                .toList();
        if (!downloads.isEmpty() && !hasGetById(manifest)) {
            // The generated download endpoint looks the aggregate up through service.getById
            downloads.forEach(field -> context.logError(
                    "[@Download] field '%s' on %s requires the aggregate to be annotated with @GetById"
                            .formatted(field.name(), manifest.simpleName()),
                    manifest.element()));
            return;
        }
        downloads.forEach(field -> builder.addMethod(binaryControllerWriter.downloadMethod(manifest, field)));
    }

    @Override
    public void writeTestClient(ClassManifest manifest, TypeSpec.Builder builder) {
        if (!hasPublicAggregateExposure(manifest)) {
            return;
        }
        manifest.fields().stream()
                .filter(f -> f.type().is(Binary.class) && f.hasAnnotation(Download.class))
                .forEach(field ->
                        builder.addMethod(binaryTestClientWriter.downloadMethod(manifest, field)));
    }

    private static boolean hasGetById(ClassManifest manifest) {
        return !manifest.annotationsOfType(GetById.class).isEmpty();
    }

    private static boolean hasPublicAggregateExposure(ClassManifest manifest) {
        return ExposureResolver.isPublic((TypeElement) manifest.element(), Exposure.INHERIT);
    }

}
