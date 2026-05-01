package be.appify.prefab.test.asserts;

import be.appify.prefab.test.EventConsumer;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventConsumerWhereStepTest {

    private static final List<String> EVENTS = List.of("event-A", "event-B");

    @Test
    void existingWhereOverloadContinuesToWork() {
        var consumer = fixedConsumer(EVENTS);

        EventConsumerAssert.assertThat(consumer)
                .hasReceivedMessages(2)
                .within(1, TimeUnit.SECONDS)
                .where(events -> events.containsExactlyInAnyOrder("event-A", "event-B"));
    }

    @Test
    void customAssertClassReceivesEventList() {
        var consumer = fixedConsumer(EVENTS);
        var captured = new AtomicReference<List<String>>();

        EventConsumerAssert.assertThat(consumer)
                .hasReceivedMessages(2)
                .within(1, TimeUnit.SECONDS)
                .where(TestEventListAssert.class, assert_ -> {
                    captured.set(assert_.actualEvents());
                    assertThat(assert_.actualEvents()).containsExactlyInAnyOrder("event-A", "event-B");
                });

        assertThat(captured.get()).isEqualTo(EVENTS);
    }

    @Test
    void customAssertClassPropagatesAssertionFailures() {
        var consumer = fixedConsumer(EVENTS);

        assertThatThrownBy(() ->
                EventConsumerAssert.assertThat(consumer)
                        .hasReceivedMessages(2)
                        .within(1, TimeUnit.SECONDS)
                        .where(TestEventListAssert.class, assert_ ->
                                assertThat(assert_.actualEvents()).containsExactly("wrong-event"))
        ).getCause().isInstanceOf(AssertionError.class);
    }

    @Test
    void customAssertClassMissingConstructorThrowsDescriptiveError() {
        var consumer = fixedConsumer(EVENTS);

        assertThatThrownBy(() ->
                EventConsumerAssert.assertThat(consumer)
                        .hasReceivedMessages(2)
                        .within(1, TimeUnit.SECONDS)
                        .where(AssertWithoutListConstructor.class, assert_ -> {})
        ).getCause().isInstanceOf(AssertionError.class)
                .hasMessageContaining("constructor accepting List");
    }

    private static <V> EventConsumer<V> fixedConsumer(List<V> events) {
        return new EventConsumer<>(events);
    }

    static class TestEventListAssert extends AbstractAssert<TestEventListAssert, List<String>> {
        TestEventListAssert(List<String> events) {
            super(events, TestEventListAssert.class);
        }

        List<String> actualEvents() {
            return actual;
        }
    }

    static class AssertWithoutListConstructor extends AbstractAssert<AssertWithoutListConstructor, String> {
        AssertWithoutListConstructor(String value) {
            super(value, AssertWithoutListConstructor.class);
        }
    }
}
