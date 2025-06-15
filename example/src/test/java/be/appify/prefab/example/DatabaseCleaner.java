package be.appify.prefab.example;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseCleaner.class);
    private final Flyway flyway;

    public DatabaseCleaner(Flyway flyway) {
        this.flyway = flyway;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent ignoredEvent) {
        try {
            flyway.validate();
        } catch (FlywayValidateException _) {
            log.debug("Database validation failed, cleaning database");
            flyway.clean();
        }
    }
}
