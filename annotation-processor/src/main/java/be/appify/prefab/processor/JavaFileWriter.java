package be.appify.prefab.processor;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class JavaFileWriter {
    private final ProcessingEnvironment processingEnvironment;
    private final String packageSuffix;

    public JavaFileWriter(ProcessingEnvironment processingEnvironment, String packageSuffix) {
        this.processingEnvironment = processingEnvironment;
        this.packageSuffix = packageSuffix;
    }

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
