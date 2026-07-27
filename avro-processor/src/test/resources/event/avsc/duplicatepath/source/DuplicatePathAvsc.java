package event.avsc.duplicatepath;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.AvscFile;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "duplicate-path", serialization = Event.Serialization.AVRO)
@Avsc(
        value = "event/avsc/simple/source/SimpleAvscEvent.avsc",
        files = @AvscFile(path = "event/avsc/simple/source/SimpleAvscEvent.avsc", keyProperty = "name")
)
public interface DuplicatePathAvsc {
}
