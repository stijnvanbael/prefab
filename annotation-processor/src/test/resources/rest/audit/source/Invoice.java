package rest.audit;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.audit.AuditInfo;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Invoice(
        @Id Reference<Invoice> id,
        @Version long version,
        String number,
        AuditInfo audit
) {
    @Create
    public Invoice(String number) {
        this(Reference.create(), 0L, number, new AuditInfo());
    }

    @Update
    public Invoice update(String number) {
        return new Invoice(id, version, number, audit);
    }
}
