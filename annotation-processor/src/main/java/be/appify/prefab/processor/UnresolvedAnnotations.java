package be.appify.prefab.processor;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Detects annotations whose type, or whose values, javac could not resolve, for example because of a wrong import.
 *
 * <p>The annotation proxies javac hands out for such annotations throw
 * {@link java.lang.annotation.IncompleteAnnotationException} when a member is read. The processor must leave these
 * elements alone: javac then reports its own "cannot find symbol" error, which is the diagnostic the user needs.
 */
final class UnresolvedAnnotations {

    private UnresolvedAnnotations() {
    }

    /**
     * Returns true if the element, one of its members or one of its parameters carries an annotation that is, or
     * contains, an unresolved type.
     *
     * @param element
     *         the element to inspect
     * @return whether an unresolved annotation was found
     */
    static boolean existsIn(Element element) {
        return hasUnresolvedAnnotation(element)
                || element.getEnclosedElements().stream().anyMatch(UnresolvedAnnotations::existsIn)
                || (element instanceof ExecutableElement executable
                && executable.getParameters().stream().anyMatch(UnresolvedAnnotations::hasUnresolvedAnnotation));
    }

    /**
     * Returns true if any annotation directly on the element is, or contains, an unresolved type.
     *
     * @param element
     *         the element to inspect
     * @return whether an unresolved annotation was found
     */
    static boolean hasUnresolvedAnnotation(Element element) {
        return element.getAnnotationMirrors().stream().anyMatch(UnresolvedAnnotations::isUnresolved);
    }

    private static boolean isUnresolved(AnnotationMirror mirror) {
        return mirror.getAnnotationType().getKind() == TypeKind.ERROR
                || mirror.getElementValues().values().stream().anyMatch(UnresolvedAnnotations::isUnresolved);
    }

    private static boolean isUnresolved(AnnotationValue value) {
        return switch (value.getValue()) {
            case AnnotationMirror nested -> isUnresolved(nested);
            case TypeMirror type -> type.getKind() == TypeKind.ERROR;
            case List<?> items -> items.stream()
                    .anyMatch(item -> item instanceof AnnotationValue itemValue && isUnresolved(itemValue));
            default -> value.toString().contains("<error>");
        };
    }
}
