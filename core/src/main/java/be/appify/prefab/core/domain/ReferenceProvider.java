package be.appify.prefab.core.domain;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.util.ServiceLocator;

public abstract class ReferenceProvider {

    protected static ServiceLocator serviceLocator;

    private static final class InstanceHolder {
        private static final ReferenceProvider instance = serviceLocator != null
            ? serviceLocator.getInstance(ReferenceProvider.class)
            : null;
    }

    static ReferenceProvider getInstance() {
        return InstanceHolder.instance;
    }

    public abstract <T> Reference<T> referenceTo(T aggregate);
}
