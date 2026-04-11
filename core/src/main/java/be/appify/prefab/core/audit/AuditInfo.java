package be.appify.prefab.core.audit;

import be.appify.prefab.core.annotations.audit.CreatedAt;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.audit.LastModifiedAt;
import be.appify.prefab.core.annotations.audit.LastModifiedBy;
import java.time.Instant;

/**
 * Convenience value object that groups all four standard audit fields together.
 * <p>
 * Use this record as a single field on your aggregate instead of declaring the four fields
 * individually:
 * </p>
 * <pre>{@code
 * @Aggregate
 * public record Contract(
 *     @Id Reference<Contract> id,
 *     @Version long version,
 *     String title,
 *     AuditInfo audit   // groups createdAt, createdBy, lastModifiedAt, lastModifiedBy
 * ) { ... }
 * }</pre>
 * <p>
 * The Prefab annotation processor detects the {@code AuditInfo} type and generates the same
 * population logic as when the four annotations are applied to individual fields.
 * </p>
 *
 * @param createdAt       timestamp of the first creation
 * @param createdBy       user ID of the creator
 * @param lastModifiedAt  timestamp of the most recent write
 * @param lastModifiedBy  user ID of the last modifier
 */
public record AuditInfo(
        @CreatedAt Instant createdAt,
        @CreatedBy String createdBy,
        @LastModifiedAt Instant lastModifiedAt,
        @LastModifiedBy String lastModifiedBy
) {
    /** Creates a new {@code AuditInfo} with all fields set to {@code null}. */
    public AuditInfo() {
        this(null, null, null, null);
    }
}
