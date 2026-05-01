package event.avsc.infrastructure.avro;

import java.io.IOException;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component("event_avsc_SimpleAvscEventSchemaFactory")
public class SimpleAvscEventSchemaFactory {
    private final Schema schema;

    public SimpleAvscEventSchemaFactory() {
        try (var stream = SimpleAvscEventSchemaFactory.class.getClassLoader().getResourceAsStream("event/avsc/simple/source/SimpleAvscEvent.avsc")) {
            if (stream == null) {
                throw new IllegalStateException("AVSC file not found on classpath: event/avsc/simple/source/SimpleAvscEvent.avsc");
            }
            this.schema = new Schema.Parser().parse(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Schema createSchema() {
        return this.schema;
    }
}
