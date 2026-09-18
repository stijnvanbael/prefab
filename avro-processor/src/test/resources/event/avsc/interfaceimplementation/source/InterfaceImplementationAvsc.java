package event.avsc.interfaceimplementation;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.AvscInterface;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "interface-implementation-avsc", serialization = Event.Serialization.AVRO)
@Avsc({
        "event/avsc/interfaceimplementation/source/lifecycleStarted.avsc",
        "event/avsc/interfaceimplementation/source/lifecycleStopped.avsc"
})
@AvscInterface(type = LifecycleEvent.class, namespace = "intern.events.lifecycle.v1", name = "lifecycleStarted")
@AvscInterface(type = LifecycleEvent.class, namespace = "intern.events.lifecycle.v1", name = "lifecycleStopped")
@AvscInterface(type = SharedLifecycleEvent.class, namespace = "intern.events.lifecycle.v1", name = "lifecycleStarted")
@AvscInterface(type = LifecycleStatus.class, namespace = "intern.events.lifecycle.v1", name = "status")
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface InterfaceImplementationAvsc {
}

interface LifecycleEvent {
    String id();
}

interface SharedLifecycleEvent {
    String id();

    default String stream() {
        return "lifecycle";
    }
}

interface LifecycleStatus {
}
