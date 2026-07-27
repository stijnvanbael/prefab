package be.appify.prefab.avro.processor;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.AvscFiles;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;
import org.apache.avro.Schema;


/** Plugin that processes {@link Avsc}-annotated types and generates Java records from AVSC schema files. */
public class AvscPlugin implements PrefabPlugin {

    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeEventFiles() {
        var registry = new AvscRecordRegistry(context.processingEnvironment());
        context.avscElementsFromCurrentCompilation()
                .forEach(element -> processElement(element, element.getAnnotation(Avsc.class), registry));
    }

    private void processElement(Element element, Avsc annotation, AvscRecordRegistry registry) {
        var eventAnnotation = element.getAnnotation(Event.class);
        if (eventAnnotation == null) {
            context.processingEnvironment().getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Avsc requires @Event to be present on the same type. "
                            + "Add @Event(topic = \"...\", serialization = Event.Serialization.AVRO).",
                    element);
            return;
        }
        var avscFiles = AvscFiles.resolve(annotation);
        if (avscFiles.hasErrors()) {
            avscFiles.errors().forEach(error -> context.processingEnvironment().getMessager().printMessage(
                    Diagnostic.Kind.ERROR, error, element));
            return;
        }
        var typeElement = (TypeElement) element;
        var contractPackage = context.processingEnvironment()
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
        var contractInterface = ClassName.get(contractPackage, typeElement.getSimpleName().toString());
        var generateAnnotationSpecs = buildGenerateAnnotationSpecs(typeElement);
        var sharedPartitioningProperty = sharedPartitioningProperty(typeElement);
        var writer = new AvscEventWriter(context);
        for (var definition : avscFiles.definitions()) {
            var schema = parseSchema(definition.path(), element);
            if (schema == null) continue;
            var effectivePartitioningProperty = definition.keyProperty().or(() -> sharedPartitioningProperty);
            if (effectivePartitioningProperty.isPresent() && schema.getField(effectivePartitioningProperty.orElseThrow()) == null) {
                context.processingEnvironment().getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        missingPartitioningPropertyMessage(definition, effectivePartitioningProperty.orElseThrow(), sharedPartitioningProperty),
                        element);
                continue;
            }
            var javaTypeName = capitalize(schema.getName());
            if (javaTypeName.equals(contractInterface.simpleName())) {
                context.processingEnvironment().getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "The capitalised AVSC record name '" + javaTypeName + "' conflicts with the contract interface name. "
                                + "Rename the interface or the AVSC record to avoid the collision.",
                        element);
                continue;
            }
            registry.registerAll(definition.path(), schema, element);
            writer.writeAll(schema, eventAnnotation.topic(), eventAnnotation.platform(), contractPackage, contractInterface, generateAnnotationSpecs);
        }
    }

    private Optional<String> sharedPartitioningProperty(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getAnnotation(PartitioningKey.class) != null)
                .findFirst()
                .map(element -> element.getSimpleName().toString());
    }

    private String missingPartitioningPropertyMessage(AvscFiles.Definition definition, String property,
                                                      Optional<String> sharedPartitioningProperty) {
        if (definition.keyProperty().isPresent()) {
            return "AVSC file '%s' does not define field '%s' required by @Avsc keyProperty."
                    .formatted(definition.path(), property);
        }
        if (sharedPartitioningProperty.isPresent()) {
            return "AVSC file '%s' does not define field '%s' required by the @PartitioningKey method on the @Avsc contract."
                    .formatted(definition.path(), property);
        }
        return "AVSC file '%s' does not define field '%s'."
                .formatted(definition.path(), property);
    }

    private List<AnnotationSpec> buildGenerateAnnotationSpecs(TypeElement element) {
        return Stream.of(element.getAnnotationsByType(Generate.class))
                .map(this::toAnnotationSpec)
                .toList();
    }

    private AnnotationSpec toAnnotationSpec(Generate generate) {
        TypeName pluginTypeName;
        try {
            pluginTypeName = ClassName.get(generate.plugin());
        } catch (MirroredTypeException e) {
            pluginTypeName = TypeName.get(e.getTypeMirror());
        }
        var builder = AnnotationSpec.builder(Generate.class)
                .addMember("plugin", "$T.class", pluginTypeName);
        if (!generate.enabled()) {
            builder.addMember("enabled", "$L", false);
        }
        if (generate.target() != OutputTarget.DEFAULT) {
            builder.addMember("target", "$T.$L", ClassName.get(OutputTarget.class), generate.target().name());
        }
        return builder.build();
    }

    private Schema parseSchema(String path, Element originatingElement) {
        try (var stream = openResource(path)) {
            if (stream == null) {
                context.processingEnvironment().getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "AVSC file not found: '" + path + "'. "
                                + "Ensure it is on the classpath (e.g. src/main/resources/" + path + ").",
                        originatingElement);
                return null;
            }
            return new Schema.Parser().parse(stream);
        } catch (IOException e) {
            context.processingEnvironment().getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to read or parse AVSC file '" + path + "': " + e.getMessage(),
                    originatingElement);
            return null;
        }
    }

    private InputStream openResource(String path) throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream != null) return stream;
        var file = Path.of("src/main/resources", path);
        if (Files.exists(file)) return Files.newInputStream(file);
        return null;
    }

    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
