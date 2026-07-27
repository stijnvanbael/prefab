package event.avsc.invalidkeyproperty;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.AvscFile;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "invalid-key-property", serialization = Event.Serialization.AVRO)
@Avsc(files = @AvscFile(path = "event/avsc/simple/source/SimpleAvscEvent.avsc", keyProperty = "missing"))
public interface InvalidKeyPropertyAvsc {
}
