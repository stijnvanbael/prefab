package be.appify.prefab.processor.rest.update;

import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.processor.VariableManifest;

import java.util.List;

record UpdateManifest(
        String operationName,
        List<VariableManifest> parameters,
        List<VariableManifest> requestParameters,
        List<VariableManifest> aggregateParameters,
        List<VariableManifest> parentEntityParameters,
        boolean stateful,
        String method,
        String path,
        Security security) {
}
