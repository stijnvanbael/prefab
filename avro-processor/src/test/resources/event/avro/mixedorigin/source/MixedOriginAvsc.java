package event.avro.mixedorigin;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.AvscFile;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "mixed-origin-avsc", serialization = Event.Serialization.AVRO)
@Avsc(files = @AvscFile(path = "event/avro/mixedorigin/source/MixedOriginAvscEvent.avsc", keyProperty = "name"))
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface MixedOriginAvsc {
}
