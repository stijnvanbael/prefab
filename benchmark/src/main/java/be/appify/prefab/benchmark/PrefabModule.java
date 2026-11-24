package be.appify.prefab.benchmark;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * Prefab modules a benchmark task's starting project depends on, in addition to {@code prefab-core},
 * {@code prefab-annotation-processor} and {@code prefab-test}, which every project gets.
 */
enum PrefabModule {
    POSTGRES("prefab-postgres", "compile"),
    MONGODB("prefab-mongodb", "compile"),
    KAFKA("prefab-kafka", "compile"),
    AVRO("prefab-avro", "compile"),
    AVRO_PROCESSOR("prefab-avro-processor", "provided"),
    SECURITY("prefab-security", "compile"),
    OPENAPI("prefab-openapi", "compile"),
    STREAMS("prefab-streams", "compile"),
    STREAMS_ENGINE("prefab-streams-engine", "compile");

    private final String artifactId;
    private final String scope;

    PrefabModule(String artifactId, String scope) {
        this.artifactId = artifactId;
        this.scope = scope;
    }

    @JsonCreator
    static PrefabModule fromName(String name) {
        return valueOf(name.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }

    String dependencyXml() {
        var scopeXml = "compile".equals(scope) ? "" : "\n            <scope>" + scope + "</scope>";
        return """
                        <dependency>
                            <groupId>be.appify.prefab</groupId>
                            <artifactId>%s</artifactId>
                            <version>${prefab.version}</version>%s
                        </dependency>
                """.formatted(artifactId, scopeXml);
    }
}
