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
    // TODO: make sleep and max wait settings configurable per service
    // TODO: add something to make sure semaphores are cleaned up on failures and when the thread somehow goes away without cleaning it up
    // TODO: write service engine test cases to make sure semaphore both blocking and timing out (use config to set sleep and wait to low values so it times out quickly)

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
    protected int mode = SEMAPHORE_MODE_NONE;
    protected Timestamp lockTime = null;

    public ServiceSemaphore(GenericDelegator delegator, ModelService model) {
        this.delegator = delegator;
        this.mode = "wait".equals(model.semaphore) ? SEMAPHORE_MODE_WAIT : ("fail".equals(model.semaphore) ? SEMAPHORE_MODE_FAIL : SEMAPHORE_MODE_NONE);
        this.model = model;
        this.lock = null;
    }

    public void acquire() throws SemaphoreWaitException, SemaphoreFailException {
        if (mode == SEMAPHORE_MODE_NONE) return;

        lockTime = UtilDateTime.nowTimestamp();
        
        if (this.checkLockNeedToWait()) {
            waitOrFail();
        }
    }

    public void release() throws SemaphoreFailException {
        if (mode == SEMAPHORE_MODE_NONE) return;

        // remove the lock file
        dbWrite(lock, true);
    }
    
    private void waitOrFail() throws SemaphoreWaitException, SemaphoreFailException {
        if (SEMAPHORE_MODE_FAIL == mode) {
            // fail
            throw new SemaphoreFailException("Service [" + model.name + "] is locked");
        } else if (SEMAPHORE_MODE_WAIT == mode) {
            boolean timedOut = true;
            while (wait < MAX_WAIT) {
                wait++;
                try {
                    Thread.sleep(SLEEP);               
                } catch (InterruptedException e) {
                    Debug.logInfo(e, "Sleep interrupted: ServiceSemaphone.waitOrFail()", module);
                }

                // try again
                if (!checkLockNeedToWait()) {
                    timedOut = false;
                    break;
                }
            }
            if (timedOut) {
                double waitTimeSec = ((double) (System.currentTimeMillis() - lockTime.getTime()) / 1000.0);
                String errMsg = "Service [" + model.name + "] with wait semaphore exceeded wait timeout, waited [" + waitTimeSec + "], wait started at " + lockTime;
                Debug.logWarning(errMsg, module);
                throw new SemaphoreWaitException(errMsg);
            }
        } else if (SEMAPHORE_MODE_NONE == mode) {
            Debug.logWarning("Semaphore mode [none] attempted to aquire a lock; but should not have!", module);
        } else {
            throw new SemaphoreFailException("Found invalid Semaphore mode [" + mode + "]");
        }
    }

    private boolean checkLockNeedToWait() throws SemaphoreFailException {
        String threadName = Thread.currentThread().getName();
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
            
            // we own the lock, no waiting
            return false;
        } else {
            // found a semaphore, need to wait
            return true;
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
