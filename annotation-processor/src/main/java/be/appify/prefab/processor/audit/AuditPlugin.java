package be.appify.prefab.processor.audit;

import be.appify.prefab.core.annotations.audit.CreatedAt;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.audit.LastModifiedAt;
import be.appify.prefab.core.annotations.audit.LastModifiedBy;
import be.appify.prefab.core.audit.AuditContextProvider;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/**
 * Plugin that handles the {@code @CreatedAt}, {@code @CreatedBy}, {@code @LastModifiedAt}, and
 * {@code @LastModifiedBy} annotations on aggregate fields.
 * <p>
 * When audit-annotated fields are detected, this plugin:
 * <ul>
 *   <li>Injects an {@link AuditContextProvider} as a service dependency.</li>
 *   <li>Generates {@code withAuditCreate} and {@code withAuditUpdate} helper methods in the service
 *       that reconstruct the aggregate with the audit fields filled in.</li>
 * </ul>
 * The {@code CreateServiceWriter} and {@code UpdateServiceWriter} call these helper methods when
 * audit-annotated fields are present.
 * </p>
 */
public class AuditPlugin implements PrefabPlugin {

    /** Constructs a new AuditPlugin. */
    public AuditPlugin() {
    }

    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest classManifest) {
        if (!hasAuditFields(classManifest)) {
            return Collections.emptySet();
        }
        return Set.of(ClassName.get(AuditContextProvider.class));
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        if (!hasAuditFields(manifest)) {
            return;
        }
        builder.addMethod(withAuditCreateMethod(manifest));
        builder.addMethod(withAuditUpdateMethod(manifest));
    }

    /**
     * Returns {@code true} if the given manifest contains at least one field annotated with one of
     * the four audit annotations.
     *
     * @param manifest the class manifest to inspect
     * @return {@code true} when audit fields are present
     */
    public static boolean hasAuditFields(ClassManifest manifest) {
        return manifest.fields().stream().anyMatch(AuditPlugin::isAuditField);
    }

    /**
     * Returns {@code true} if the given variable is annotated with any of the four audit annotations.
     *
     * @param field the variable to inspect
     * @return {@code true} when the field carries an audit annotation
     */
    public static boolean isAuditField(VariableManifest field) {
        return field.hasAnnotation(CreatedAt.class)
                || field.hasAnnotation(CreatedBy.class)
                || field.hasAnnotation(LastModifiedAt.class)
                || field.hasAnnotation(LastModifiedBy.class);
    }

    private MethodSpec withAuditCreateMethod(ClassManifest manifest) {
        var fields = manifest.fields();
        var fieldArgs = fields.stream()
                .map(f -> auditCreateExpression(f))
                .collect(Collectors.joining(",\n"));
        return MethodSpec.methodBuilder("withAuditCreate")
                .addModifiers(Modifier.PRIVATE)
                .returns(manifest.type().asTypeName())
                .addParameter(manifest.type().asTypeName(), "aggregate")
                .addStatement("var now = $T.now()", Instant.class)
                .addStatement("var userId = auditContextProvider.currentUserId()")
                .addStatement("return new $T(\n$L)", manifest.type().asTypeName(), fieldArgs)
                .build();
    }

    private MethodSpec withAuditUpdateMethod(ClassManifest manifest) {
        var fields = manifest.fields();
        var fieldArgs = fields.stream()
                .map(f -> auditUpdateExpression(f))
                .collect(Collectors.joining(",\n"));
        return MethodSpec.methodBuilder("withAuditUpdate")
                .addModifiers(Modifier.PRIVATE)
                .returns(manifest.type().asTypeName())
                .addParameter(manifest.type().asTypeName(), "aggregate")
                .addStatement("var now = $T.now()", Instant.class)
                .addStatement("var userId = auditContextProvider.currentUserId()")
                .addStatement("return new $T(\n$L)", manifest.type().asTypeName(), fieldArgs)
                .build();
    }

    private static String auditCreateExpression(VariableManifest field) {
        if (field.hasAnnotation(CreatedAt.class) || field.hasAnnotation(LastModifiedAt.class)) {
            return "now";
        }
        if (field.hasAnnotation(CreatedBy.class) || field.hasAnnotation(LastModifiedBy.class)) {
            return "userId";
        }
        return "aggregate." + field.name() + "()";
    }

    private static String auditUpdateExpression(VariableManifest field) {
        if (field.hasAnnotation(LastModifiedAt.class)) {
            return "now";
        }
        if (field.hasAnnotation(LastModifiedBy.class)) {
            return "userId";
        }
        // CreatedAt and CreatedBy are preserved on updates
        return "aggregate." + field.name() + "()";
    }
}
