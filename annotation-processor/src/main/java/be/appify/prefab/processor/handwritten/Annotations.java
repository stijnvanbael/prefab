package be.appify.prefab.processor.handwritten;

import java.util.List;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Reads annotations by qualified name, so the checks work for annotations (Spring Web, JPA, ...) that are not on the
 * annotation processor's own class path.
 */
final class Annotations {
    /** Value of {@code @SuppressWarnings} that silences the hand-written code checks for one element. */
    static final String SUPPRESSION = "prefab.handwritten";

    private Annotations() {
    }

    static Optional<AnnotationMirror> find(Element element, String qualifiedName) {
        return element.getAnnotationMirrors().stream()
                .<AnnotationMirror>map(mirror -> mirror)
                .filter(mirror -> mirror.getAnnotationType().asElement() instanceof TypeElement type
                        && type.getQualifiedName().contentEquals(qualifiedName))
                .findFirst();
    }

    static boolean hasAny(Element element, List<String> qualifiedNames) {
        return qualifiedNames.stream().anyMatch(name -> find(element, name).isPresent());
    }

    /**
     * Reads a {@code String} or {@code String[]} attribute. Enum constants are returned by their simple name.
     */
    static List<String> values(AnnotationMirror mirror, String attribute) {
        return mirror.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().contentEquals(attribute))
                .map(entry -> entry.getValue().getValue())
                .findFirst()
                .map(Annotations::flatten)
                .orElse(List.of());
    }

    /** Whether this element, or an element it is nested in, opts out with {@code @SuppressWarnings}. */
    static boolean isSuppressed(Element element) {
        for (var current = element; current != null; current = current.getEnclosingElement()) {
            var suppressed = find(current, "java.lang.SuppressWarnings")
                    .map(mirror -> values(mirror, "value"))
                    .orElse(List.of());
            if (suppressed.contains(SUPPRESSION)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> flatten(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> item instanceof AnnotationValue annotationValue ? annotationValue.getValue() : item)
                    .map(Annotations::asText)
                    .toList();
        }
        return List.of(asText(value));
    }

    private static String asText(Object value) {
        return value instanceof VariableElement constant ? constant.getSimpleName().toString() : String.valueOf(value);
    }
}
