package be.appify.prefab.benchmark;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;

/**
 * How big a benchmark task is. Reports and comparisons show pass rates per tier, because a tier that every setup passes
 * says nothing about which setup is better.
 */
enum Tier {
    /** One feature on a tiny starting project, done in a few minutes. */
    QUICK,
    /** A feature that spans several aggregates, events and handlers, written as a user story. */
    FEATURE,
    /** A change to an existing, non-trivial application the agent did not write. */
    BROWNFIELD;

    /** The name used in {@code task.yml}, reports and {@code --tasks}, e.g. {@code quick}. */
    @JsonValue
    String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    static Tier fromId(String id) {
        return Arrays.stream(values())
                .filter(tier -> tier.id().equals(id.strip().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tier '" + id + "', known: "
                        + Arrays.stream(values()).map(Tier::id).toList()));
    }
}
