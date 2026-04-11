package be.appify.prefab.processor.audit;

import be.appify.prefab.core.annotations.audit.CreatedAt;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.audit.LastModifiedAt;
import be.appify.prefab.core.annotations.audit.LastModifiedBy;
import be.appify.prefab.core.audit.AuditInfo;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import java.time.Instant;
import java.util.List;

/**
 * Helper that detects audit-annotated fields on a {@link ClassManifest} and generates
 * JavaPoet {@link CodeBlock}s for populating those fields in generated service methods.
 */
public class AuditFields {

    private AuditFields() {
    }

    /**
     * Returns {@code true} when the aggregate has at least one field annotated with an audit
     * annotation, or a field of type {@link AuditInfo}.
     */
    public static boolean hasAuditFields(ClassManifest manifest) {
        return manifest.fields().stream().anyMatch(AuditFields::isAuditAnnotatedField)
                || manifest.fields().stream().anyMatch(AuditFields::isAuditInfoField);
    }

    /**
     * Returns {@code true} when the field carries one of the four audit annotations.
     */
    public static boolean isAuditAnnotatedField(VariableManifest field) {
        return field.hasAnnotation(CreatedAt.class)
                || field.hasAnnotation(CreatedBy.class)
                || field.hasAnnotation(LastModifiedAt.class)
                || field.hasAnnotation(LastModifiedBy.class);
    }

    /**
     * Returns {@code true} when the field is of type {@link AuditInfo}.
     */
    public static boolean isAuditInfoField(VariableManifest field) {
        return field.type().is(AuditInfo.class);
    }

    /**
     * Builds the argument list for the full-argument constructor call of {@code manifest}, replacing
     * each audit field with its creation-time value:
     * <ul>
     *   <li>{@code @CreatedAt} / {@code @LastModifiedAt} → {@code Instant.now()}</li>
     *   <li>{@code @CreatedBy} / {@code @LastModifiedBy} → {@code auditContextProvider.currentUserId()}</li>
     *   <li>{@link AuditInfo} field → {@code new AuditInfo(Instant.now(), currentUserId, Instant.now(), currentUserId)}</li>
     *   <li>all other fields → {@code aggregate.<fieldName>()}</li>
     * </ul>
     */
    public static CodeBlock createReconstructionArgs(List<VariableManifest> fields) {
        return fields.stream()
                .map(AuditFields::createFieldCode)
                .collect(CodeBlock.joining(", "));
    }

    /**
     * Builds the argument list for the full-argument constructor call of {@code manifest}, replacing
     * only the {@code @LastModifiedAt}/{@code @LastModifiedBy} (or the corresponding fields of an
     * {@link AuditInfo} value) with their update-time values. Created-at/by fields are preserved.
     */
    public static CodeBlock updateReconstructionArgs(List<VariableManifest> fields) {
        return fields.stream()
                .map(AuditFields::updateFieldCode)
                .collect(CodeBlock.joining(", "));
    }

    private static CodeBlock createFieldCode(VariableManifest field) {
        if (isAuditInfoField(field)) {
            return CodeBlock.of("new $T($T.now(), auditContextProvider.currentUserId(), $T.now(), auditContextProvider.currentUserId())",
                    ClassName.get("be.appify.prefab.core.audit", "AuditInfo"),
                    Instant.class, Instant.class);
        } else if (field.hasAnnotation(CreatedAt.class) || field.hasAnnotation(LastModifiedAt.class)) {
            return CodeBlock.of("$T.now()", Instant.class);
        } else if (field.hasAnnotation(CreatedBy.class) || field.hasAnnotation(LastModifiedBy.class)) {
            return CodeBlock.of("auditContextProvider.currentUserId()");
        }
        return CodeBlock.of("aggregate.$N()", field.name());
    }

    private static CodeBlock updateFieldCode(VariableManifest field) {
        if (isAuditInfoField(field)) {
            return CodeBlock.of(
                    "new $T(aggregate.$N().createdAt(), aggregate.$N().createdBy(), $T.now(), auditContextProvider.currentUserId())",
                    ClassName.get("be.appify.prefab.core.audit", "AuditInfo"),
                    field.name(), field.name(), Instant.class);
        } else if (field.hasAnnotation(LastModifiedAt.class)) {
            return CodeBlock.of("$T.now()", Instant.class);
        } else if (field.hasAnnotation(LastModifiedBy.class)) {
            return CodeBlock.of("auditContextProvider.currentUserId()");
        }
        // @CreatedAt and @CreatedBy are preserved on update
        return CodeBlock.of("aggregate.$N()", field.name());
    }
}
