package assertion;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record SampleRecord(
        @Id String id,
        @Version long version,
        List<SampleElement> sampleElementList) {

    public record SampleElement(String value, int count) {
    }

    @Create
    public SampleRecord(List<SampleElement> sampleElementList) {
        this(UUID.randomUUID().toString(), 0L, sampleElementList);
    }
}


