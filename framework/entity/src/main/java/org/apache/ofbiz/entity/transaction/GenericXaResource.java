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
package org.apache.ofbiz.entity.transaction;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.ofbiz.base.util.Debug;

/**
 * GenericXaResource - Abstract XA Resource implementation supporting a single transaction
 */
public abstract class GenericXaResource extends Thread implements XAResource {

    private static final String MODULE = GenericXaResource.class.getName();

    private Transaction trans = null;
    private boolean active = false;
    /** timeout is an Integer and defaults to null so that we know if it is set on this object; if it isn't set we won't worry
     * about the warning message, etc because we don't know what the real timeout is */
    private Integer timeout = null;
    private Xid xid = null;

    /**
     * Enlists this resource in the current transaction
     * @throws XAException
     */
    public void enlist() throws XAException {
        TransactionManager tm = TransactionFactoryLoader.getInstance().getTransactionManager();
        try {
            if (tm != null && tm.getStatus() == Status.STATUS_ACTIVE) {
                Transaction tx = tm.getTransaction();
                this.enlist(tx);
            } else {
                throw new XAException("No transaction manager or invalid status");
            }
        } catch (SystemException e) {
            throw new XAException("Unable to get transaction status");
        }
    }

    /**
     * Enlists this resource in the current transaction
     * @throws XAException
     */
    public void enlist(Transaction tx) throws XAException {
        try {
            if (tx != null) {
                this.setTransaction(tx);
                tx.enlistResource(this);
            } else {
                throw new XAException(XAException.XAER_NOTA);
            }
        } catch (SystemException e) {
            throw new XAException("Unable to get transaction status");
        } catch (RollbackException e) {
            throw new XAException("Unable to enlist resource with transaction");
        }
    }

    @Override
    public void start(Xid xid, int flag) throws XAException {
        if (this.active) {
            if (this.xid != null && this.xid.equals(xid)) {
                throw new XAException(XAException.XAER_DUPID);
            }
            throw new XAException(XAException.XAER_PROTO);
        }
        if (this.xid != null && !this.xid.equals(xid)) {
            throw new XAException(XAException.XAER_NOTA);
        }

        this.setName("GenericXaResource-Thread");
        this.setDaemon(true);
        this.active = true;
        this.xid = xid;
        this.start();
    }

    @Override
    public void end(Xid xid, int flag) throws XAException {
        if (!this.active) {
            throw new XAException(XAException.XAER_PROTO);
        }

        if (this.xid == null || !this.xid.equals(xid)) {
            throw new XAException(XAException.XAER_NOTA);
        }
        this.active = false;
    }

    @Override
    public void forget(Xid xid) throws XAException {
        if (this.xid == null || !this.xid.equals(xid)) {
            throw new XAException(XAException.XAER_NOTA);
        }
        this.xid = null;
        if (active) {
            // non-fatal
            Debug.logWarning("forget() called without end()", MODULE);
        }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        if (this.xid == null || !this.xid.equals(xid)) {
            throw new XAException(XAException.XAER_NOTA);
        }
        return XA_OK;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        if (this.xid == null) {
            return new Xid[0];
        }
        return new Xid[] {this.xid};
    }

    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException {
        return xaResource == this;
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return this.timeout == null ? 0 : this.timeout;
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        this.timeout = (seconds == 0 ? null : seconds);
        return true;
    }

    /**
     * Gets transaction.
     * @return the transaction
     */
    public Transaction getTransaction() {
        return this.trans;
    }

    /**
     * Sets transaction.
     * @param t the t
     */
    public void setTransaction(Transaction t) {
        this.trans = t;
    }

    /**
     * Sets active.
     * @param active the active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Is active boolean.
     * @return the boolean
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets xid.
     * @param xid the xid
     */
    public void setXid(Xid xid) {
        this.xid = xid;
    }

    /**
     * Gets xid.
     * @return the xid
     */
    public Xid getXid() {
        return this.xid;
    }

    @Override
    public abstract void commit(Xid xid, boolean onePhase) throws XAException;

    @Override
    public abstract void rollback(Xid xid) throws XAException;

    /**
     * Method which will run when the transaction times out
     */
    public void runOnTimeout() {
    }

    // thread run method
    @Override
    public void run() {
        try {
            if (timeout != null) {
                // sleep until the transaction times out
                sleep(timeout * 1000L);

                if (active) {
                    // get the current status
                    int status = Status.STATUS_UNKNOWN;
                    if (trans != null) {
                        try {
                            status = trans.getStatus();
                        } catch (SystemException e) {
                            Debug.logWarning(e, MODULE);
                        }
                    }

                    // log a warning message
                    String statusString = TransactionUtil.getTransactionStateString(status);
                    Debug.logWarning("Transaction timeout [" + timeout + "] Status: " + statusString + " Xid: " + getXid(), MODULE);

                    // run the abstract method
                    runOnTimeout();
                }
            }
        } catch (InterruptedException e) {
            Debug.logWarning(e, "InterruptedException thrown", MODULE);
        }
    }
}
