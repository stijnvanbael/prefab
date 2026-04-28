package be.appify.prefab.processor.event.avro;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.ClassName;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
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
        context.roundEnvironment()
                .getElementsAnnotatedWith(Avsc.class)
                .forEach(element -> processElement(element, element.getAnnotation(Avsc.class)));
    }

    private void processElement(Element element, Avsc annotation) {
        var eventAnnotation = element.getAnnotation(Event.class);
        if (eventAnnotation == null) {
            context.processingEnvironment().getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Avsc requires @Event to be present on the same type. "
                            + "Add @Event(topic = \"...\", serialization = Event.Serialization.AVRO).",
                    element);
            return;
        }
        var typeElement = (TypeElement) element;
        var contractPackage = context.processingEnvironment()
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
        var contractInterface = ClassName.get(contractPackage, typeElement.getSimpleName().toString());
        var writer = new AvscEventWriter(context.processingEnvironment());
        for (var path : annotation.value()) {
            var schema = parseSchema(path, element);
            if (schema == null) continue;
            if (schema.getName().equals(contractInterface.simpleName())) {
                context.processingEnvironment().getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "The AVSC record name '" + schema.getName() + "' conflicts with the contract interface name. "
                                + "Rename the interface or the record in the AVSC schema to avoid the collision.",
                        element);
                continue;
            }
            writer.writeAll(schema, eventAnnotation.topic(), eventAnnotation.platform(), contractPackage, contractInterface);
        }
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
        // Primary: load from the annotation processor's classpath (covers both
        // test environments and production Maven builds where resources are on
        // the compile classpath).
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream != null) return stream;
        // Secondary: resolve relative to Maven's conventional resource directory.
        // Useful when running the processor in a non-standard build environment
        // where resources have not yet been copied to the compile classpath.
        var file = Path.of("src/main/resources", path);
        if (Files.exists(file)) return Files.newInputStream(file);
        return null;
    }
}
