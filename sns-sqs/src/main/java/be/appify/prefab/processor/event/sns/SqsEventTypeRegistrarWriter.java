package be.appify.prefab.processor.event.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.sns.SnsPlugin.platformIsSnsSqs;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class SqsEventTypeRegistrarWriter {
    private final PrefabContext context;

    SqsEventTypeRegistrarWriter(PrefabContext context) {
        this.context = context;
    }

    void writeRegistrar(TypeManifest event) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.sns");
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> platformIsSnsSqs(e, event.asElement(), context))
                .findFirst()
                .orElseThrow();
        var simpleName = event.simpleName().replace(".", "");
        var name = "%sSqsEventTypeRegistrar".formatted(simpleName);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(PUBLIC)
                .addAnnotation(Component.class)
                .addMethod(constructor(annotation.topic(), simpleName, event))
                .build();
        fileWriter.writeFile(event.packageName(), name, type);
    }

    private MethodSpec constructor(String topic, String simpleName, TypeManifest event) {
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(SqsUtil.class, "sqsUtil");
        if (topic.matches("\\$\\{.+}")) {
            var topicFieldName = uncapitalize(simpleName) + "Topic";
            constructor.addParameter(ParameterSpec.builder(String.class, topicFieldName)
                    .addAnnotation(AnnotationSpec.builder(Value.class)
                            .addMember("value", "$S", topic)
                            .build())
                    .build());
            constructor.addStatement("sqsUtil.registerType($T.class.getName(), $T.class)", event.asTypeName(), event.asTypeName());
            constructor.addStatement("sqsUtil.registerEventTopic($L, $T.class)", topicFieldName, event.asTypeName());
        } else {
            constructor.addStatement("sqsUtil.registerType($T.class.getName(), $T.class)", event.asTypeName(), event.asTypeName());
            constructor.addStatement("sqsUtil.registerEventTopic($S, $T.class)", topic, event.asTypeName());
        }
        return constructor.build();
    }
}

