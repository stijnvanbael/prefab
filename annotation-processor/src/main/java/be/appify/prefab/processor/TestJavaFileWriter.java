package be.appify.prefab.processor;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import static org.apache.commons.lang3.StringUtils.isBlank;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class TestJavaFileWriter {
    private final StandardJavaFileManager fileManager = getJavaFileManager();
    private final PrefabContext context;
    private final String packageSuffix;

    public TestJavaFileWriter(PrefabContext context, String packageSuffix) {
        this.context = context;
        this.packageSuffix = packageSuffix;
    }

    public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
        var packageName = !isBlank(packageSuffix) ? "%s.%s".formatted(packagePrefix, packageSuffix) : packagePrefix;
        var rootPath = getRootPath();
        if (rootPath != null) {
            var testSourcePath = rootPath + "/target/prefab-test-sources/"
                                 + packagePrefix.replace(".", "/")
                                 + (packageSuffix != null ? "/" + packageSuffix.replace(".", "/") : "");
            try {
                var outputPath = new File(testSourcePath).toPath();
                if (!Files.exists(outputPath)) {
                    Files.createDirectories(outputPath);
                }

                var javaSourceFile = createJavaSourceFile(outputPath, typeName);
                writeJavaTestClass(packageName, type, javaSourceFile);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static File createJavaSourceFile(Path path, String className) {
        return Paths.get(path.toString(), String.format("%s.java", className)).toFile();
    }

    private void writeJavaTestClass(String packageName, TypeSpec type, File javaSourceFile) throws IOException {
        var javaFileObject = getJavaFileObject(fileManager, javaSourceFile);
        try (var writer = javaFileObject.openWriter()) {
            var javaFile = JavaFile
                    .builder(packageName, type)
                    .indent("    ")
                    .build();
            javaFile.writeTo(writer);
        }
    }

    protected JavaFileObject getJavaFileObject(StandardJavaFileManager fileManager, File javaSrcFile) {
        return fileManager.getJavaFileObjectsFromFiles(List.of(javaSrcFile)).iterator().next();
    }

    private static StandardJavaFileManager getJavaFileManager() {
        return ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, Locale.getDefault(), Charset.defaultCharset());
    }

    public String getRootPath() {
        var element = context.roundEnvironment().getRootElements().stream()
                .filter(e -> e instanceof TypeElement)
                .map(e -> (TypeElement) e)
                .findFirst()
                .orElseThrow();
        var qualifiedName = element.getQualifiedName().toString();
        var packageName = qualifiedName.contains(".")
                ? qualifiedName.substring(0, qualifiedName.lastIndexOf('.'))
                : "";
        var fileName = element.getSimpleName().toString() + ".java";
        try {
            var sourcePath = context.processingEnvironment().getFiler()
                    .getResource(StandardLocation.SOURCE_PATH, packageName, fileName)
                    .toUri().getPath();
            return sourcePath.substring(0, sourcePath.indexOf("/src/main/java"));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

}
