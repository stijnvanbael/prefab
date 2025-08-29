package be.appify.prefab.processor.spring;

import be.appify.prefab.core.service.IdCache;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;

@Component
public class PostTransactionHook implements TransactionSynchronization {
    @Override
    public void afterCompletion(int status) {
        IdCache.INSTANCE.clear();
    }
}
