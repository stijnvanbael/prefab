package com.example.streams;

import be.appify.prefab.streams.StreamProcessor;
import be.appify.prefab.streams.StreamProcessorContext;
import be.appify.prefab.streams.StreamRecord;
import java.time.Duration;
import java.util.Map;

public final class HeartbeatProcessor implements StreamProcessor<String, Pulse, String, Heartbeat> {
    private long sequence;

    @Override
    public void process(StreamRecord<String, Pulse> record) {
    }

    @Override
    public void initContext(StreamProcessorContext<String, Heartbeat> context) {
        context.schedule(Duration.ofSeconds(1), now -> {
            var nextSequence = ++sequence;
            context.forward(new StreamRecord<>(
                    "heartbeat", new Heartbeat("heartbeat", nextSequence, now), now, Map.of()));
        });
    }
}
