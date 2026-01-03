package be.appify.prefab.processor;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Utility class to write Java files using the annotation processing environment.
 */
public class JavaFileWriter {
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
            JavaFileObject builderFile;
            try {
                builderFile = processingEnvironment.getFiler()
                        .createSourceFile("%s.%s".formatted(packageName, typeName));
            } catch (FilerException e) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
