package be.appify.prefab.mongodb.diagnostics;

import be.appify.prefab.core.spring.EnablePrefab;
import be.appify.prefab.mongodb.spring.PrefabMongoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import static be.appify.prefab.mongodb.diagnostics.StartupFailures.startExpectingFailure;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class StartupDiagnosticsTest {

    @Test
    void missingMongoBeans(CapturedOutput output) {
        startExpectingFailure(ApplicationWithoutMongo.class);

        assertThat(output)
                .contains("prefab-mongodb needs MongoDB beans")
                .contains("spring.mongodb.uri (mongodb://localhost:27017/<database>)");
    }

    @Test
    void unreachableDatabase(CapturedOutput output) {
        startExpectingFailure(ApplicationUsingMongoAtStartup.class,
                "spring.mongodb.uri=mongodb://app:secret@localhost:1/app?serverSelectionTimeoutMS=300");

        assertThat(output)
                .contains("The application could not connect to MongoDB within the server selection timeout")
                .contains("spring.mongodb.uri are correct")
                .doesNotContain("secret");
    }

    @Test
    void invalidConnectionString(CapturedOutput output) {
        startExpectingFailure(ApplicationUsingMongoAtStartup.class, "spring.mongodb.uri=app:secret@localhost");

        assertThat(output)
                .contains("The MongoDB connection string in spring.mongodb.uri is invalid")
                .contains("mongodb+srv://")
                .doesNotContain("secret");
    }

    @Configuration(proxyBeanMethods = false)
    @EnablePrefab
    @Import(PrefabMongoConfiguration.class)
    @ImportAutoConfiguration(JacksonAutoConfiguration.class)
    static class ApplicationWithoutMongo {
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({MongoAutoConfiguration.class, DataMongoAutoConfiguration.class})
    static class ApplicationUsingMongoAtStartup {
        @Bean
        Object collectionNames(MongoTemplate template) {
            return template.getCollectionNames();
        }
    }
}
