package be.appify.prefab.example.showcase.customer;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.EventVersions;
import be.appify.prefab.core.versioning.MessageVersions;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Event versioning: a message published as version 1 of {@link CustomerRegistered} is migrated to version 2. The test
 * needs no broker, so it runs unchanged on every variant.
 */
class CustomerRegisteredMigrationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final EventMigrator migrator = new EventMigrator(List.of(new CustomerRegisteredMigration()));

    private static MessageVersions producedWith(String version) {
        return MessageVersions.from(name -> EventVersions.EVENT_VERSION_HEADER.equals(name) ? version : null);
    }

    @Test
    void migration_givenFullName_splitsAtTheLastSpace() {
        var old = new CustomerRegisteredV1(Reference.fromId("c-1"), "Ada King Lovelace");

        var migrated = new CustomerRegisteredMigration().migrate(old);

        assertThat(migrated).isEqualTo(new CustomerRegistered(Reference.fromId("c-1"), "Ada King", "Lovelace"));
    }

    @Test
    void migration_givenSingleName_leavesTheLastNameEmpty() {
        var migrated = new CustomerRegisteredMigration().migrate(new CustomerRegisteredV1(Reference.fromId("c-1"), "Madonna"));

        assertThat(migrated).isEqualTo(new CustomerRegistered(Reference.fromId("c-1"), "Madonna", ""));
    }

    @Test
    void plan_givenVersion1Message_readsItThroughTheMigration() {
        var plan = migrator.plan(CustomerRegistered.class, producedWith("1.0.0"));

        var event = plan.apply(mapper.readTree("{\"customer\":\"c-1\",\"name\":\"Ada Lovelace\"}"), mapper);

        assertThat(plan.isRequired()).isTrue();
        assertThat(event).isEqualTo(new CustomerRegistered(Reference.fromId("c-1"), "Ada", "Lovelace"));
    }

    @Test
    void plan_givenCurrentVersionMessage_readsItDirectly() {
        assertThat(migrator.plan(CustomerRegistered.class, producedWith("2.0.0")).isRequired()).isFalse();
    }
}
