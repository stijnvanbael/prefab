package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.AvscFile;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "simple-avsc", serialization = Event.Serialization.AVRO)
@Avsc(files = @AvscFile(path = "event/avsc/simple/source/SimpleAvscEvent.avsc", keyProperty = "name"))
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface SimpleAvsc {
}
