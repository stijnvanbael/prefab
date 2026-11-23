package be.appify.prefab.test.inmemory;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A {@code PlatformTransactionManager} that manages no resource.
 * <p>
 * It exists so that {@code @Transactional} services start and behave consistently without a database: it tracks
 * transaction boundaries, rollback-only marking and {@code TransactionSynchronization} callbacks (such as
 * {@code afterCommit}), but cannot undo any change.
 */
public class InMemoryTransactionManager extends AbstractPlatformTransactionManager {

    /** Constructs a new InMemoryTransactionManager. */
    public InMemoryTransactionManager() {
        setTransactionSynchronization(SYNCHRONIZATION_ALWAYS);
    }

    @Override
    protected Object doGetTransaction() {
        return TransactionSynchronizationManager.getResource(this) instanceof Transaction existing
                ? existing
                : new Transaction();
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        return transaction == TransactionSynchronizationManager.getResource(this);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        TransactionSynchronizationManager.bindResource(this, transaction);
    }

    @Override
    protected Object doSuspend(Object transaction) {
        return TransactionSynchronizationManager.unbindResource(this);
    }

    @Override
    protected void doResume(Object transaction, Object suspendedResources) {
        TransactionSynchronizationManager.bindResource(this, suspendedResources);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        // Nothing to commit: no resource is involved.
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        // Nothing to roll back: no resource is involved.
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        ((Transaction) status.getTransaction()).rollbackOnly = true;
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        TransactionSynchronizationManager.unbindResource(this);
    }

    private static final class Transaction implements SmartTransactionObject {
        private boolean rollbackOnly;

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public void flush() {
            // Nothing to flush.
        }
    }
}
