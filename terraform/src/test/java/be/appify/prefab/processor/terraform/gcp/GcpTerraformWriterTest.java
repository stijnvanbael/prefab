package be.appify.prefab.processor.terraform.gcp;

import be.appify.prefab.processor.PrefabProcessor;
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
