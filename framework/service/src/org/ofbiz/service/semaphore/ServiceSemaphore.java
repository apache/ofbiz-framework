package org.ofbiz.service.semaphore;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.ModelService;

import javax.transaction.Transaction;
import java.sql.Timestamp;
import java.util.Map;

/**
 * ServiceSemaphore
 */
public class ServiceSemaphore {

    public static final String module = ServiceSemaphore.class.getName();
    public static final int SEMAPHORE_MODE_FAIL = 0;
    public static final int SEMAPHORE_MODE_WAIT = 1;
    public static final int SEMAPHORE_MODE_NONE = 2;
    public static final long MAX_WAIT = 600;
    public static final long SLEEP = 500;

    protected GenericDelegator delegator;
    protected GenericValue lock;
    protected ModelService model;

    protected int wait = 0;
    protected int mode = 2;

    public ServiceSemaphore(GenericDelegator delegator, ModelService model) {
        this.mode = "wait".equals(model.semaphore) ? 1 : ("fail".equals(model.semaphore) ? 0 : 2);
        this.delegator = delegator;
        this.model = model;
        this.lock = null;
    }

    public void acquire() throws SemaphoreWaitException, SemaphoreFailException {
        if (mode == SEMAPHORE_MODE_NONE) return;

        String threadName = Thread.currentThread().getName();
        Timestamp lockTime = UtilDateTime.nowTimestamp();
        GenericValue semaphore;

        try {
            semaphore = delegator.findByPrimaryKey("ServiceSemaphore", UtilMisc.toMap("serviceName", model.name));
        } catch (GenericEntityException e) {
            throw new SemaphoreFailException(e);
        }

        if (semaphore == null) {
            Map fields = UtilMisc.toMap("serviceName", model.name, "lockThread", threadName, "lockTime", lockTime);
            semaphore = delegator.makeValue("ServiceSemaphore", fields);

            // use the special method below so we can reuse the unqiue tx functions
            dbWrite(semaphore, false);
        } else {             
            waitOrFail(mode);
        }
    }

    public void release() throws SemaphoreFailException {
        if (mode == SEMAPHORE_MODE_NONE) return;

        // remove the lock file
        dbWrite(lock, true);
    }

    private void waitOrFail(int mode) throws SemaphoreWaitException, SemaphoreFailException {
        switch (mode) {
            case SEMAPHORE_MODE_FAIL:
                // fail
                throw new SemaphoreFailException("Service [" + model.name + "] is locked");
            case SEMAPHORE_MODE_WAIT:
                if (wait < MAX_WAIT) {
                    ++wait;
                    try {
                        Thread.sleep(SLEEP);               
                    } catch (InterruptedException e) {
                        Debug.logInfo(e, "Sleep interrupted: ServiceSemaphone.waitOrFail()", module);
                    }

                    // try again
                    acquire();
                    break;
                } else {
                    throw new SemaphoreWaitException("Service [" + model.name + "] wait timeout exceeded");
                }
            case SEMAPHORE_MODE_NONE:
                Debug.logWarning("Semaphore mode [none] attempted to aquire a lock; but should not have!", module);
                break;
            default:
                throw new SemaphoreFailException();
        }
    }

    private synchronized void dbWrite(GenericValue value, boolean delete) throws SemaphoreFailException {
        Transaction parent = null;
        boolean beganTx = false;
        boolean isError = false;

        try {
            // prepare the suspended transaction
            parent = TransactionUtil.suspend();            
            beganTx = TransactionUtil.begin();
            if (!beganTx) {
                throw new SemaphoreFailException("Cannot obtain unique transaction for semaphore logging");
            }

            // store the value
            try {
                if (delete) {
                    value.refresh();
                    value.remove();
                    lock = null;
                } else {
                    lock = value.create();
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                isError = true;
                throw new SemaphoreFailException("Cannot obtain unique transaction for semaphore logging");
            }
        } catch (GenericTransactionException e) {
            Debug.logError(e, module);
        } finally {
            if (isError) {
                try {
                    TransactionUtil.rollback(beganTx, "ServiceSemaphore: dbWrite()", new Exception());
                } catch (GenericTransactionException e) {
                    Debug.logError(e, module);
                }
            }
            if (!isError && beganTx) {
                try {
                    TransactionUtil.commit(beganTx);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, module);
                }
            }
            if (parent != null) {
                try {
                    TransactionUtil.resume(parent);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, module);
                }
            }
        }
    }
}
