package be.appify.prefab.streams.mongodb;

import be.appify.prefab.streams.engine.JsonStreamStateCodec;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MongoStandaloneStartupTest {
    private static final String IMAGE = "mongo:9";

    @Test
    void startupFailsClearlyOnAStandaloneServer() {
        try (var standalone = new StandaloneMongo()) {
            standalone.start();
            var template = new MongoTemplate(new SimpleMongoClientDatabaseFactory(
                    "mongodb://" + standalone.getHost() + ":" + standalone.getFirstMappedPort() + "/standalone"));

            assertThatThrownBy(() -> new MongoStreamStateRepository(template, new JsonStreamStateCodec()))
                    .isInstanceOf(MongoTransactionsUnavailableException.class)
                    .hasMessageContaining("standalone")
                    .hasMessageContaining("transactions");
        }
    }

    @Test
    void failureAnalyzerTellsHowToFixIt() {
        var analysis = new MongoTransactionsFailureAnalyzer().analyze(new MongoTransactionsUnavailableException());

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("standalone");
        assertThat(analysis.getAction()).contains("replica set");
    }

    @Test
    void replicaSetIsAccepted() {
        var repository = MongoTestDatabase.newInstance();

        assertThat(repository).isNotNull();
    }

    /** A plain {@code mongod}: unlike {@link MongoDBContainer}, it is not started as a replica set. */
    private static final class StandaloneMongo extends GenericContainer<StandaloneMongo> {
        StandaloneMongo() {
            super(DockerImageName.parse(IMAGE));
            withExposedPorts(27017);
            waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1));
        }
    }
}
