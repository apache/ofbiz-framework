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
public class ServiceSemaphore {
    // TODO: add something to make sure semaphores are cleaned up on failures and when the thread somehow goes away without cleaning it up
    // TODO: write service engine test cases to make sure semaphore both blocking and timing out (use config to set sleep and wait to low values so it times out quickly)

    public static final String module = ServiceSemaphore.class.getName();
    public static final int SEMAPHORE_MODE_FAIL = 0;
    public static final int SEMAPHORE_MODE_WAIT = 1;
    public static final int SEMAPHORE_MODE_NONE = 2;

    protected Delegator delegator;
    protected GenericValue lock;
    protected ModelService model;

    protected int wait = 0;
    protected int mode = SEMAPHORE_MODE_NONE;
    protected Timestamp lockTime = null;

    public ServiceSemaphore(Delegator delegator, ModelService model) {
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
        if (lock != null) {
            dbWrite(lock, true);
        }
    }

    private void waitOrFail() throws SemaphoreWaitException, SemaphoreFailException {
        if (SEMAPHORE_MODE_FAIL == mode) {
            // fail
            throw new SemaphoreFailException("Service [" + model.name + "] is locked");
        } else if (SEMAPHORE_MODE_WAIT == mode) {
            // get the wait and sleep values
            long maxWaitCount = ((model.semaphoreWait * 1000) / model.semaphoreSleep);
            long sleep = model.semaphoreSleep;

            boolean timedOut = true;
            while (wait < maxWaitCount) {
                wait++;
                try {
                    Thread.sleep(sleep);
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
                double waitTimeSec = ((System.currentTimeMillis() - lockTime.getTime()) / 1000.0);
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
            semaphore = EntityQuery.use(delegator).from("ServiceSemaphore").where("serviceName", model.name).queryOne();
        } catch (GenericEntityException e) {
            throw new SemaphoreFailException(e);
        }

        if (semaphore == null) {
            semaphore = delegator.makeValue("ServiceSemaphore", "serviceName", model.name, "lockedByInstanceId", JobManager.instanceId, "lockThread", threadName, "lockTime", lockTime);

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
            }
        } catch (GenericTransactionException e) {
            Debug.logError(e, module);
        } finally {
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
