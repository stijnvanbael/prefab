package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.CaseUtil.toCamelCase;
import static be.appify.prefab.processor.CaseUtil.toPascalCase;

class SerializationRegistryConfigurationWriter {
    private final PrefabContext context;

    SerializationRegistryConfigurationWriter(PrefabContext context) {
        this.context = context;
    }

    void writeConfigurationForPackage(String eventPackage, List<TypeManifest> events) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.event");
        var className = configurationClassName(eventPackage);

        var eventConfigs = events.stream()
                .map(event -> event.inheritedAnnotationsOfType(Event.class).stream().findFirst().orElseThrow())
                .map(event -> new EventConfig(event.topic(), event.serialization()))
                .distinct()
                .toList();

        var placeholderConfigs = eventConfigs.stream()
                .filter(e -> e.topic().matches("\\$\\{.+}"))
                .toList();

        var typeBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addSuperinterface(EventRegistryCustomizer.class);

        // Add fields and constructor for placeholder topics
        if (!placeholderConfigs.isEmpty()) {
            var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
            for (var config : placeholderConfigs) {
                var fieldName = topicFieldName(config.topic());
                typeBuilder.addField(FieldSpec.builder(String.class, fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                constructor.addParameter(ParameterSpec.builder(String.class, fieldName)
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", config.topic())
                                .build())
                        .build());
                constructor.addStatement("this.$L = $L", fieldName, fieldName);
            }
            typeBuilder.addMethod(constructor.build());
        }

        typeBuilder.addMethod(customizeMethod(eventConfigs));

        fileWriter.writeFile(eventPackage, className, typeBuilder.build());
    }

    private static String configurationClassName(String eventPackage) {
        return toPascalCase(eventPackage) + "SerializationRegistryConfiguration";
    }

    private static String topicFieldName(String topicPlaceholder) {
        return toCamelCase(topicPlaceholder.replaceAll("[^\\w._]", ""));
    }

    private static MethodSpec customizeMethod(List<EventConfig> eventConfigs) {
        var method = MethodSpec.methodBuilder("customize")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(EventRegistry.class, "registry");

        eventConfigs.forEach(event -> {
            if (event.topic().matches("\\$\\{.+}")) {
                var fieldName = topicFieldName(event.topic());
                method.addStatement("registry.register($L, $T.$L)",
                        fieldName,
                        Event.Serialization.class,
                        event.serialization.toString());
            } else {
                method.addStatement("registry.register($S, $T.$L)",
                        event.topic(),
                        Event.Serialization.class,
                        event.serialization.toString());
            }
        });

        return method.build();
    }

    private record EventConfig(String topic, Event.Serialization serialization) {
    }
}
