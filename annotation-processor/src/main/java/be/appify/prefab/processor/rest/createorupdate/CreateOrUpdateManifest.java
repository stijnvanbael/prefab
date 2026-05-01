package be.appify.prefab.processor.rest.createorupdate;

import be.appify.prefab.processor.rest.update.UpdateManifest;
import javax.lang.model.element.ExecutableElement;

/**
 * Pairs a {@code @Create} constructor (or static factory) with an {@code @Update} method that share the same
 * HTTP method and effective URL path, forming a create-or-update endpoint.
 *
 * @param createConstructor the {@code @Create}-annotated constructor or static factory method
 * @param updateManifest    the matched {@code @Update} method descriptor
 * @param lookupVariable    the path variable name (from the {@code @Create} path) used to look up an existing aggregate
 */
record CreateOrUpdateManifest(
        ExecutableElement createConstructor,
        UpdateManifest updateManifest,
        String lookupVariable
) {
}
