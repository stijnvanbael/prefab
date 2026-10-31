package be.appify.prefab.processor.terraform.gcp;

import be.appify.prefab.processor.PrefabProcessor;
import be.appify.prefab.core.serialization.EventSubscriptionNames;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class GcpTerraformWriterTest {

    @Test
    void genericBindingsProvisionEveryTopicAndExactHandlerSubscriptions() {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("test.Changed", """
                        package test;
                        import be.appify.prefab.core.annotations.*;
                        @Event(topic = {"shared", "text-only"}, typeArguments = String.class, platform = Event.Platform.PUB_SUB)
                        @Event(topic = "shared", typeArguments = Integer.class, platform = Event.Platform.PUB_SUB)
                        @Event(topic = "publisher-only", typeArguments = Long.class, platform = Event.Platform.PUB_SUB)
                        @Event(topic = "kafka-only", typeArguments = Double.class, platform = Event.Platform.KAFKA)
                        public record Changed<T>(T payload) {}
                        """),
                JavaFileObjects.forSourceString("test.Handler", """
                        package test;
                        import be.appify.prefab.core.annotations.EventHandler;
                        @org.springframework.stereotype.Component
                        public class Handler {
                            @EventHandler public void text(Changed<String> event) {}
                            @EventHandler public void number(Changed<Integer> event) {}
                        }
                        """));
        assertThat(compilation).succeeded();
        for (var topic : new String[]{"shared", "text-only", "publisher-only"}) {
            var label = EventSubscriptionNames.name("terraform", "google_pubsub_topic", topic).replace('-', '_');
            assertThat(compilation).generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/" + label + ".tf")
                    .contentsAsUtf8String().contains("name = \"" + topic + "\"");
        }
        for (var binding : new String[]{"java.lang.String", "java.lang.Integer"}) {
            var name = EventSubscriptionNames.name("test.Handler", "test.Changed<" + binding + ">", "shared");
            assertThat(compilation).generatedFile(StandardLocation.CLASS_OUTPUT, "",
                            "terraform/gcp/" + name.replace('-', '_') + ".tf")
                    .contentsAsUtf8String().contains("name  = \"" + name + "\"");
        }
        var files = compilation.generatedFiles().stream()
                .filter(file -> file.getName().contains("terraform/gcp/") && file.getName().endsWith(".tf"))
                .map(file -> {
                    try {
                        return file.getCharContent(true).toString();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }).collect(java.util.stream.Collectors.joining());
        org.junit.jupiter.api.Assertions.assertFalse(files.contains("kafka-only"));
        org.junit.jupiter.api.Assertions.assertEquals(3, files.split("resource \"google_pubsub_topic\"", -1).length - 1);
        org.junit.jupiter.api.Assertions.assertEquals(3, files.split("resource \"google_pubsub_subscription\"", -1).length - 1);
    }

    @Test
    void genericTopicExpressionsDoNotBecomeInvalidTerraformLiterals() {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("test.Configured", """
                        package test;
                        import be.appify.prefab.core.annotations.Event;
                        @Event(topic = "${topics.dynamic:dynamic}", typeArguments = String.class, platform = Event.Platform.PUB_SUB)
                        public record Configured<T>(T payload) {}
                        """));
        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("is provisioned at runtime, not in Terraform");
        assertThat(compilation).generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/pubsub.tf")
                .contentsAsUtf8String().doesNotContain("${topics.");
    }

    @Test
    void baseFilesAlwaysGenerated() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("terraform/source/User.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/main.tf")
                .isNotNull();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/variables.tf")
                .isNotNull();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/outputs.tf")
                .isNotNull();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/cloud_run.tf")
                .isNotNull();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/artifact_registry.tf")
                .isNotNull();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/iam.tf")
                .isNotNull();
    }

    @Test
    void cloudSqlGeneratedWhenPostgresOnClasspath() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("terraform/source/User.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/cloud_sql.tf")
                .contentsAsUtf8String()
                .contains("google_sql_database_instance");
    }

    @Test
    void vpcGeneratedAlongsideCloudSql() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("terraform/source/User.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/vpc.tf")
                .contentsAsUtf8String()
                .contains("google_compute_network");
    }

    @Test
    void restEndpointsGenerateLoadBalancer() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("terraform/source/UserWithRest.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/load_balancer.tf")
                .contentsAsUtf8String()
                .contains("google_compute_global_forwarding_rule");
    }

    private static JavaFileObject sourceOf(String name) throws IOException {
        var resource = new ClassPathResource(name).getURL();
        return JavaFileObjects.forResource(resource);
    }
}
