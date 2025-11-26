package be.appify.prefab.processor.update;

import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.processor.VariableManifest;

import java.util.List;

public record UpdateManifest(
        String operationName,
        List<VariableManifest> parameters,
        boolean stateful,
        String method,
        String path,
        Security security) {
}
