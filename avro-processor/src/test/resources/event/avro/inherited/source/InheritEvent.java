package event.avro;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "inherited", serialization = Event.Serialization.AVRO)
public class InheritEvent extends SuperType {
    private String subField;

    public InheritEvent(String superField, String subField) {
        super(superField);
        this.subField = subField;
    }

    public String subField() {
        return subField;
    }
}