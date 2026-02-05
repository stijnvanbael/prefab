package event.avro;

public class SuperType {
    private String superField;

    public SuperType(String superField) {
        this.superField = superField;
    }

    public String superField() {
        return superField;
    }
}