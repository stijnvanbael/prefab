package be.appify.prefab.core.audit;

import be.appify.prefab.core.annotations.audit.CreatedAt;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.audit.LastModifiedAt;
import be.appify.prefab.core.annotations.audit.LastModifiedBy;
import java.time.Instant;

/**
 * Convenience value object that groups all four audit-trail fields together.
 * <p>
 * Can be used as a nested record inside an aggregate to keep audit fields together:
 * </p>
 * <pre>{@code
 * @Aggregate
 * public record Contract(
 *     @Id String id,
 *     @Version long version,
 *     String title,
 *     AuditInfo auditInfo
 * ) { ... }
 * }</pre>
 * <p>
 * Alternatively, the four annotations can be placed directly on individual aggregate fields.
 * </p>
 *
 * @param createdBy       the user who created the aggregate
 * @param createdAt       the timestamp when the aggregate was created
 * @param lastModifiedBy  the user who last modified the aggregate
 * @param lastModifiedAt  the timestamp of the last modification
 */
public record AuditInfo(
        @CreatedBy String createdBy,
        @CreatedAt Instant createdAt,
        @LastModifiedBy String lastModifiedBy,
        @LastModifiedAt Instant lastModifiedAt
) {
}
