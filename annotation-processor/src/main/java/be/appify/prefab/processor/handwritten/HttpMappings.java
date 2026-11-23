package be.appify.prefab.processor.handwritten;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Reads the HTTP mappings of a hand-written Spring MVC controller.
 */
final class HttpMappings {
    private static final String SPRING_WEB = "org.springframework.web.bind.annotation.";
    private static final String REQUEST_MAPPING = SPRING_WEB + "RequestMapping";
    private static final Map<String, String> SHORTCUTS = Map.of(
            SPRING_WEB + "GetMapping", "GET",
            SPRING_WEB + "PostMapping", "POST",
            SPRING_WEB + "PutMapping", "PUT",
            SPRING_WEB + "DeleteMapping", "DELETE",
            SPRING_WEB + "PatchMapping", "PATCH");
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^}]*}");

    /**
     * One mapped endpoint of a hand-written controller.
     *
     * @param handler
     *         the controller method
     * @param method
     *         the HTTP method, or {@code null} when the mapping accepts any method
     * @param path
     *         the full path with path variables normalised to {@code {}}
     */
    record Mapping(ExecutableElement handler, String method, String path) {
        boolean matches(String otherMethod, String otherPath) {
            return path.equals(normalise(otherPath))
                    && (method == null || method.equalsIgnoreCase(otherMethod));
        }
    }

    private HttpMappings() {
    }

    static boolean isController(TypeElement type) {
        return Annotations.hasAny(type, List.of(
                SPRING_WEB + "RestController", "org.springframework.stereotype.Controller"));
    }

    static List<Mapping> of(TypeElement controller) {
        var classPaths = Annotations.find(controller, REQUEST_MAPPING)
                .map(HttpMappings::pathsOf)
                .orElse(List.of(""));
        var mappings = new ArrayList<Mapping>();
        controller.getEnclosedElements().stream()
                .filter(member -> member.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .forEach(handler -> mappings.addAll(mappingsOf(handler, classPaths)));
        return List.copyOf(mappings);
    }

    static String normalise(String path) {
        var withSlash = ("/" + path).replaceAll("/{2,}", "/");
        var trimmed = withSlash.length() > 1 && withSlash.endsWith("/")
                ? withSlash.substring(0, withSlash.length() - 1) : withSlash;
        return PATH_VARIABLE.matcher(trimmed).replaceAll("{}");
    }

    private static List<Mapping> mappingsOf(ExecutableElement handler, List<String> classPaths) {
        var mappings = new ArrayList<Mapping>();
        SHORTCUTS.forEach((annotation, method) -> Annotations.find(handler, annotation)
                .ifPresent(mirror -> mappings.addAll(expand(handler, List.of(method), classPaths, mirror))));
        Annotations.find(handler, REQUEST_MAPPING).ifPresent(mirror -> {
            var methods = Annotations.values(mirror, "method").stream()
                    .map(name -> name.toUpperCase(Locale.ROOT))
                    .toList();
            mappings.addAll(expand(handler, methods.isEmpty() ? Collections.singletonList(null) : methods,
                    classPaths, mirror));
        });
        return mappings;
    }

    private static List<Mapping> expand(
            ExecutableElement handler,
            List<String> methods,
            List<String> classPaths,
            AnnotationMirror mirror
    ) {
        var methodPaths = pathsOf(mirror);
        return methods.stream()
                .flatMap(method -> classPaths.stream().flatMap(classPath -> methodPaths.stream()
                        .map(methodPath -> new Mapping(handler, method, normalise(classPath + "/" + methodPath)))))
                .toList();
    }

    private static List<String> pathsOf(AnnotationMirror mirror) {
        var paths = new ArrayList<>(Annotations.values(mirror, "value"));
        paths.addAll(Annotations.values(mirror, "path"));
        return paths.isEmpty() ? List.of("") : List.copyOf(paths);
    }
}
