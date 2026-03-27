package be.appify.prefab.test.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway migration strategy that, during tests, automatically drops the schema and retries if there is a checksum
 * mismatch for the last applied migration.
 * <p>
 * This supports the common development scenario where a developer modifies the last migration script and then runs
 * tests. Without this strategy, the test run would fail with a checksum mismatch error.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Flyway.class)
public class FlywayChecksumMismatchMigrationStrategy {

    /** Constructs a new FlywayChecksumMismatchMigrationStrategy. */
    public FlywayChecksumMismatchMigrationStrategy() {
    }

    /**
     * Returns a {@link FlywayMigrationStrategy} that drops the schema and retries if there is a checksum mismatch for
     * the last applied migration.
     *
     * @return the migration strategy
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return new ChecksumMismatchMigrationStrategy();
    }

    private static class ChecksumMismatchMigrationStrategy implements FlywayMigrationStrategy {
        private static final Logger log = LoggerFactory.getLogger(ChecksumMismatchMigrationStrategy.class);

        @Override
        public void migrate(Flyway flyway) {
            if (hasChecksumMismatchForLastMigration(flyway)) {
                log.warn("Checksum mismatch detected for last Flyway migration. Dropping schema and retrying...");
                flyway.clean();
            }
            flyway.migrate();
        }

        private boolean hasChecksumMismatchForLastMigration(Flyway flyway) {
            MigrationInfo[] applied = flyway.info().applied();
            if (applied.length == 0) {
                return false;
            }
            MigrationInfo lastApplied = applied[applied.length - 1];
            return !lastApplied.isChecksumMatching();
        }
    }
}
