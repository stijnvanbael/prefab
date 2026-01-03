package be.appify.prefab.core.domain;

import java.io.File;

/**
 * Represents a binary file with its name, content type, and data.
 * @param name        The name of the binary file.
 * @param contentType The content type of the binary file.
 * @param data        The file data.
 */
public record Binary(
        String name,
        String contentType,
        File data
) {
}
