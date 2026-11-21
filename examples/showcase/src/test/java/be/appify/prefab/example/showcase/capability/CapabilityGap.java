package be.appify.prefab.example.showcase.capability;

/**
 * A capability a variant does not support, with the reason and the backlog task that tracks closing the gap.
 *
 * @param capability the unsupported capability
 * @param reason     why the variant lacks it
 * @param task       backlog task ID that tracks the gap, e.g. {@code TASK-341}
 */
public record CapabilityGap(Capability capability, String reason, String task) {
}
