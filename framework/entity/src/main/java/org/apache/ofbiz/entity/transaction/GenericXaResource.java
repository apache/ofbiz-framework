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

import org.apache.ofbiz.base.util.Debug;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;

/**
 * GenericXaResource - Abstract XA Resource implementation supporting a single transaction
 */
public abstract class GenericXaResource extends Thread implements XAResource {

    public static final String module = GenericXaResource.class.getName();

    protected Transaction trans = null;
    protected boolean active = false;
    /** timeout is an Integer and defaults to null so that we know if it is set on this object; if it isn't set we won't worry about the warning message, etc because we don't know what the real timeout is */
    protected Integer timeout = null;
    protected Xid xid = null;

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

    /**
     * @see javax.transaction.xa.XAResource#start(javax.transaction.xa.Xid xid, int flag)
     */
    public void start(Xid xid, int flag) throws XAException {
        if (this.active) {
            if (this.xid != null && this.xid.equals(xid)) {
                throw new XAException(XAException.XAER_DUPID);
            } else {
                throw new XAException(XAException.XAER_PROTO);
            }
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

    /**
     * @see javax.transaction.xa.XAResource#end(javax.transaction.xa.Xid xid, int flag)
     */
    public void end(Xid xid, int flag) throws XAException {
        if (!this.active) {
            throw new XAException(XAException.XAER_PROTO);
        }

        if (this.xid == null || !this.xid.equals(xid)) {
            throw new XAException(XAException.XAER_NOTA);
        }
        this.active = false;
    }

    /**
     * @see javax.transaction.xa.XAResource#forget(javax.transaction.xa.Xid xid)
     */
    public void forget(Xid xid) throws XAException {
        if (this.xid == null || !this.xid.equals(xid)) {
            throw new XAException(XAException.XAER_NOTA);
        }
        this.xid = null;
        if (active) {
            // non-fatal
            Debug.logWarning("forget() called without end()", module);
        }
    }

    /**
     * @see javax.transaction.xa.XAResource#prepare(javax.transaction.xa.Xid xid)
     */
    public int prepare(Xid xid) throws XAException {
        if (this.xid == null || !this.xid.equals(xid)) {
            throw new XAException(XAException.XAER_NOTA);
        }
        return XA_OK;
    }

    /**
     * @see javax.transaction.xa.XAResource#recover(int flag)
     */
    public Xid[] recover(int flag) throws XAException {
        if (this.xid == null) {
            return new Xid[0];
        } else {
            return new Xid[] {this.xid};
        }
    }

    /**
     * @see javax.transaction.xa.XAResource#isSameRM(javax.transaction.xa.XAResource xaResource)
     */
    public boolean isSameRM(XAResource xaResource) throws XAException {
        return xaResource == this;
    }

    /**
     * @see javax.transaction.xa.XAResource#getTransactionTimeout()
     */
    public int getTransactionTimeout() throws XAException {
        return this.timeout == null ? 0 : this.timeout.intValue();
    }

    /**
     * @see javax.transaction.xa.XAResource#setTransactionTimeout(int seconds)
     * Note: the valus is saved but in the current implementation this is not used.
     */
    public boolean setTransactionTimeout(int seconds) throws XAException {
        this.timeout = (seconds == 0 ? null : Integer.valueOf(seconds));
        return true;
    }

    public Transaction getTransaction() {
        return this.trans;
    }

    public void setTransaction(Transaction t) {
        this.trans = t;
    }

    public Xid getXid() {
        return this.xid;
    }

    /**
     * @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid xid, boolean onePhase)
     */
    public abstract void commit(Xid xid, boolean onePhase) throws XAException;

    /**
     * @see javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid xid)
     */
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
                sleep(timeout.intValue() * 1000);

                if (active) {
                    // get the current status
                    int status = Status.STATUS_UNKNOWN;
                    if (trans != null) {
                        try {
                            status = trans.getStatus();
                        } catch (SystemException e) {
                            Debug.logWarning(e, module);
                        }
                    }

                    // log a warning message
                    String statusString = TransactionUtil.getTransactionStateString(status);
                    Debug.logWarning("Transaction timeout [" + timeout + "] Status: " + statusString + " Xid: " + getXid(), module);

                    // run the abstract method
                    runOnTimeout();
                }
            }
        } catch (InterruptedException e) {
            Debug.logWarning(e, "InterruptedException thrown", module);
        }
    }
}
