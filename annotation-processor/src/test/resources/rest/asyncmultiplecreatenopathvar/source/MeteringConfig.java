package rest.asyncmultiplecreatenopathvar;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;

@Aggregate
@AsyncCommit
public record MeteringConfig(
        @Id Reference<MeteringConfig> id,
        String status,
        String remark
) {
    @Create(path = "/{meteringconfig}/close-for-input")
    public static void closeForInput(String meteringconfigId, String remark) {
    }

    @Create(path = "/{meteringconfig}/open-for-input")
    public static void openForInput(String meteringconfigId, String remark) {
    }
}

