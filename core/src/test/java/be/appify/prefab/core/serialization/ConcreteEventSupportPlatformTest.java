package be.appify.prefab.core.serialization;

import be.appify.prefab.core.annotations.Event;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.core.annotations.Event.Platform.KAFKA;
import static be.appify.prefab.core.annotations.Event.Platform.PUB_SUB;
import static be.appify.prefab.core.annotations.Event.Platform.SNS_SQS;
import static org.assertj.core.api.Assertions.assertThat;

class ConcreteEventSupportPlatformTest {
    @Event(topic = "kafka", platform = KAFKA)
    record OnKafka() {
    }

    @Event(topic = "pubsub", platform = PUB_SUB)
    record OnPubSub() {
    }

    @Event(topic = "derived")
    record Derived() {
    }

    record Undeclared() {
    }

    @Event(topic = "sealed", platform = SNS_SQS)
    sealed interface Parent permits Child {
    }

    record Child() implements Parent {
    }

    @Test
    void claimsOnlyTheDeclaredPlatform() {
        assertThat(ConcreteEventSupport.usesPlatform(OnKafka.class, KAFKA)).isTrue();
        assertThat(ConcreteEventSupport.usesPlatform(OnKafka.class, PUB_SUB)).isFalse();
        assertThat(ConcreteEventSupport.usesPlatform(OnPubSub.class, PUB_SUB)).isTrue();
        assertThat(ConcreteEventSupport.usesPlatform(OnPubSub.class, SNS_SQS)).isFalse();
    }

    @Test
    void anyPlatformMayDeriveItsOwnEvents() {
        assertThat(ConcreteEventSupport.usesPlatform(Derived.class, KAFKA)).isTrue();
        assertThat(ConcreteEventSupport.usesPlatform(Derived.class, SNS_SQS)).isTrue();
    }

    @Test
    void anEventWithoutDeclarationIsNotTiedToAPlatform() {
        assertThat(ConcreteEventSupport.usesPlatform(Undeclared.class, PUB_SUB)).isTrue();
    }

    @Test
    void aSubtypeUsesTheDeclarationOfItsSupertype() {
        assertThat(ConcreteEventSupport.usesPlatform(Child.class, SNS_SQS)).isTrue();
        assertThat(ConcreteEventSupport.usesPlatform(Child.class, KAFKA)).isFalse();
    }
}
