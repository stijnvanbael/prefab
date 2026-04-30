package be.appify.prefab.processor;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class TestJavaFileWriter implements TestFileOutput {
    private final StandardJavaFileManager fileManager = getJavaFileManager();
    private final PrefabContext context;
    private final String packageSuffix;
    /** Optional: a specific TypeElement (always from a source file) to use when resolving the root path. */
    private TypeElement preferredElement;

    public TestJavaFileWriter(PrefabContext context, String packageSuffix) {
        this.context = context;
        this.packageSuffix = packageSuffix;
    }

    public void setPreferredElement(TypeElement element) {
        this.preferredElement = element;
    }

    public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
        var packageName = !isBlank(packageSuffix) ? "%s.%s".formatted(packagePrefix, packageSuffix) : packagePrefix;
        var rootPath = getRootPath();
        if (rootPath.isPresent()) {
            writeToFilesystem(rootPath.get(), packagePrefix, packageName, typeName, type);
        } else if (context != null) {
            writeToFiler(packageName, typeName, type);
        }
    }

    private void writeToFilesystem(String rootPath, String packagePrefix, String packageName, String typeName, TypeSpec type) {
        var testSourcePath = rootPath + "/target/prefab-test-sources/"
                + packagePrefix.replace(".", "/")
                + (packageSuffix != null ? "/" + packageSuffix.replace(".", "/") : "");
        try {
            var outputPath = new File(testSourcePath).toPath();
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
            var javaSourceFile = createJavaSourceFile(outputPath, typeName);
            if (isFileOverride(packageName, typeName, javaSourceFile)) {
                return;
            }
            writeJavaTestClass(packageName, type, javaSourceFile);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static boolean isFileOverride(String packageName, String typeName, File javaSourceFile) {
        if (javaSourceFile.exists()) {
            System.out.println(
                    "Prefab: Skipping generation of %s.%s in target/prefab-test-sources/: file already exists"
                            .formatted(packageName, typeName)
                            + " and is treated as a manual override."
            );
            return true;
        }
        return false;
    }

    private void writeToFiler(String packageName, String typeName, TypeSpec type) {
        try {
            var relativePath = packageName.replace(".", "/") + "/" + typeName + ".java";
            var resource = context.processingEnvironment().getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", relativePath);
            try (var writer = resource.openWriter()) {
                JavaFile.builder(packageName, type).indent("    ").build().writeTo(writer);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
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

    public Optional<String> getRootPath() {
        if (context == null) {
            return Optional.empty();
        }
        var candidates = preferredElement != null
                ? Stream.concat(Stream.of(preferredElement),
                        context.roundEnvironment().getRootElements().stream()
                                .filter(TypeElement.class::isInstance)
                                .map(TypeElement.class::cast))
                : context.roundEnvironment().getRootElements().stream()
                        .filter(TypeElement.class::isInstance)
                        .map(TypeElement.class::cast);
        return candidates
                .map(this::tryRootPath)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> tryRootPath(TypeElement element) {
        var qualifiedName = element.getQualifiedName().toString();
        var packageName = qualifiedName.contains(".")
                ? qualifiedName.substring(0, qualifiedName.lastIndexOf('.'))
                : "";
        var fileName = element.getSimpleName() + ".java";
        try {
            var sourcePath = context.processingEnvironment().getFiler()
                    .getResource(StandardLocation.SOURCE_PATH, packageName, fileName)
                    .toUri().getPath();
            if (sourcePath.contains("/src/main/java")) {
                return Optional.of(sourcePath.substring(0, sourcePath.indexOf("/src/main/java")));
            } else if (sourcePath.contains("/src/test/java")) {
                return Optional.of(sourcePath.substring(0, sourcePath.indexOf("/src/test/java")));
            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

}
