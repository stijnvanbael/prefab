package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Event {
    String topic();

    Platform platform() default Platform.KAFKA;

    Class<?> ownedBy();

    Serialization serialization() default Serialization.JSON;

    enum Platform {
        KAFKA,
        PUB_SUB,
//        SNS_SQS,
//        EVENT_HUBS,
    }

    enum Serialization {
        JSON,
//        AVRO,
//        PROTOBUF,
//        PLAIN_TEXT,
    }
}
