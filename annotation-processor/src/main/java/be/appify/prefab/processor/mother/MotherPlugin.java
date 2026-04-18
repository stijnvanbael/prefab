package be.appify.prefab.processor.mother;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.Binary;
import org.springframework.web.multipart.MultipartFile;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.event.EventPlatformPluginSupport;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import static org.apache.commons.text.WordUtils.capitalize;

/**
 * Plugin that generates Object Mother classes for request records and @Event-annotated types.
 */
public class MotherPlugin implements PrefabPlugin {

    private PrefabContext context;
    private final Set<String> writtenTypes = Collections.synchronizedSet(new HashSet<>());


    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, List<PolymorphicAggregateManifest> polymorphicManifests) {
        var writer = new MotherWriter(context, writtenTypes);

        manifests.forEach(manifest -> writeRequestMothers(manifest, writer));

        context.eventElements().forEach(element -> {
            var type = TypeManifest.of(element.asType(), context.processingEnvironment());
            // AVSC-generated records are compiled in round 2 from generated sources, so
            // TestJavaFileWriter cannot resolve the source root path from them directly.
            // Use the @Avsc contract interface (which is always a source file) instead.
            var preferredElement = EventPlatformPluginSupport.isAvscGeneratedRecord(element)
                    ? avscContractInterface(element)
                    : element;
            if (type.isSealed()) {
                type.permittedSubtypes().forEach(subtype -> {
                    if (subtype.isRecord()) {
                        writer.writeEventMother(subtype, preferredElement);
                    }
                });
            } else if (type.isRecord()) {
                writer.writeEventMother(type, preferredElement);
            }
        });
    }

    private void writeRequestMothers(ClassManifest manifest, MotherWriter writer) {
        var preferredElement = manifest.type().asElement();

        manifest.constructorsWith(Create.class).forEach(ctor -> {
            if (!ctor.getParameters().isEmpty()) {
                var name = "Create%sRequest".formatted(manifest.simpleName());
                var params = parametersOf(ctor).stream()
                        .map(p -> isAggregateTyped(p) ? p.withType(String.class).withName(p.name() + "Id") : p)
                        .map(p -> isBinaryTyped(p) ? p.withType(MultipartFile.class) : p)
                        .toList();
                writer.writeRequestMother(name, manifest.packageName(), params, preferredElement);
            }
        });

        manifest.methodsWith(Update.class).forEach(method -> {
            var opName = capitalize(method.getSimpleName().toString());
            var name = "%s%sRequest".formatted(manifest.simpleName(), opName);
            var params = parametersOf(method).stream()
                    .filter(p -> !isAggregateTyped(p))
                    .map(p -> isBinaryTyped(p) ? p.withType(MultipartFile.class) : p)
                    .toList();
            if (!params.isEmpty()) {
                writer.writeRequestMother(name, manifest.packageName(), params, preferredElement);
            }
        });
    }

    private boolean isAggregateTyped(VariableManifest param) {
        return !param.type().annotationsOfType(Aggregate.class).isEmpty();
    }

    private boolean isBinaryTyped(VariableManifest param) {
        return param.type().is(Binary.class);
    }

    private List<VariableManifest> parametersOf(ExecutableElement element) {
        return element.getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
    }

    private static TypeElement avscContractInterface(TypeElement element) {
        return element.getInterfaces().stream()
                .map(iface -> (TypeElement) ((DeclaredType) iface).asElement())
                .filter(iface -> iface.getAnnotation(Avsc.class) != null)
                .findFirst()
                .orElse(element);
    }
}

