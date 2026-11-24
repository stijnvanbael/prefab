package com.example.streams;

import be.appify.prefab.streams.StreamProcessor;
import be.appify.prefab.streams.StreamProcessorContext;
import be.appify.prefab.streams.StreamRecord;

public final class HeartbeatProcessor implements StreamProcessor<String, Pulse, String, Heartbeat> {
    @Override
    public void process(StreamRecord<String, Pulse> record) {
    }

    @Override
    public void initContext(StreamProcessorContext<String, Heartbeat> context) {
    }
}
