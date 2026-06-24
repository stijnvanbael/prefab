package event.avsc.crossdocwarning;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "cross-doc-warning", serialization = Event.Serialization.AVRO)
@Avsc({
    "event/avsc/crossdocwarning/source/CrossEventC.avsc",
    "event/avsc/crossdocwarning/source/CrossEventD.avsc"
})
public interface CrossDocWarningAvsc {
}

