/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.service.semaphore;

import java.sql.Timestamp;

import javax.transaction.Transaction;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.job.JobManager;

/**
 * ServiceSemaphore
 */
public final class ServiceSemaphore {
    // TODO: add something to make sure semaphores are cleaned up on failures
    //  and when the thread somehow goes away without cleaning it up
    // TODO: write service engine test cases to make sure semaphore both blocking
    //  and timing out (use config to set sleep and wait to low values so it times out quickly)

    private static final String MODULE = ServiceSemaphore.class.getName();
    private static final int SEMAPHORE_MODE_FAIL = 0;
    private static final int SEMAPHORE_MODE_WAIT = 1;
    private static final int SEMAPHORE_MODE_NONE = 2;

    private Delegator delegator;
    private GenericValue lock;
    private ModelService model;

    private int wait = 0;
    private int mode;
    private Timestamp lockTime = null;

    public ServiceSemaphore(Delegator delegator, ModelService model) {
        this.delegator = delegator;
        this.mode = "wait".equals(model.getSemaphore()) ? SEMAPHORE_MODE_WAIT
                : ("fail".equals(model.getSemaphore()) ? SEMAPHORE_MODE_FAIL : SEMAPHORE_MODE_NONE);
        this.model = model;
        this.lock = null;
    }

    /**
     * Try to acquire semaphore lock
     * @throws SemaphoreWaitException @link SemaphoreWaitException
     * @throws SemaphoreFailException @link SemaphoreFailException
     */
    public void acquire() throws SemaphoreWaitException, SemaphoreFailException {
        if (mode == SEMAPHORE_MODE_NONE) {
            return;
        }

        lockTime = UtilDateTime.nowTimestamp();

        if (this.checkLockNeedToWait()) {
            waitOrFail();
        }
    }

    /**
     * Release semaphore locks
     * @return {@code true} if release is success
     */
    public synchronized boolean release() {
        // remove the lock file
        if (mode != SEMAPHORE_MODE_NONE && lock != null) {
            return dbWrite(lock, true);
        }
        return true;
    }

    /**
     * Try to get lock ownership corresponding to semaphore type
     * Throw exception when failure.
     * @throws SemaphoreWaitException @link SemaphoreWaitException
     * @throws SemaphoreFailException @link SemaphoreFailException
     */
    private void waitOrFail() throws SemaphoreWaitException, SemaphoreFailException {
        if (SEMAPHORE_MODE_FAIL == mode) {
            // fail
            throw new SemaphoreFailException("Service [" + model.getName() + "] is locked");
        } else if (SEMAPHORE_MODE_WAIT == mode) {
            // get the wait and sleep values
            long maxWaitCount = ((model.getSemaphoreWait() * 1000) / model.getSemaphoreSleep());
            long sleep = model.getSemaphoreSleep();

            boolean timedOut = true;
            while (wait < maxWaitCount) {
                wait++;
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Debug.logInfo(e, "Sleep interrupted: ServiceSemaphore.waitOrFail()", MODULE);
                }

                // try again
                if (!checkLockNeedToWait()) {
                    timedOut = false;
                    break;
                }
            }
            if (timedOut) {
                double waitTimeSec = ((System.currentTimeMillis() - lockTime.getTime()) / 1000.0);
                String errMsg = "Service [" + model.getName() + "] with wait semaphore exceeded wait timeout, waited ["
                        + waitTimeSec + "], wait started at " + lockTime;
                throw new SemaphoreWaitException(errMsg);
            }
        } else if (SEMAPHORE_MODE_NONE == mode) {
            Debug.logWarning("Semaphore mode [none] attempted to aquire a lock; but should not have!", MODULE);
        } else {
            throw new SemaphoreFailException("Found invalid Semaphore mode [" + mode + "]");
        }
    }

    /**
     * Check the absence of the lock, if true, try to insert the lock in the synchronized way.
     * Return {@code true} if lock already in place or failed during insertion.
     * @return boolean indicating if more wait is needed
     * @throws SemaphoreFailException @link SemaphoreFailException
     */
    private boolean checkLockNeedToWait() throws SemaphoreFailException {
        String threadName = Thread.currentThread().getName();
        GenericValue semaphore;

        try {
            if (EntityQuery.use(delegator).from("ServiceSemaphore")
                    .where("serviceName", model.getName()).queryCount() == 0) {
                semaphore = delegator.makeValue("ServiceSemaphore", "serviceName", model.getName(),
                        "lockedByInstanceId", JobManager.INSTANCE_ID, "lockThread", threadName, "lockTime", lockTime);

                // use the special method below so we can reuse the unique tx functions
                // if semaphore successfully owned no need to wait anymore.
                return !dbWrite(semaphore, false);
            }
            // found a semaphore, need to wait
            return true;
        } catch (GenericEntityException e) {
            throw new SemaphoreFailException(e);
        }
    }

    /**
     * Operates synchronized jdbc access (create/remove) method to ensure unique semaphore token management
     * The same method is used for creating or removing the lock.
     * @param value  the value that will be operated
     * @param delete specify the action
     *               {@code true} for removal
     *               {@code false} for insertion
     * @return boolean if operation is success
     */
    private synchronized boolean dbWrite(GenericValue value, boolean delete) {
        Transaction parent = null;
        boolean beganTx;
        boolean isError = false;

        try {
            // prepare the suspended transaction
            if (TransactionUtil.isTransactionInPlace()) {
                parent = TransactionUtil.suspend();
            }
            beganTx = TransactionUtil.begin();
            if (!beganTx) {
                Debug.logError("Cannot obtain unique transaction for semaphore logging", MODULE);
                return false;
            }

            // store the value
            try {
                if (delete) {
                    value.refresh();
                    value.remove();
                    lock = null;
                } else {
                    // Last check before inserting data in this transaction to avoid error log
                    isError = EntityQuery.use(delegator).from("ServiceSemaphore")
                            .where("serviceName", model.getName()).queryCount() != 0;
                    if (!isError) {
                        lock = value.create();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError("Cannot obtain unique transaction for semaphore logging", MODULE);
                isError = true;
            } finally {
                try {
                    if (isError) {
                        TransactionUtil.rollback();
                    } else {
                        TransactionUtil.commit();
                    }
                } catch (GenericTransactionException e) {
                    Debug.logError(e, MODULE);
                }
            }
        } catch (GenericTransactionException e) {
            Debug.logError(e, MODULE);
        } finally {
            if (parent != null) {
                try {
                    TransactionUtil.resume(parent);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, MODULE);
                }
            }
        }
        return !isError;
    }
}
