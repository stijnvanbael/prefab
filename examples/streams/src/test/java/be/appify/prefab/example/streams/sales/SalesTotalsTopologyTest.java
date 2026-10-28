package be.appify.prefab.example.streams.sales;

import be.appify.prefab.example.streams.BiddingZoneId;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class SalesTotalsTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;
    private final BiddingZoneId belgium = new BiddingZoneId("BE");
    private final BiddingZoneId netherlands = new BiddingZoneId("NL");

    @Test
    void shouldEvolveRunningTotalPerBiddingZone() {
        new SalesTotalsTopology().salesTotalsTopology(streams);

        var input = topology.input(EnergySale.class);
        var output = topology.output(ZoneSalesTotal.class);

        input.send(new EnergySale("s-1", belgium, 10.0));
        input.send(new EnergySale("s-2", netherlands, 5.0));
        input.send(new EnergySale("s-3", belgium, 2.5));
        input.send(new EnergySale("s-4", belgium, 1.5));

        assertThat(output.values())
                .containsExactly(
                        new ZoneSalesTotal(belgium, 10.0, 1),
                        new ZoneSalesTotal(netherlands, 5.0, 1),
                        new ZoneSalesTotal(belgium, 12.5, 2),
                        new ZoneSalesTotal(belgium, 14.0, 3)
                );
    }
    @Test
    void shouldKeepSeparateHourlyTotalsPerBiddingZone() {
        new SalesTotalsTopology().salesTotalsTopology(streams);

        var input = topology.input(EnergySale.class);
        var output = topology.output(HourlyZoneSalesTotal.class);

        var firstHour = Instant.parse("2026-01-01T10:05:00Z");
        var secondHour = Instant.parse("2026-01-01T11:05:00Z");
        input.send(new EnergySale("s-1", belgium, 10.0), firstHour);
        input.send(new EnergySale("s-2", belgium, 2.5), firstHour.plusSeconds(600));
        input.send(new EnergySale("s-3", belgium, 4.0), secondHour);

        assertThat(output.values())
                .containsExactly(
                        new HourlyZoneSalesTotal(belgium, 10.0, 1),
                        new HourlyZoneSalesTotal(belgium, 12.5, 2),
                        new HourlyZoneSalesTotal(belgium, 4.0, 1)
                );
    }

    @Test
    void shouldAcceptLateSaleWithinGracePeriod() {
        new SalesTotalsTopology().salesTotalsTopology(streams, Duration.ofMinutes(5));

        var input = topology.input(EnergySale.class);
        var output = topology.output(HourlyZoneSalesTotal.class);

        input.send(new EnergySale("s-1", belgium, 10.0), Instant.parse("2026-01-01T10:05:00Z"));
        input.send(new EnergySale("s-2", belgium, 4.0), Instant.parse("2026-01-01T11:02:00Z"));
        input.send(new EnergySale("s-3", belgium, 2.0), Instant.parse("2026-01-01T10:59:00Z"));

        assertThat(output.values())
                .containsExactly(
                        new HourlyZoneSalesTotal(belgium, 10.0, 1),
                        new HourlyZoneSalesTotal(belgium, 4.0, 1),
                        new HourlyZoneSalesTotal(belgium, 12.0, 2)
                );
    }

    @Test
    void shouldDropLateSaleBeyondGracePeriod() {
        new SalesTotalsTopology().salesTotalsTopology(streams, Duration.ofMinutes(5));

        var input = topology.input(EnergySale.class);
        var output = topology.output(HourlyZoneSalesTotal.class);

        input.send(new EnergySale("s-1", belgium, 10.0), Instant.parse("2026-01-01T10:05:00Z"));
        input.send(new EnergySale("s-2", belgium, 4.0), Instant.parse("2026-01-01T11:10:00Z"));
        input.send(new EnergySale("s-3", belgium, 2.0), Instant.parse("2026-01-01T10:59:00Z"));

        assertThat(output.values())
                .containsExactly(
                        new HourlyZoneSalesTotal(belgium, 10.0, 1),
                        new HourlyZoneSalesTotal(belgium, 4.0, 1)
                );
    }

    @Test
    void shouldDropAnyLateSaleWhenGraceIsZero() {
        new SalesTotalsTopology().salesTotalsTopology(streams, Duration.ZERO);

        var input = topology.input(EnergySale.class);
        var output = topology.output(HourlyZoneSalesTotal.class);

        input.send(new EnergySale("s-1", belgium, 10.0), Instant.parse("2026-01-01T10:05:00Z"));
        input.send(new EnergySale("s-2", belgium, 4.0), Instant.parse("2026-01-01T11:02:00Z"));
        input.send(new EnergySale("s-3", belgium, 2.0), Instant.parse("2026-01-01T10:59:00Z"));

        assertThat(output.values())
                .containsExactly(
                        new HourlyZoneSalesTotal(belgium, 10.0, 1),
                        new HourlyZoneSalesTotal(belgium, 4.0, 1)
                );
    }
}
