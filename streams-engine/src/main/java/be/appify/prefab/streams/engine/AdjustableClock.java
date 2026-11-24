package be.appify.prefab.streams.engine;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

final class AdjustableClock extends Clock {
    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    AdjustableClock(Instant initialInstant) {
        this(new AtomicReference<>(Objects.requireNonNull(initialInstant, "initialInstant")), ZoneId.systemDefault());
    }

    private AdjustableClock(AtomicReference<Instant> instant, ZoneId zone) {
        this.instant = instant;
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    void advance(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        instant.updateAndGet(current -> current.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new AdjustableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant.get();
    }
}
