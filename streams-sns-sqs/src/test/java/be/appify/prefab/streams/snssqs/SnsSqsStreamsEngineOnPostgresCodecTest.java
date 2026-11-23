package be.appify.prefab.streams.snssqs;

import be.appify.prefab.streams.engine.StreamStateRepository;

/**
 * Runs every engine scenario with state encoded by the Postgres codec, so a codec that loses the type of nested
 * values (aggregate state, join buffers, windows) fails here without needing a database.
 */
class SnsSqsStreamsEngineOnPostgresCodecTest extends SnsSqsStreamsEngineTest {
    @Override
    StreamStateRepository repository() {
        return new CodecRoundTrippingRepository();
    }
}
