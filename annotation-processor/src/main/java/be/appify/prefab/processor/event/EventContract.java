package be.appify.prefab.processor.event;

import be.appify.prefab.core.annotations.Compatibility;
import be.appify.prefab.core.versioning.EventVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * The committed contract of an event or component at a given version: its fields with their types and whether they
 * are optional (nullable, {@code Optional} or defaulted). Used as the baseline against which changes are checked.
 *
 * @param version the version the contract was released with
 * @param fields  the fields by name; fields of permitted subtypes are prefixed with the subtype name
 */
public record EventContract(EventVersion version, Map<String, Field> fields) {

    /** Creates a contract. */
    public EventContract {
        fields = new TreeMap<>(fields);
    }

    /**
     * A field of the contract.
     *
     * @param type     the Java type of the field
     * @param optional whether readers and writers can omit the field
     */
    public record Field(String type, boolean optional) {
    }

    /**
     * Reads a contract from its stored form.
     *
     * @param input the stored contract
     * @return the contract
     * @throws IOException if the contract cannot be read
     */
    public static EventContract read(InputStream input) throws IOException {
        var properties = new Properties();
        try (var reader = new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        var fields = new TreeMap<String, Field>();
        properties.stringPropertyNames().stream().filter(name -> name.startsWith("field."))
                .forEach(name -> {
                    var parts = properties.getProperty(name).split(";", 2);
                    fields.put(name.substring("field.".length()), new Field(parts[0], "optional".equals(parts[1])));
                });
        return new EventContract(EventVersion.parse(properties.getProperty("version")), fields);
    }

    /**
     * Writes the contract in a stable, diff-friendly form.
     *
     * @param writer the target
     * @throws IOException if the contract cannot be written
     */
    public void write(Writer writer) throws IOException {
        writer.write("version=" + version + "\n");
        for (var entry : fields.entrySet()) {
            writer.write("field." + entry.getKey() + "=" + entry.getValue().type() + ";"
                    + (entry.getValue().optional() ? "optional" : "required") + "\n");
        }
    }

    /**
     * Compares this (current) contract with the released baseline.
     *
     * @param baseline  the released contract
     * @param renamed   current field names mapped to the names they previously had
     * @return the changes
     */
    public Changes changesSince(EventContract baseline, Map<String, List<String>> renamed) {
        var backward = new ArrayList<String>();
        var forward = new ArrayList<String>();
        var changed = false;
        var matchedBaseline = new java.util.HashSet<String>();
        for (var entry : fields.entrySet()) {
            var name = entry.getKey();
            var field = entry.getValue();
            var previous = baseline.fields.containsKey(name) ? name : renamed.getOrDefault(name, List.of()).stream()
                    .filter(baseline.fields::containsKey).findFirst().orElse(null);
            if (previous == null) {
                changed = true;
                if (!field.optional()) {
                    backward.add("required field '%s' was added".formatted(name));
                }
                continue;
            }
            matchedBaseline.add(previous);
            var old = baseline.fields.get(previous);
            if (!previous.equals(name)) {
                changed = true;
                if (!old.optional()) {
                    forward.add("field '%s' was renamed to '%s'".formatted(previous, name));
                }
            }
            if (!old.type().equals(field.type())) {
                changed = true;
                backward.add("type of field '%s' changed from %s to %s".formatted(name, old.type(), field.type()));
                forward.add("type of field '%s' changed from %s to %s".formatted(name, old.type(), field.type()));
            } else if (old.optional() != field.optional()) {
                changed = true;
                if (old.optional()) {
                    backward.add("field '%s' is no longer optional".formatted(name));
                } else {
                    forward.add("field '%s' became optional".formatted(name));
                }
            }
        }
        for (var entry : baseline.fields.entrySet()) {
            if (!matchedBaseline.contains(entry.getKey())) {
                changed = true;
                if (!entry.getValue().optional()) {
                    forward.add("required field '%s' was removed".formatted(entry.getKey()));
                }
            }
        }
        return new Changes(changed, backward, forward);
    }

    /**
     * The differences between a contract and its baseline.
     *
     * @param changed  whether the contract differs from the baseline
     * @param backward changes that prevent new consumers from reading old data
     * @param forward  changes that prevent old consumers from reading new data
     */
    public record Changes(boolean changed, List<String> backward, List<String> forward) {

        /**
         * Returns the changes that violate the compatibility mode.
         *
         * @param mode the mode, not {@link Compatibility#DEFAULT}
         * @return the violations, empty if the changes are compatible
         */
        public List<String> violations(Compatibility mode) {
            return switch (mode) {
                case BACKWARD -> backward;
                case FORWARD -> forward;
                case FULL -> java.util.stream.Stream.concat(backward.stream(), forward.stream()).distinct().toList();
                case NONE, DEFAULT -> List.of();
            };
        }
    }
}
