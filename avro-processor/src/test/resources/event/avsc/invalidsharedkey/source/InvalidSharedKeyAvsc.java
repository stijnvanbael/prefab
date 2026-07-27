package event.avsc.invalidsharedkey;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "invalid-shared-key", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/simple/source/SimpleAvscEvent.avsc")
public interface InvalidSharedKeyAvsc {
    @PartitioningKey
    String missing();
}
