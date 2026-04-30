package rest.asyncmultiplecreatewithpathvar;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;

@Aggregate
@AsyncCommit
public record MeteringConfig(
        @Id Reference<MeteringConfig> id,
        String meteringconfig,
        String remark,
        String status
) {
    @Create(path = "/{meteringconfig}/close-for-input")
    public static void closeForInput(String meteringconfig, String remark) {
    }

    @Create(path = "/{meteringconfig}/open-for-input")
    public static void openForInput(String meteringconfig, String remark) {
    }
}

