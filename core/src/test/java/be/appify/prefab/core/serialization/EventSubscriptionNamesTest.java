package be.appify.prefab.core.serialization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventSubscriptionNamesTest {
    @Test
    void preservesFullIdentityWithinBrokerLimits() {
        var owner = "example." + "VeryLongHandlerOwner".repeat(12);
        var first = EventSubscriptionNames.name(owner, "example.Changed<java.lang.String>", "shared");

        assertThat(first).hasSizeLessThanOrEqualTo(80).matches("[a-zA-Z][a-zA-Z0-9_-]+");
        assertThat(EventSubscriptionNames.name(owner, "example.Changed<java.lang.String>", "shared")).isEqualTo(first);
        assertThat(EventSubscriptionNames.name(owner, "example.Changed<java.lang.Integer>", "shared")).isNotEqualTo(first);
        assertThat(EventSubscriptionNames.name(owner, "example.Changed<java.lang.String>", "other")).isNotEqualTo(first);
        assertThat(EventSubscriptionNames.name(owner + "Other", "example.Changed<java.lang.String>", "shared"))
                .isNotEqualTo(first);
    }
}
