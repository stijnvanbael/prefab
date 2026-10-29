package be.appify.prefab.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the previous name(s) of an event type or of an event field, so that messages written under an older name
 * can still be read into the current contract.
 * <ul>
 *   <li>On an event field (record component): JSON property names and Avro field names. For Avro the previous names
 *       are emitted as aliases on the generated schema field.</li>
 *   <li>On an event type: the type id of JSON messages, the Avro record name (emitted as a schema alias) and the
 *       class name used by SNS/SQS. Use the unqualified name or the fully qualified class name, whichever was used
 *       by the writer of the message.</li>
 * </ul>
 * Reading under a previous name is the only behaviour affected; events are always written under the current name.
 */
@Documented
@Target({ElementType.TYPE, ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreviousName {

    /**
     * The previous name(s), most recent first.
     *
     * @return the previous names
     */
    String[] value();
}
