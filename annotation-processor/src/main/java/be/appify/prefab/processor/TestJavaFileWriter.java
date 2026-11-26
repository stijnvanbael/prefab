package be.appify.prefab.processor;

import be.appify.prefab.processor.error.PrefabProcessingException;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class TestJavaFileWriter implements FileOutput {
    private static final Pattern SOURCE_ROOT = Pattern.compile("/src/[^/]+/java/");
    private final PrefabContext context;
    private final String packageSuffix;
    private List<TypeElement> originatingElements = List.of();

    public TestJavaFileWriter(PrefabContext context, String packageSuffix) {
        this.context = context;
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

    @Override
    public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
        var packageName = !isBlank(packageSuffix) ? "%s.%s".formatted(packagePrefix, packageSuffix) : packagePrefix;
        var source = JavaFile.builder(packageName, type).indent("    ").build();
        var relativePath = (packageName.isBlank() ? "" : packageName.replace('.', '/') + "/") + typeName + ".java";
        var owners = originatingElements;
        var root = getRootPath();
        Runnable write = () -> {
            try {
                if (root.isPresent()) {
                    writeToFilesystem(Path.of(root.get()), relativePath, source);
                    if (context != null) {
                        context.incrementalState().record(relativePath, owners, context);
                    }
                } else if (context != null) {
                    var resource = context.processingEnvironment().getFiler()
                            .createResource(StandardLocation.CLASS_OUTPUT, "", relativePath, owners.toArray(TypeElement[]::new));
                    try (var writer = resource.openWriter()) {
                        source.writeTo(writer);
                    }
                }
            } catch (IOException e) {
                throw new PrefabProcessingException(owners.stream().findFirst().orElse(null), "Prefab",
                        "cannot write generated test source %s: %s".formatted(relativePath, e.getMessage()),
                        "check that the compiler output directory is writable and has free disk space", e);
            }
        };
        if (context == null || !GeneratedFiles.defer(context.processingEnvironment(), write)) {
            write.run();
        }
    }

    private static void writeToFilesystem(Path root, String relativePath, JavaFile source) throws IOException {
        var output = root.resolve("target/prefab-test-sources").resolve(relativePath);
        if (Files.isRegularFile(root.resolve("src/test/java").resolve(relativePath))) {
            Files.deleteIfExists(output);
            System.out.printf("Prefab: Skipping generation - manual override found at src/test/java/%s%n", relativePath);
            return;
        }
        var contents = source.toString();
        if (!Files.isRegularFile(output) || !Files.readString(output).equals(contents)) {
            Files.createDirectories(output.getParent());
            Files.writeString(output, contents);
        }
    }

    public Optional<String> getRootPath() {
        if (context == null) {
            return Optional.empty();
        }
        return context.incrementalState().root().or(() -> Stream.concat(originatingElements.stream(), context.roundEnvironment().getRootElements().stream()
                        .filter(TypeElement.class::isInstance).map(TypeElement.class::cast))
                .map(element -> sourcePath(context, element).flatMap(path -> rootPathFromSourcePath(path.toString())))
                .flatMap(Optional::stream)
                .findFirst());
    }

    static Optional<Path> sourcePath(PrefabContext context, TypeElement element) {
        var elements = context.processingEnvironment().getElementUtils();
        try {
            var file = elements.getFileObjectOf(element);
            if (file == null || file.getKind() != JavaFileObject.Kind.SOURCE || !"file".equals(file.toUri().getScheme())) {
                return Optional.empty();
            }
            return Optional.of(Paths.get(file.toUri()));
        } catch (UnsupportedOperationException unavailable) {
            // Elements.getFileObjectOf is optional; SOURCE_PATH is only useful for a top-level source.
            var owner = element;
            while (owner.getEnclosingElement() instanceof TypeElement enclosing) {
                owner = enclosing;
            }
            try {
                var uri = context.processingEnvironment().getFiler().getResource(StandardLocation.SOURCE_PATH,
                        elements.getPackageOf(owner).getQualifiedName(), owner.getSimpleName() + ".java").toUri();
                return "file".equals(uri.getScheme()) ? Optional.of(Path.of(uri)) : Optional.empty();
            } catch (IOException missing) {
                return Optional.empty();
            }
        }
    }

    static Optional<String> rootPathFromSourcePath(String sourcePath) {
        var normalized = sourcePath.replace('\\', '/');
        if (hasWindowsUriDrivePrefix(normalized)) {
            normalized = normalized.substring(1);
        }
        var match = SOURCE_ROOT.matcher(normalized);
        return match.find() ? Optional.of(normalized.substring(0, match.start())) : Optional.empty();
    }

    private static boolean hasWindowsUriDrivePrefix(String sourcePath) {
        return sourcePath.length() > 3
                && sourcePath.charAt(0) == '/'
                && Character.isLetter(sourcePath.charAt(1))
                && sourcePath.charAt(2) == ':'
                && sourcePath.charAt(3) == '/';
    }
}