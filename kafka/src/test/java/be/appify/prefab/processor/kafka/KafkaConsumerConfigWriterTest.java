package be.appify.prefab.processor.kafka;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.kafka.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.processor.kafka.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerConfigWriterTest {

    @Test
    void defaultConfigNotGeneratedWithoutDlt() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/single/User.java"),
                        sourceOf("kafka/single/UserCreated.java"),
                        sourceOf("kafka/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        Assertions.assertThrows(AssertionError.class,
                () -> assertThat(compilation).generatedSourceFile("kafka.customdlt.infrastructure.kafka.UserExporterKafkaConsumerConfig"));
    }

    @Test
    void customDeadLetterTopic() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/customdlt/User.java"),
                        sourceOf("kafka/customdlt/UserEvent.java"),
                        sourceOf("kafka/customdlt/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.customdlt.infrastructure.kafka.UserExporterKafkaConsumerConfig");
        // error handler bean named to match the consumer's errorHandler reference
        assertThat(source).contains("@Qualifier(\"userExporterKafkaErrorHandler\")");
        // dead-letter publishing recoverer bean is generated for custom DLT
        assertThat(source).contains("@Qualifier(\"userExporterDeadLetterPublishingRecoverer\")");
        assertThat(source).contains("DeadLetterPublishingRecoverer");
        // custom DLT topic property is wired in
        assertThat(source).contains("${custom.dlt.name}");
    }

    @Test
    void deadLetterTopicDisabled() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/dltdisabled/User.java"),
                        sourceOf("kafka/dltdisabled/UserEvent.java"),
                        sourceOf("kafka/dltdisabled/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "kafka.dltdisabled.infrastructure.kafka.UserExporterKafkaConsumerConfig");
        assertThat(source).contains("@Qualifier(\"userExporterKafkaErrorHandler\")");
        // DLT disabled: no dead-letter publisher, just retry backoff
        assertThat(source).doesNotContain("DeadLetterPublishingRecoverer");
        assertThat(source).contains("DefaultErrorHandler");
        assertThat(source).contains("ExponentialBackOffWithMaxRetries");
    }

    @Test
    void autoOffsetResetOverrideDoesNotGenerateConsumerConfig() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/offsetoverride/User.java"),
                        sourceOf("kafka/offsetoverride/UserCreated.java"),
                        sourceOf("kafka/offsetoverride/UserExporter.java"));
        assertThat(compilation).succeeded();
        Assertions.assertThrows(AssertionError.class,
                () -> assertThat(compilation).generatedSourceFile("kafka.offsetoverride.infrastructure.kafka.UserExporterKafkaConsumerConfig"));
    }
}
