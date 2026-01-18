package be.appify.prefab.processor.event.kafka;

import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.core.kafka.KafkaUtil;
import be.appify.prefab.core.util.Classes;
import be.appify.prefab.processor.JavaFileWriter;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.google.common.collect.Streams;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.WildcardTypeName;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import static be.appify.prefab.core.annotations.EventHandlerConfig.Util.hasCustomDltTopic;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class KafkaConsumerConfigWriter {
    void writeConsumerConfig(
            TypeManifest owner,
            List<ExecutableElement> eventHandlers,
            PrefabContext context
    ) {
        var fileWriter = new JavaFileWriter(context.processingEnvironment(), "infrastructure.kafka");

        var name = "%sKafkaConsumerConfig".formatted(owner.simpleName());
        var packageName = new TypeManifest(eventHandlers.getFirst().getEnclosingElement().asType(),
                context.processingEnvironment()).packageName();
        var config = owner.inheritedAnnotationsOfType(EventHandlerConfig.class).stream().findFirst().orElseThrow();
        var type = TypeSpec.classBuilder(name)
                .addAnnotation(Configuration.class)
                .addModifiers(PUBLIC)
                .addMethod(dltErrorHandlerMethod(owner, config));
        if (hasCustomDltTopic(config)) {
            type.addMethod(deadLetterPublishingRecovererMethod(owner, config));
        }
        fileWriter.writeFile(packageName, name, type.build());
    }

    private MethodSpec dltErrorHandlerMethod(TypeManifest owner, EventHandlerConfig config) {
        var method = MethodSpec.methodBuilder("%sKafkaErrorHandler".formatted(uncapitalize(owner.simpleName())))
                .addAnnotation(Bean.class)
                .addAnnotation(AnnotationSpec.builder(Qualifier.class)
                        .addMember("value", "$S", "%sKafkaErrorHandler".formatted(uncapitalize(owner.simpleName())))
                        .build())
                .returns(CommonErrorHandler.class)
                .addParameter(configParameter(Integer.class, "maxRetries", "${prefab.kafka.consumer.dlt.max-retries:5}"))
                .addParameter(configParameter(Long.class, "initialRetryInterval", "${prefab.dlt.retries.initial-interval-ms:1000}"))
                .addParameter(configParameter(Float.class, "backoffMultiplier", "${prefab.dlt.retries.multiplier:1.5}"))
                .addParameter(configParameter(Long.class, "maxRetryInterval", "${prefab.dlt.retries.max-interval-ms:30000}"))
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(List.class, String.class), "nonRetryableExceptions")
                        .addAnnotation(AnnotationSpec.builder(Value.class)
                                .addMember("value", "$S", "#{'${prefab.dlt.non-retryable-exceptions:}'.split(',')}")
                                .build())
                        .build());
        if (config.deadLetteringEnabled()) {
            if (hasCustomDltTopic(config)) {
                method.addParameter(ParameterSpec.builder(DeadLetterPublishingRecoverer.class, "deadLetteringRecoverer")
                        .addAnnotation(AnnotationSpec.builder(Qualifier.class)
                                .addMember("value", "$S", "%sDeadLetterPublishingRecoverer".formatted(uncapitalize(owner.simpleName())))
                                .build())
                        .build());
            } else {
                method.addParameter(DeadLetterPublishingRecoverer.class, "deadLetteringRecoverer");
            }
        }
        method.addStatement("var backoff = new $T(maxRetries)", ExponentialBackOffWithMaxRetries.class)
                .addStatement("backoff.setInitialInterval(initialRetryInterval)")
                .addStatement("backoff.setMultiplier(backoffMultiplier)")
                .addStatement("backoff.setMaxInterval(maxRetryInterval)");
        if (config.deadLetteringEnabled()) {
            method.addStatement("var errorHandler = new $T(deadLetteringRecoverer, backoff)", DefaultErrorHandler.class)
                    .addStatement("""
                            var customExceptions = nonRetryableExceptions.stream()
                            .filter(name -> !name.isBlank())
                            .map($T::classWithName)""",
                            Classes.class)
                    .addStatement("""
                                    var notRetryable = $T.concat($T.DEFAULT_NOT_RETRYABLE.stream(), customExceptions)
                                    .toArray($T[]::new)""",
                            Streams.class,
                            KafkaUtil.class,
                            Class.class)
                    .addStatement("errorHandler.addNotRetryableExceptions(notRetryable)")
                    .addStatement("return errorHandler");
        } else {
            method.addStatement("return new $T(backoff)", DefaultErrorHandler.class);
        }
        return method.build();
    }

    private MethodSpec deadLetterPublishingRecovererMethod(TypeManifest owner, EventHandlerConfig config) {
        return MethodSpec.methodBuilder("%sDeadLetterPublishingRecoverer".formatted(uncapitalize(owner.simpleName())))
                .addAnnotation(Bean.class)
                .addAnnotation(AnnotationSpec.builder(Qualifier.class)
                        .addMember("value", "$S", "%sDeadLetterPublishingRecoverer".formatted(uncapitalize(owner.simpleName())))
                        .build())
                .returns(DeadLetterPublishingRecoverer.class)
                .addParameter(ParameterizedTypeName.get(
                                ClassName.get(KafkaTemplate.class),
                                WildcardTypeName.subtypeOf(Object.class),
                                WildcardTypeName.subtypeOf(Object.class)),
                        "kafkaTemplate")
                .addStatement("""
                                return new $T(
                                    $T.of($T.class, kafkaTemplate),
                                    (record, ex) -> new $T($S, -1))""",
                        DeadLetterPublishingRecoverer.class,
                        Map.class,
                        Object.class,
                        TopicPartition.class,
                        config.deadLetterTopic())
                .build();
    }

    private static ParameterSpec configParameter(Class<?> type, String name, String value) {
        return ParameterSpec.builder(type, name)
                .addAnnotation(AnnotationSpec.builder(Value.class)
                        .addMember("value", "$S", value)
                        .build())
                .build();
    }
}
