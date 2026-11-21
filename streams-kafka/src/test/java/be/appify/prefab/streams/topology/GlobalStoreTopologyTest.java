package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.CustomerProfile;
import be.appify.prefab.streams.topology.TopologyFixtures.CustomerProfileUpdated;
import be.appify.prefab.streams.topology.TopologyFixtures.EnrichOrderWithProfileProcessor;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.OrderCount;
import be.appify.prefab.streams.topology.TopologyFixtures.OrderProfileView;
import be.appify.prefab.streams.topology.TopologyFixtures.UpdateCustomerProfileUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(TopologyTestExtension.class)
class GlobalStoreTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void globalStore_shouldBeMaterializedAsGlobalStoreInTopology() {
        var customerProfiles = streams.globalStore(CustomerProfile.class);

        streams.from(IncomingOrder.class)
                .process(new EnrichOrderWithProfileProcessor(customerProfiles))
                .end();

        assertThat(topology.inspect().globalStoreCount()).isEqualTo(1);
    }

    @Test
    void globalStore_shouldBeReadableDirectlyFromTest() {
        var customerProfiles = streams.globalStore(CustomerProfile.class);
        topology.input(CustomerProfile.class).send(new CustomerProfile(Reference.fromId("alice"), "GOLD"));

        assertThat(customerProfiles.get(Reference.fromId("alice")))
                .hasValueSatisfying(profile -> assertThat(profile.tier()).isEqualTo("GOLD"));
    }
    @Test
    void globalStore_shouldServeRecordsFromSourceTopicToProcessors() {
        var customerProfiles = streams.globalStore(CustomerProfile.class);

        streams.from(IncomingOrder.class)
                .process(new EnrichOrderWithProfileProcessor(customerProfiles))
                .to(OrderProfileView.class);

        var profiles = topology.input(CustomerProfile.class);
        var orders = topology.input(IncomingOrder.class);
        var output = topology.output(OrderProfileView.class);

        orders.send(new IncomingOrder(Reference.fromId("o-0"), Reference.fromId("alice")));
        profiles.send(new CustomerProfile(Reference.fromId("alice"), "SILVER"));
        orders.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
        profiles.send(new CustomerProfile(Reference.fromId("alice"), "GOLD"));
        orders.send(new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("alice")));

        assertThat(output.values())
                .containsExactly(
                        new OrderProfileView(Reference.fromId("o-0"), Reference.fromId("alice"), "UNKNOWN"),
                        new OrderProfileView(Reference.fromId("o-1"), Reference.fromId("alice"), "SILVER"),
                        new OrderProfileView(Reference.fromId("o-2"), Reference.fromId("alice"), "GOLD")
                );
    }

    @Test
    void globalStore_shouldRemoveKeyWhenSourceRecordHasNullValue() {
        var customerProfiles = streams.globalStore(CustomerProfile.class);

        streams.from(IncomingOrder.class)
                .process(new EnrichOrderWithProfileProcessor(customerProfiles))
                .to(OrderProfileView.class);

        var profiles = topology.input(CustomerProfile.class);
        var orders = topology.input(IncomingOrder.class);
        var output = topology.output(OrderProfileView.class);

        profiles.send(new CustomerProfile(Reference.fromId("alice"), "GOLD"));
        profiles.sendTombstone(Reference.fromId("alice"));
        orders.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

        assertThat(output.value().tier()).isEqualTo("UNKNOWN");
    }

    @Test
    void globalStore_shouldReturnSameDeclarationForRepeatedDeclaration() {
        var first = streams.globalStore(CustomerProfile.class);
        var second = streams.globalStore(CustomerProfile.class);

        assertThat(first).isSameAs(second);
    }

    @Test
    void globalStore_shouldMapSourceRecordsToStoreType() {
        var customerProfiles = streams.globalStore(
                CustomerProfileUpdated.class,
                CustomerProfile.class,
                updated -> new CustomerProfile(updated.customerId(), updated.tier()));

        streams.from(IncomingOrder.class)
                .process(new EnrichOrderWithProfileProcessor(customerProfiles))
                .to(OrderProfileView.class);

        var updates = topology.input(CustomerProfileUpdated.class);
        var orders = topology.input(IncomingOrder.class);
        var output = topology.output(OrderProfileView.class);

        updates.send(new CustomerProfileUpdated(Reference.fromId("alice"), "GOLD"));
        orders.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

        assertThat(output.value().tier()).isEqualTo("GOLD");
    }

    @Test
    void globalStore_shouldAllowMultipleStoresOfTheSameStoreTypeWithDifferentNames() {
        var fromProfiles = streams.globalStore("profiles-a", CustomerProfile.class);
        var fromUpdates = streams.globalStore(
                "profiles-b",
                CustomerProfileUpdated.class,
                CustomerProfile.class,
                updated -> new CustomerProfile(updated.customerId(), updated.tier()));

        assertThat(fromProfiles.name()).isEqualTo("global-profiles-a");
        assertThat(fromUpdates.name()).isEqualTo("global-profiles-b");
        streams.from(IncomingOrder.class)
                .process(new EnrichOrderWithProfileProcessor(fromProfiles, fromUpdates))
                .end();
        assertThat(topology.inspect().globalStoreCount()).isEqualTo(2);
    }

    @Test
    void globalStore_shouldFailFastWhenSourceTopicAlreadyFeedsAnotherStore() {
        streams.globalStore("profiles-a", CustomerProfile.class);

        assertThatThrownBy(() -> streams.globalStore("profiles-b", CustomerProfile.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already feeds global store 'global-profiles-a'");
    }

    @Test
    void globalStore_shouldFailFastWhenNameIsReusedForOtherTypes() {
        streams.globalStore("profiles", CustomerProfile.class);

        assertThatThrownBy(() -> streams.globalStore("profiles", OrderCount.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already declared");
    }

    @Test
    void globalStoreFrom_shouldLetUpdaterReadAndWriteTheStoreFromSourceRecords() {
        var customerProfiles = streams.globalStoreFrom(CustomerProfileUpdated.class)
                .updatedBy(UpdateCustomerProfileUpdater::update)
                .as(CustomerProfile.class);

        streams.from(IncomingOrder.class)
                .process(new EnrichOrderWithProfileProcessor(customerProfiles))
                .to(OrderProfileView.class);

        var updates = topology.input(CustomerProfileUpdated.class);
        var orders = topology.input(IncomingOrder.class);
        var output = topology.output(OrderProfileView.class);

        updates.send(new CustomerProfileUpdated(Reference.fromId("alice"), "SILVER"));
        updates.send(new CustomerProfileUpdated(Reference.fromId("alice"), "GOLD"));
        updates.sendTombstone(Reference.fromId("alice"));
        orders.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

        assertThat(output.value().tier()).isEqualTo("GOLD (2 updates)");
    }

    @Test
    void globalStoreFrom_shouldRegisterGlobalStoreWithoutAStream() {
        streams.globalStoreFrom(CustomerProfileUpdated.class)
                .updatedBy(UpdateCustomerProfileUpdater::update)
                .as("profiles", CustomerProfile.class);
        streams.from(IncomingOrder.class).end();

        assertThat(topology.inspect().globalStoreCount()).isEqualTo(1);
    }

    @Test
    void run_shouldExposeGlobalStoreContentAndAcceptMultipleDefinitions() {
        var customerProfiles = streams.globalStoreFrom(CustomerProfileUpdated.class)
                .updatedBy(UpdateCustomerProfileUpdater::update)
                .as(CustomerProfile.class);
        streams.from(IncomingOrder.class)
                .process(new EnrichOrderWithProfileProcessor(customerProfiles))
                .to(OrderProfileView.class);
        streams.from(OrderProfileView.class).end();

        topology.input(CustomerProfileUpdated.class).send(new CustomerProfileUpdated(Reference.fromId("alice"), "SILVER"));

        assertThat(topology.lookup(customerProfiles, Reference.fromId("alice"))).isPresent();
    }

    @Test
    void run_shouldAcceptTopologyConsistingOfGlobalStoresOnly() {
        var customerProfiles = streams.globalStore(CustomerProfile.class);

        topology.input(CustomerProfile.class).send(new CustomerProfile(Reference.fromId("alice"), "GOLD"));

        assertThat(topology.lookup(customerProfiles, Reference.fromId("alice"))).isPresent();
    }

    @Test
    void globalStoreFrom_shouldFailFastWhenNameOrSourceTopicIsAlreadyUsed() {
        streams.globalStoreFrom(CustomerProfileUpdated.class)
                .updatedBy(UpdateCustomerProfileUpdater::update)
                .as("profiles", CustomerProfile.class);
        var declaration = streams.globalStoreFrom(CustomerProfileUpdated.class)
                .updatedBy(UpdateCustomerProfileUpdater::update);

        assertThatThrownBy(() -> declaration.as("profiles", CustomerProfile.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already declared");
        assertThatThrownBy(() -> declaration.as("other-profiles", CustomerProfile.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already feeds global store 'global-profiles'");
    }
}
