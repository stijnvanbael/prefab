package be.appify.prefab.benchmark;

/**
 * Leaves the starting project untouched. Used by {@code validate}: the acceptance checks must fail on the starter,
 * otherwise the task does not test anything.
 */
record NoopAgent() implements Agent {
    @Override
    public String name() {
        return "noop";
    }

    @Override
    public String model() {
        return "noop";
    }

    @Override
    public AgentOutcome run(AgentRun run) {
        return AgentOutcome.instant("left the starting project unchanged");
    }
}
