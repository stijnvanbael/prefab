package be.appify.prefab.processor.event.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import io.awspring.cloud.sns.core.SnsTemplate;
import javax.lang.model.element.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static be.appify.prefab.processor.event.sns.SnsPlugin.platformIsSnsSqs;
import static javax.lang.model.element.Modifier.PUBLIC;

class SnsPublisherWriter {
    private final PrefabContext context;

    SnsPublisherWriter(PrefabContext context) {
        this.context = context;
    }

    void writeSnsPublisher(TypeManifest event) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.sns");

        var name = "%sSnsPublisher".formatted(event.simpleName().replace(".", ""));
        var annotation = event.annotationsOfType(Event.class).stream()
                .filter(e -> platformIsSnsSqs(e, event.asElement(), context))
                .findFirst().orElseThrow();
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(Logger.class, "log", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger($T.class)", ClassName.get(LoggerFactory.class),
                                ClassName.get(event.packageName() + ".infrastructure.sns", name))
                        .build())
                .addField(SnsTemplate.class, "snsTemplate", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "topicArn", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(constructor(annotation.topic()))
                .addMethod(publisher(event))
                .build();

        fileWriter.writeFile(event.packageName(), name, type);
    }

    private MethodSpec constructor(String topic) {
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(SnsTemplate.class, "snsTemplate")
                .addParameter(SqsUtil.class, "sqsUtil")
                .addStatement("this.snsTemplate = snsTemplate");
        if (topic.matches("\\$\\{.+}")) {
            constructor.addParameter(ParameterSpec.builder(String.class, "topic")
                            .addAnnotation(AnnotationSpec.builder(Value.class)
                                    .addMember("value", "$S", topic)
                                    .build())
                            .build())
                    .addStatement("this.topicArn = sqsUtil.ensureTopicExists(topic)");
        } else {
            constructor.addStatement("this.topicArn = sqsUtil.ensureTopicExists($S)", topic);
        }
        return constructor.build();
    }

    private MethodSpec publisher(TypeManifest event) {
        return MethodSpec.methodBuilder("publish")
                .addModifiers(PUBLIC)
                .addParameter(event.asTypeName(), "event")
                .addAnnotation(EventListener.class)
                .addStatement("log.debug($S, event, topicArn)", "Publishing event {} on topic {}")
                .addStatement("snsTemplate.sendNotification(topicArn, event, event.getClass().getName())")
                .build();
    }
}
