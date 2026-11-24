package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.test.inmemory.InMemoryPersistenceAutoConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.Customer;
import be.appify.prefab.test.inmemory.persistence.fixture.FixtureConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.Note;
import be.appify.prefab.test.inmemory.persistence.fixture.NoteRepository;
import be.appify.prefab.test.inmemory.persistence.fixture.Order;
import be.appify.prefab.test.inmemory.persistence.fixture.OrderRepository;
import be.appify.prefab.test.inmemory.persistence.fixture.ReportRepository;
import be.appify.prefab.test.inmemory.persistence.fixture.Shape;
import be.appify.prefab.test.inmemory.persistence.fixture.Shape.Circle;
import be.appify.prefab.test.inmemory.persistence.fixture.Shape.Square;
import be.appify.prefab.test.inmemory.persistence.fixture.ShapeRepository;
import be.appify.prefab.test.inmemory.persistence.fixture.Ticket;
import be.appify.prefab.test.inmemory.persistence.fixture.TicketRepository;
import be.appify.prefab.test.inmemory.persistence.fixture.Widget;
import be.appify.prefab.test.inmemory.persistence.fixture.WidgetRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class InMemoryRepositoriesTest {
    private static final Reference<Customer> ALICE = Reference.fromId("alice");
    private static final Reference<Customer> BOB = Reference.fromId("bob");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InMemoryPersistenceAutoConfiguration.class))
            .withUserConfiguration(FixtureConfiguration.class)
            .withPropertyValues("prefab.test.containers=false");

    @Test
    void everyRepositoryInterfaceGetsABean() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(OrderRepository.class)
                .hasSingleBean(WidgetRepository.class)
                .hasSingleBean(NoteRepository.class)
                .hasSingleBean(ShapeRepository.class));
    }

    @Test
    void noRepositoriesInContainersMode() {
        runner.withPropertyValues("prefab.test.containers=true")
                .run(context -> assertThat(context).doesNotHaveBean(OrderRepository.class));
    }

    @Test
    void saveAndFind_roundTrips() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);

            var saved = orders.save(Order.create("o1", "Grinder", "kitchen", ALICE, "10"));

            assertThat(orders.findById("o1")).contains(saved);
            assertThat(orders.existsById("o1")).isTrue();
            assertThat(orders.count()).isEqualTo(1);
            assertThat(orders.findAll()).containsExactly(saved);
            assertThat(orders.findAllById(List.of("o1", "missing"))).containsExactly(saved);
        });
    }

    @Test
    void aggregateIdentifiedByAReference_isFoundAndDeletedByItsPlainIdentifier() {
        runner.run(context -> {
            var tickets = context.getBean(TicketRepository.class);

            var saved = tickets.save(Ticket.create("Broken grinder"));

            assertThat(tickets.findById(saved.id().id())).contains(saved);
            tickets.deleteById(saved.id().id());
            assertThat(tickets.findById(saved.id().id())).isEmpty();
        });
    }

    @Test
    void delete_removesTheAggregate() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);
            var first = orders.save(Order.create("o1", "A", "x", ALICE, "1"));
            orders.save(Order.create("o2", "B", "x", ALICE, "1"));
            orders.save(Order.create("o3", "C", "x", ALICE, "1"));

            orders.deleteById("o2");
            orders.delete(first);

            assertThat(orders.findAll()).extracting(Order::id).containsExactly("o3");
            orders.deleteAll();
            assertThat(orders.count()).isZero();
        });
    }

    @Test
    void findAll_pagedAndSorted() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);
            orders.save(Order.create("o1", "Charlie", "x", ALICE, "1"));
            orders.save(Order.create("o2", "Alpha", "x", ALICE, "1"));
            orders.save(Order.create("o3", "Bravo", "x", ALICE, "1"));

            var page = orders.findAll(PageRequest.of(1, 2, Sort.by("name")));

            assertThat(page.getContent()).extracting(Order::name).containsExactly("Charlie");
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(orders.findAll(Sort.by(Sort.Order.desc("name")))).extracting(Order::name)
                    .containsExactly("Charlie", "Bravo", "Alpha");
        });
    }

    @Test
    void derivedQuery_onReferenceProperty_takesTheIdentifier() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);
            orders.save(Order.create("o1", "B", "x", ALICE, "1"));
            orders.save(Order.create("o2", "A", "x", ALICE, "1"));
            orders.save(Order.create("o3", "C", "x", BOB, "1"));

            var page = orders.findByCustomer("alice", PageRequest.of(0, 10, Sort.by("name")));

            assertThat(page.getContent()).extracting(Order::id).containsExactly("o2", "o1");
            assertThat(page.getTotalElements()).isEqualTo(2);
        });
    }

    @Test
    void mixinQueries_takeAReference() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);
            orders.save(Order.create("o1", "B", "x", ALICE, "1"));
            orders.save(Order.create("o2", "A", "x", ALICE, "1"));
            orders.save(Order.create("o3", "C", "y", BOB, "1"));

            assertThat(orders.countByCustomer(ALICE)).isEqualTo(2);
            assertThat(orders.findByCategoryOrderByNameAsc("x")).extracting(Order::name).containsExactly("A", "B");
        });
    }

    @Test
    void nativeQuery_warnsAtStartupAndFailsWhenCalled(CapturedOutput output) {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);

            assertThat(output).contains("OrderRepository#findBigOrders")
                    .contains("@IntegrationTest(containers = true)");
            assertThatThrownBy(orders::findBigOrders)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("OrderRepository#findBigOrders")
                    .hasMessageContaining("@IntegrationTest(containers = true)");
        });
    }

    @Test
    void autocompleteMethodsWithNativeQueries_doNotWarn(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).hasSingleBean(OrderRepository.class);

            assertThat(output).doesNotContain("OrderRepository#autocompleteByName");
        });
    }

    @Test
    void mongoQueries_failWhenCalled(CapturedOutput output) {
        runner.run(context -> {
            var reports = context.getBean(ReportRepository.class);

            assertThat(output).contains("ReportRepository#findWithMongoQuery")
                    .contains("ReportRepository#findWithPipeline");
            assertThatThrownBy(() -> reports.findWithMongoQuery("x"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("ReportRepository#findWithMongoQuery");
            assertThatThrownBy(() -> reports.findWithPipeline("x"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("@IntegrationTest(containers = true)");
        });
    }

    @Test
    void save_givenNewAggregate_startsAtVersionOneForPrimitive() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);

            var saved = orders.save(Order.create("o1", "A", "x", ALICE, "1"));

            assertThat(saved.version()).isEqualTo(1);
            assertThat(orders.findById("o1")).map(Order::version).contains(1L);
        });
    }

    @Test
    void save_givenExistingAggregate_incrementsTheVersion() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);
            var first = orders.save(Order.create("o1", "A", "x", ALICE, "1"));

            var second = orders.save(first.withName("B"));

            assertThat(second.version()).isEqualTo(2);
            assertThat(orders.findById("o1")).contains(second);
        });
    }

    @Test
    void save_givenStaleVersion_failsAndKeepsTheStoredAggregate() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);
            var stale = orders.save(Order.create("o1", "A", "x", ALICE, "1"));
            var current = orders.save(stale.withName("B"));

            assertThatThrownBy(() -> orders.save(stale.withName("C")))
                    .isInstanceOf(OptimisticLockingFailureException.class);
            assertThat(orders.findById("o1")).contains(current);
        });
    }

    @Test
    void save_givenAggregateDeletedMeanwhile_fails() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);
            var saved = orders.save(Order.create("o1", "A", "x", ALICE, "1"));
            orders.deleteById("o1");

            assertThatThrownBy(() -> orders.save(saved.withName("B")))
                    .isInstanceOf(OptimisticLockingFailureException.class);
        });
    }

    @Test
    void save_givenNewAggregateWithTakenId_fails() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);
            orders.save(Order.create("o1", "A", "x", ALICE, "1"));

            assertThatThrownBy(() -> orders.save(Order.create("o1", "B", "x", ALICE, "1")))
                    .isInstanceOf(DuplicateKeyException.class);
        });
    }

    @Test
    void delete_givenStaleVersion_fails() {
        runner.run(context -> {
            var orders = context.getBean(OrderRepository.class);
            var stale = orders.save(Order.create("o1", "A", "x", ALICE, "1"));
            orders.save(stale.withName("B"));

            assertThatThrownBy(() -> orders.delete(stale)).isInstanceOf(OptimisticLockingFailureException.class);
            assertThat(orders.count()).isEqualTo(1);
        });
    }

    @Test
    void save_givenWrapperVersion_startsAtZeroAndIncrements() {
        runner.run(context -> {
            var widgets = context.getBean(WidgetRepository.class);

            var first = widgets.save(new Widget("w1", null, "A"));
            var second = widgets.save(new Widget("w1", first.version(), "B"));

            assertThat(first.version()).isZero();
            assertThat(second.version()).isEqualTo(1L);
            assertThatThrownBy(() -> widgets.save(new Widget("w1", first.version(), "C")))
                    .isInstanceOf(OptimisticLockingFailureException.class);
        });
    }

    @Test
    void save_givenAggregateWithoutVersion_overwrites() {
        runner.run(context -> {
            var notes = context.getBean(NoteRepository.class);

            notes.save(new Note("n1", "first"));
            notes.save(new Note("n1", "second"));

            assertThat(notes.findAll()).containsExactly(new Note("n1", "second"));
        });
    }

    @Test
    void sealedAggregate_allVariantsShareTheRootsRepository() {
        runner.run(context -> {
            var shapes = context.getBean(ShapeRepository.class);

            shapes.save(new Circle("c1", 0, 1.5));
            shapes.save(new Square("s1", 0, 2));

            assertThat(shapes.findAll()).extracting(Shape::id).containsExactlyInAnyOrder("c1", "s1");
            assertThat(shapes.findById("s1")).containsInstanceOf(Square.class);
            assertThat(shapes.count()).isEqualTo(2);
            assertThat(shapes.findAll(PageRequest.of(0, 1, Sort.by("id"))).getContent())
                    .extracting(Shape::id).containsExactly("c1");
            var updated = shapes.save(((Circle) shapes.findById("c1").orElseThrow()));
            assertThat(updated).isEqualTo(new Circle("c1", 2, 1.5));
        });
    }
}
