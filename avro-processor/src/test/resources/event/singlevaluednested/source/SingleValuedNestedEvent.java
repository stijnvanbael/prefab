package event.avro;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "single-valued-nested", serialization = Event.Serialization.AVRO)
public record SingleValuedNestedEvent(
        String id,
        Outer outer) {
    /**
     * Single-value record whose only component is itself a record.
     * Before the fix, isSingleValueType() caused this to be treated as a logical (String-wrapper)
     * type, generating incorrect toString() conversion instead of a nested-record converter.
     */
    public record Outer(
            Inner inner
    ) {
        public record Inner(
                String value
        ) {
        }
    }
}

