package be.appify.prefab.core.domain;

import java.io.File;

public record Binary(
        String name,
        String contentType,
        File data
) {
}
