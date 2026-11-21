package be.appify.prefab.mongodb.spring;

import be.appify.prefab.core.domain.Binary;
import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrefabMongoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PrefabMongoConfiguration.class));

    @Test
    void configuration_withoutPrefabCore_isInactive() {
        runner.run(context -> assertThat(context).doesNotHaveBean(PrefabMongoConfiguration.class));
    }

    @Test
    void configuration_withPrefabCore_isActive() {
        runner.withUserConfiguration(CoreMarker.class)
                .withBean(MongoDatabaseFactory.class, this::databaseFactory)
                .run(context -> assertThat(context).hasSingleBean(PrefabMongoConfiguration.class));
    }

    @Test
    void configuration_withoutSpringDataMongo_isInactive() {
        runner.withUserConfiguration(CoreMarker.class)
                .withClassLoader(new FilteredClassLoader(MongoMappingContext.class))
                .run(context -> assertThat(context).doesNotHaveBean(PrefabMongoConfiguration.class));
    }

    @Test
    void mappingConverter_writesAndReadsBinaryContentAsBytes(@TempDir Path directory) throws IOException {
        var file = Files.write(directory.resolve("image.png"), new byte[] {1, 2, 3}).toFile();
        var binary = new Binary("image.png", "image/png", file);
        runner.withUserConfiguration(CoreMarker.class)
                .withBean(MongoDatabaseFactory.class, this::databaseFactory)
                .run(context -> {
                    var converter = context.getBean(MappingMongoConverter.class);
                    var document = new Document();

                    converter.write(new BinaryHolder(binary), document);
                    var stored = document.get("binary", Document.class);
                    assertThat(stored.get("data")).isEqualTo(new byte[] {1, 2, 3});
                    // The driver reads binary data back as a BSON Binary
                    stored.put("data", new org.bson.types.Binary(stored.get("data", byte[].class)));
                    var read = converter.read(BinaryHolder.class, document).binary();

                    assertThat(read.name()).isEqualTo("image.png");
                    assertThat(read.contentType()).isEqualTo("image/png");
                    assertThat(Files.readAllBytes(read.data().toPath())).containsExactly(1, 2, 3);
                });
    }

    record BinaryHolder(Binary binary) {
    }

    private MongoDatabaseFactory databaseFactory() {
        var factory = mock(MongoDatabaseFactory.class);
        when(factory.getExceptionTranslator()).thenReturn(new MongoExceptionTranslator());
        return factory;
    }

    @Configuration(proxyBeanMethods = false)
    static class CoreMarker {
        @Bean
        PrefabCoreConfiguration prefabCoreConfiguration() {
            return new PrefabCoreConfiguration();
        }
    }
}