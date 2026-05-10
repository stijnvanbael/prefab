package event.avro;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "inherited", serialization = Event.Serialization.AVRO)
public abstract class SuperType {
    private String superField;

    public SuperType(String superField) {
        this.superField = superField;
    }

    public String superField() {
        return superField;
    }
}