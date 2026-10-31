package be.appify.prefab.avro.processor;

import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.EventBindings;
import com.palantir.javapoet.AnnotationSpec;
import org.springframework.stereotype.Component;

class AvroSupport {
    private AvroSupport() {
    }

    static String generatedName(TypeManifest type) {
        return type.simpleName().replace(".", "") + EventBindings.suffix(type);
    }

    static AnnotationSpec componentAnnotation(TypeManifest event, String name) {
        return AnnotationSpec.builder(Component.class)
                .addMember("value", "$S", event.packageName().replace('.', '_') + "_" + name)
                .build();
    }

    /** Returns whether every dot-separated segment of the name is a valid Avro name, so that it can be an alias. */
    static boolean isAvroName(String name) {
        return name.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
    }
}