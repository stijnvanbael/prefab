package be.appify.prefab.processor;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Utility class to write Java files using the annotation processing environment.
 */
public class JavaFileWriter {

    private static final Map<ProcessingEnvironment, Set<String>> PROCESSOR_GENERATED_FILES = new WeakHashMap<>();

    private final ProcessingEnvironment processingEnvironment;
    private final String packageSuffix;

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

    /**
     * Writes a Java file with the specified package prefix, type name, and type specification.
     *
     * @param packagePrefix the base package prefix
     * @param typeName      the name of the type to be written
     * @param type          the type specification
     */
    public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
        try {
            var packageName = !isBlank(packageSuffix) ? "%s.%s".formatted(packagePrefix, packageSuffix) : packagePrefix;
            var qualifiedName = "%s.%s".formatted(packageName, typeName);
            JavaFileObject builderFile;
            try {
                builderFile = processingEnvironment.getFiler()
                        .createSourceFile(qualifiedName);
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
                processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                return;
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
            throw new RuntimeException(e);
        }
    }

    private boolean wasGeneratedByProcessor(String qualifiedName) {
        synchronized (PROCESSOR_GENERATED_FILES) {
            return PROCESSOR_GENERATED_FILES
                    .getOrDefault(processingEnvironment, Set.of())
                    .contains(qualifiedName);
        }
    }

    private void markAsGeneratedByProcessor(String qualifiedName) {
        synchronized (PROCESSOR_GENERATED_FILES) {
            PROCESSOR_GENERATED_FILES
                    .computeIfAbsent(processingEnvironment, k -> new HashSet<>())
                    .add(qualifiedName);
        }
    }

    private static boolean indicatesFileAlreadyExists(FilerException e) {
        return e.getMessage() != null && e.getMessage().startsWith("Attempt to recreate a file for type");
    }
}
