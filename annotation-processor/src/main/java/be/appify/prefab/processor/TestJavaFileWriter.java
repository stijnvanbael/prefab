package be.appify.prefab.processor;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class TestJavaFileWriter implements FileOutput {
    private final PrefabContext context;
    private final String packageSuffix;
    /**
     * Optional: a specific TypeElement (always from a source file) to use when resolving the root path.
     */
    private TypeElement preferredElement;

    public TestJavaFileWriter(PrefabContext context, String packageSuffix) {
        this.context = context;
        this.packageSuffix = packageSuffix;
    }

    @Override
    public void setPreferredElement(TypeElement element) {
        this.preferredElement = element;
    }

    @Override
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
        if (manualOverrideExistsInTestSources(rootPath, packageName, typeName)) {
            System.out.printf("Prefab: Skipping generation of %s.%s - manual override found at src/test/java/%s/%s.java%n", packageName, typeName, packageName.replace(".", "/"), typeName);
            return;
        }
        var outputPath = buildGeneratedTestSourcePath(rootPath, packagePrefix);
        if (!isBlank(packageSuffix)) {
            outputPath = outputPath.resolve(packageToPath(packageSuffix));
        }
        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
            var javaSourceFile = createJavaSourceFile(outputPath, typeName);
            if (javaSourceFile.exists()) {
                System.out.println(
                        "Prefab: Skipping generation of %s.%s in target/prefab-test-sources/: file already exists"
                                .formatted(packageName, typeName)
                                + " and is treated as a manual override."
                );
                return;
            }
            writeJavaTestClass(packageName, type, javaSourceFile);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private Path buildGeneratedTestSourcePath(String rootPath, String packagePrefix) {
        return Paths.get(rootPath, "target", "prefab-test-sources")
                .resolve(packageToPath(packagePrefix));
    }

    private boolean manualOverrideExistsInTestSources(String rootPath, String packageName, String typeName) {
        var manualOverridePath = Paths.get(rootPath, "src", "test", "java")
                .resolve(packageToPath(packageName))
                .resolve(typeName + ".java");
        return Files.exists(manualOverridePath);
    }

    private Path packageToPath(String packageName) {
        if (isBlank(packageName)) {
            return Path.of("");
        }
        var path = Path.of("");
        for (var part : packageName.split("\\.")) {
            if (!part.isBlank()) {
                path = path.resolve(part);
            }
        }
        return path;
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
        return path.resolve(className + ".java").toFile();
    }

    private void writeJavaTestClass(String packageName, TypeSpec type, File javaSourceFile) throws IOException {
        try (var writer = Files.newBufferedWriter(javaSourceFile.toPath())) {
            JavaFile.builder(packageName, type)
                    .indent("    ")
                    .build()
                    .writeTo(writer);
        }
    }

    public Optional<String> getRootPath() {
        if (context == null) {
            return Optional.empty();
        }
        var candidates = preferredElement != null
                ? Stream.concat(
                Stream.of(preferredElement),
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
            return rootPathFromSourcePath(sourcePath);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    static Optional<String> rootPathFromSourcePath(String sourcePath) {
        var normalized = sourcePath.replace('\\', '/');
        if (hasWindowsUriDrivePrefix(normalized)) {
            normalized = normalized.substring(1);
        }
        var mainSourceMarker = "/src/main/java";
        if (normalized.contains(mainSourceMarker)) {
            return Optional.of(normalized.substring(0, normalized.indexOf(mainSourceMarker)));
        }
        var testSourceMarker = "/src/test/java";
        if (normalized.contains(testSourceMarker)) {
            return Optional.of(normalized.substring(0, normalized.indexOf(testSourceMarker)));
        }
        return Optional.empty();
    }

    private static boolean hasWindowsUriDrivePrefix(String sourcePath) {
        return sourcePath.length() > 3
                && sourcePath.charAt(0) == '/'
                && Character.isLetter(sourcePath.charAt(1))
                && sourcePath.charAt(2) == ':'
                && sourcePath.charAt(3) == '/';
    }
}