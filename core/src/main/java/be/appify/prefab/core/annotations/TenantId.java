package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the tenant discriminator for multi-tenancy support.
 *
 * <p>Annotate a {@code String} or {@code Reference} field on an {@link Aggregate} with {@code @TenantId} to
 * automatically isolate data per tenant. The annotation processor will:</p>
 * <ul>
 *   <li>Populate the field from {@link be.appify.prefab.core.tenant.TenantContextProvider#currentTenantId()} on
 *       every write operation (create, update, delete).</li>
 *   <li>Add a {@code WHERE tenantField = :tenantId} predicate to every read operation (GetById, GetList,
 *       Update, Delete) so tenants never see each other's data.</li>
 *   <li>Generate a {@code NOT NULL} database column with an index for query performance.</li>
 * </ul>
 *
 * <p>Only one {@code @TenantId} field is allowed per aggregate; the annotation processor raises a compile
 * error if more than one is declared.  The field must <em>not</em> appear in any generated request record
 * (i.e. not as a parameter of a {@link be.appify.prefab.core.annotations.rest.Create @Create} constructor or
 * an {@link be.appify.prefab.core.annotations.rest.Update @Update} method); a compile error is raised if it
 * does.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * @Aggregate
 * @GetList
 * @GetById
 * public record Project(
 *     @Id Reference<Project> id,
 *     @Version long version,
 *     @TenantId String organisationId,
 *     String name,
 *     String description) {
 *
 *     @Create
 *     public Project(String name, String description) {
 *         this(Reference.create(), 0L, null, name, description);
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantId {
}
