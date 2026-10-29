package event.avro.snapshot;

import be.appify.prefab.core.annotations.EventSnapshot;

@EventSnapshot(avsc = "event/avro/snapshot/source/UserCreatedV1.avsc", name = "UserCreatedV1")
public class Snapshots {
}
