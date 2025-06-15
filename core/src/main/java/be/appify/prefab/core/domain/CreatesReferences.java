package be.appify.prefab.core.domain;

import be.appify.prefab.core.service.Reference;

public interface CreatesReferences {
    default <T> Reference<T> referenceTo(T aggregate) {
        var referenceProvider = ReferenceProvider.getInstance();
        if (referenceProvider != null) {
            return referenceProvider.referenceTo(aggregate);
        }
        return null;
    }
}
