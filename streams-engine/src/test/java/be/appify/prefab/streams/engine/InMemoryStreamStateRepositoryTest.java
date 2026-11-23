package be.appify.prefab.streams.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryStreamStateRepositoryTest extends StreamStateRepositoryContractTest {
    @Override
    protected StreamStateRepository createRepository() {
        return new InMemoryStreamStateRepository();
    }

    @Test
    void readExposesCurrentValueForLocalAssertions() {
        var repository = new InMemoryStreamStateRepository();
        var transaction = repository.beginTransaction();
        transaction.put("counts", "key", 3);
        transaction.commit();

        assertThat(repository.read("counts", "key")).contains(3);
    }
}
