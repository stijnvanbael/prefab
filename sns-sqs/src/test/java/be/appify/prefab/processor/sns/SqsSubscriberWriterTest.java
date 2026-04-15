package be.appify.prefab.processor.sns;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.sns.ProcessorTestUtil.contentsOf;
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
        assertThat(compilation).generatedSourceFile("sns.single.infrastructure.sns.UserExporterSqsSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/sns/single/UserExporterSqsSubscriber.java"));
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
        assertThat(compilation).generatedSourceFile(
                        "sns.multiple.infrastructure.sns.UserExporterSqsSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/sns/multiple/UserExporterSqsSubscriber.java"));
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
        assertThat(compilation).generatedSourceFile("sns.multitopic.infrastructure.sns.DayTotalSqsSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/sns/multitopic/DayTotalSqsSubscriber.java"));
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
        assertThat(compilation).generatedSourceFile(
                        "sns.customdlt.infrastructure.sns.UserExporterSqsSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/sns/customdlt/UserExporterSqsSubscriber.java"));
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
        assertThat(compilation).generatedSourceFile(
                        "sns.dltdisabled.infrastructure.sns.UserExporterSqsSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/sns/dltdisabled/UserExporterSqsSubscriber.java"));
    }

    @Test
    void aggregateCreateOrUpdateHandler() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/createorupdate/ChannelSummary.java"),
                        sourceOf("sns/createorupdate/MessageEvent.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile(
                        "sns.createorupdate.infrastructure.sns.ChannelSummarySqsSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/sns/createorupdate/ChannelSummarySqsSubscriber.java"));
    }
}
