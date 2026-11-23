package be.appify.prefab.processor;

import be.appify.prefab.processor.error.PrefabProcessingException;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Utility class to write Java files using the annotation processing environment.
 */
public class JavaFileWriter implements FileOutput {

    // Annotation processing is single-threaded; a plain HashMap is sufficient and avoids
    // unnecessary synchronization overhead. ProcessingEnvironment instances are long-lived
    // within a compilation so a WeakHashMap is not appropriate here.
    private static final Map<ProcessingEnvironment, Set<String>> PROCESSOR_GENERATED_FILES = new HashMap<>();

    private final ProcessingEnvironment processingEnvironment;
    private final String packageSuffix;
    private List<TypeElement> originatingElements = List.of();

    /**
     * Constructs a JavaFileWriter.
     *
     * @param processingEnvironment the processing environment
     * @param packageSuffix         the package suffix to append to the base package
     */
    public JavaFileWriter(ProcessingEnvironment processingEnvironment, String packageSuffix) {
        this.processingEnvironment = processingEnvironment;
        this.packageSuffix = packageSuffix;
    }

    @Override
    public void setPreferredElement(TypeElement element) {
        setOriginatingElements(element);
    }

    @Override
    public void setOriginatingElements(TypeElement... elements) {
        originatingElements = List.of(elements);
    }

    /**
     * Writes a Java file with the specified package prefix, type name, and type specification.
     *
     * @param packagePrefix the base package prefix
     * @param typeName      the name of the type to be written
     * @param type          the type specification
     */
    @Override
    public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
        if (!GeneratedFiles.defer(processingEnvironment, () -> write(packagePrefix, typeName, type))) {
            write(packagePrefix, typeName, type);
        }
    }

    private void write(String packagePrefix, String typeName, TypeSpec type) {
        try {
            var packageName = !isBlank(packageSuffix) ? "%s.%s".formatted(packagePrefix, packageSuffix) : packagePrefix;
            var qualifiedName = "%s.%s".formatted(packageName, typeName);
            JavaFileObject builderFile;
            try {
                builderFile = processingEnvironment.getFiler()
                        .createSourceFile(qualifiedName, originatingElements.toArray(TypeElement[]::new));
            } catch (FilerException e) {
                if (indicatesFileAlreadyExists(e)) {
                    if (!wasGeneratedByProcessor(qualifiedName)) {
                        processingEnvironment.getMessager().printMessage(
                                Diagnostic.Kind.NOTE,
                                "Skipping generation of %s: a source file with this name already exists".formatted(qualifiedName)
                                        + " and will be used as-is. Remove it to let the annotation processor regenerate it."
                        );
                    }
                    return;
                }
                throw new PrefabProcessingException(originatingElements.stream().findFirst().orElse(null), "Prefab",
                        "cannot create generated source file %s: %s".formatted(qualifiedName, e.getMessage()),
                        "check that no other annotation processor generates the same type and that the output"
                                + " directory is writable",
                        e);
            }
            try (var writer = builderFile.openWriter()) {
                var javaFile = JavaFile
                        .builder(packageName, type)
                        .skipJavaLangImports(true)
                        .indent("    ")
                        .build();
                javaFile.writeTo(writer);
            }
            markAsGeneratedByProcessor(qualifiedName);
        } catch (IOException e) {
            throw new PrefabProcessingException(originatingElements.stream().findFirst().orElse(null), "Prefab",
                    "cannot write generated source file %s.%s: %s".formatted(packagePrefix, typeName, e.getMessage()),
                    "check that the compiler output directory exists, is writable and has free disk space",
                    e);
        }
    }

    private boolean wasGeneratedByProcessor(String qualifiedName) {
        return PROCESSOR_GENERATED_FILES
                .getOrDefault(processingEnvironment, Set.of())
                .contains(qualifiedName);
    }

    private void markAsGeneratedByProcessor(String qualifiedName) {
        PROCESSOR_GENERATED_FILES
                .computeIfAbsent(processingEnvironment, k -> new HashSet<>())
                .add(qualifiedName);
    }

    private static boolean indicatesFileAlreadyExists(FilerException e) {
        return e.getMessage() != null && e.getMessage().startsWith("Attempt to recreate a file for type");
    }
}
