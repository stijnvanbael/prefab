package be.appify.prefab.processor.event.avro;

import be.appify.prefab.core.annotations.AvscFirst;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import org.apache.avro.Schema;

/** Plugin that processes {@link AvscFirst}-annotated types and generates Java records from AVSC schema files. */
public class AvscFirstPlugin implements PrefabPlugin {

    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        context.roundEnvironment()
                .getElementsAnnotatedWith(AvscFirst.class)
                .forEach(element -> processElement(element, element.getAnnotation(AvscFirst.class)));
    }

    private void processElement(Element element, AvscFirst annotation) {
        var schema = parseSchema(annotation.path(), element);
        if (schema == null) return;
        var defaultPackage = context.processingEnvironment()
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
        new AvscEventWriter(context.processingEnvironment())
                .writeAll(schema, annotation.topic(), annotation.platform(), defaultPackage);
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
