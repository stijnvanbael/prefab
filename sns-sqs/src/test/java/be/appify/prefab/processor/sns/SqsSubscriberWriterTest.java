package be.appify.prefab.processor.sns;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.sns.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.processor.sns.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class SqsSubscriberWriterTest {
    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/single/User.java"),
                        sourceOf("sns/single/UserCreated.java"),
                        sourceOf("sns/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.single.infrastructure.sns.UserExporterSqsSubscriber",
                "expected/sns/single/UserExporterSqsSubscriber.java");
    }

    @Test
    void multipleEventTypes() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/multiple/User.java"),
                        sourceOf("sns/multiple/UserEvent.java"),
                        sourceOf("sns/multiple/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.multiple.infrastructure.sns.UserExporterSqsSubscriber",
                "expected/sns/multiple/UserExporterSqsSubscriber.java");
    }

    @Test
    void noParentEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/noparent/User.java"),
                        sourceOf("sns/noparent/UserEvent.java"),
                        sourceOf("sns/noparent/UserExporter.java"));
        assertThat(compilation).hadErrorContaining("share the same topic [user] but have no common ancestor");
    }

    @Test
    void multipleTopics() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/multitopic/Sale.java"),
                        sourceOf("sns/multitopic/Refund.java"),
                        sourceOf("sns/multitopic/DayTotal.java"),
                        sourceOf("sns/multitopic/DayTotalRepositoryMixin.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.multitopic.infrastructure.sns.DayTotalSqsSubscriber",
                "expected/sns/multitopic/DayTotalSqsSubscriber.java");
    }

    @Test
    void customDlt() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/customdlt/User.java"),
                        sourceOf("sns/customdlt/UserEvent.java"),
                        sourceOf("sns/customdlt/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.customdlt.infrastructure.sns.UserExporterSqsSubscriber",
                "expected/sns/customdlt/UserExporterSqsSubscriber.java");
    }

    @Test
    void dltDisabled() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/dltdisabled/User.java"),
                        sourceOf("sns/dltdisabled/UserEvent.java"),
                        sourceOf("sns/dltdisabled/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.dltdisabled.infrastructure.sns.UserExporterSqsSubscriber",
                "expected/sns/dltdisabled/UserExporterSqsSubscriber.java");
    }

    @Test
    void aggregateCreateOrUpdateHandler() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/createorupdate/ChannelSummary.java"),
                        sourceOf("sns/createorupdate/MessageEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.createorupdate.infrastructure.sns.ChannelSummarySqsSubscriber",
                "expected/sns/createorupdate/ChannelSummarySqsSubscriber.java");
    }
}
