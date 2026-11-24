Implement `HeartbeatProcessor` in `com.example.streams`.

Independently of input, it must emit a `Heartbeat` every second using the processor context's wall-clock scheduling
API. Each heartbeat must have key `"heartbeat"`, a one-based increasing sequence, and an `emittedAt` value matching
the callback time. Receiving a `Pulse` must not emit a heartbeat early. Do not emit the first heartbeat before the
first interval.

The in-memory topology test harness can advance time deterministically. Keep the implementation in the stream
processor; do not add a Spring service or a background thread.
