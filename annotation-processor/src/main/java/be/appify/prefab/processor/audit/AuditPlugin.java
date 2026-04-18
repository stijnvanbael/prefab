package be.appify.prefab.processor.audit;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import java.util.Collections;
import java.util.Set;

/**
 * Prefab plugin that enables audit-trail support.
 * <p>
 * When an aggregate declares fields annotated with {@code @CreatedAt}, {@code @CreatedBy},
 * {@code @LastModifiedAt}, or {@code @LastModifiedBy} (or uses the {@code AuditInfo} convenience
 * record), this plugin injects an {@code AuditContextProvider} dependency into the generated service
 * so that the service writers can reference {@code auditContextProvider.currentUserId()} in the
 * generated create and update methods.
 * </p>
 */
public class AuditPlugin implements PrefabPlugin {


    @Override
    public Set<TypeName> getServiceDependencies(ClassManifest manifest) {
        if (AuditFields.hasAuditFields(manifest)) {
            return Set.of(ClassName.get("be.appify.prefab.core.audit", "AuditContextProvider"));
        }
        return Collections.emptySet();
    }
}
