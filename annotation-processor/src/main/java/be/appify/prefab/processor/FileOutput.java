package be.appify.prefab.processor;

import com.palantir.javapoet.TypeSpec;
import javax.lang.model.element.TypeElement;

/**
 * Abstraction for writing generated Java types to a concrete output destination.
 */
public interface FileOutput {
    default void setPreferredElement(TypeElement element) {
        // Most outputs do not resolve filesystem roots and can ignore this hint.
    }

    void writeFile(String packagePrefix, String typeName, TypeSpec type);
}

