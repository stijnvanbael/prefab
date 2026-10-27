package be.appify.prefab.example.streams.sales;

import be.appify.prefab.example.streams.BiddingZoneId;
import be.appify.prefab.test.streams.kafka.KafkaTopologyTestBootstrap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SalesTotalsTopologyTest {
    private final KafkaTopologyTestBootstrap bootstrap = KafkaTopologyTestBootstrap.bootstrap();
    private final BiddingZoneId belgium = new BiddingZoneId("BE");
    private final BiddingZoneId netherlands = new BiddingZoneId("NL");

    @Test
    void shouldEvolveRunningTotalPerBiddingZone() {
        var topology = new SalesTotalsTopology().salesTotalsTopology(bootstrap.streams());

        try (var test = bootstrap.run(topology)) {
            var input = test.input(EnergySale.class);
            var output = test.output(ZoneSalesTotal.class);

            input.pipeInput("s-1", new EnergySale("s-1", belgium, 10.0));
            input.pipeInput("s-2", new EnergySale("s-2", netherlands, 5.0));
            input.pipeInput("s-3", new EnergySale("s-3", belgium, 2.5));
            input.pipeInput("s-4", new EnergySale("s-4", belgium, 1.5));

            assertThat(output.readValuesToList())
                    .containsExactly(
                            new ZoneSalesTotal(belgium, 10.0, 1),
                            new ZoneSalesTotal(netherlands, 5.0, 1),
                            new ZoneSalesTotal(belgium, 12.5, 2),
                            new ZoneSalesTotal(belgium, 14.0, 3)
                    );
        }
    }
}
