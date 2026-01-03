package be.appify.prefab.test.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test execution listener that cleans up all CrudRepositories before each test instance is prepared.
 */
public class PersistenceTestExecutionListener extends AbstractTestExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(PersistenceTestExecutionListener.class);

    /** Constructs a new PersistenceTestExecutionListener. */
    public PersistenceTestExecutionListener() {
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        var repositories = new ArrayList<>(testContext.getApplicationContext()
                .getBeansOfType(CrudRepository.class)
                .values());
        while (!repositories.isEmpty()) {
            var initialSize = repositories.size();
            AtomicReference<Exception> lastException = new AtomicReference<>();
            repositories.removeIf(repository -> {
                try {
                    repository.deleteAll();
                    return true;
                } catch (Exception e) {
                    lastException.set(e);
                    return false;
                }
            });
            if (repositories.size() == initialSize) {
                log.warn("Could not clean all database tables, possible circular dependency, last exception:",
                        lastException.get());
                break;
            }
        }
    }
}
