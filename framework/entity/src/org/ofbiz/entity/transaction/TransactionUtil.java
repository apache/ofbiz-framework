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
package org.ofbiz.entity.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.sql.XAConnection;
import javax.transaction.*;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.apache.commons.collections.map.ListOrderedMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;

/**
 * <p>Transaction Utility to help with some common transaction tasks
 * <p>Provides a wrapper around the transaction objects to allow for changes in underlying implementations in the future.
 */
public class TransactionUtil implements Status {
    // Debug module name
    public static final String module = TransactionUtil.class.getName();
    public static Map debugResMap = new HashMap();
    public static boolean debugResources = true;

    /** Begins a transaction in the current thread IF transactions are available; only
     * tries if the current transaction status is ACTIVE, if not active it returns false.
     * If and on only if it begins a transaction it will return true. In other words, if
     * a transaction is already in place it will return false and do nothing.
     */
    public static boolean begin() throws GenericTransactionException {
        return begin(0);
    }

    /** Begins a transaction in the current thread IF transactions are available; only
     * tries if the current transaction status is ACTIVE, if not active it returns false.
     * If and on only if it begins a transaction it will return true. In other words, if
     * a transaction is already in place it will return false and do nothing.
     */
    public static synchronized boolean begin(int timeout) throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();
        if (ut != null) {
            try {
                int currentStatus = ut.getStatus();
                Debug.logVerbose("[TransactionUtil.begin] current status : " + getTransactionStateString(currentStatus), module);
                if (currentStatus == Status.STATUS_ACTIVE) {
                    Debug.logVerbose("[TransactionUtil.begin] active transaction in place, so no transaction begun", module);
                    return false;
                } else if (currentStatus == Status.STATUS_MARKED_ROLLBACK) {
                    Exception e = getTransactionBeginStack();
                    if (e != null) {
                        Debug.logWarning(e, "[TransactionUtil.begin] active transaction marked for rollback in place, so no transaction begun; this stack trace shows when the exception began: ", module);
                    } else {
                        Debug.logWarning("[TransactionUtil.begin] active transaction marked for rollback in place, so no transaction begun", module);
                    }

                    RollbackOnlyCause roc = getSetRollbackOnlyCause();
                    // do we have a cause? if so, throw special exception
                    if (roc != null && !roc.isEmpty()) {
                        throw new GenericTransactionException("The current transaction is marked for rollback, not beginning a new transaction and aborting current operation; the rollbackOnly was caused by: " + roc.getCauseMessage(), roc.getCauseThrowable());
                    } else {
                        return false;
                    }
                }

                // set the timeout for THIS transaction
                if (timeout > 0) {
                    ut.setTransactionTimeout(timeout);
                    Debug.logVerbose("[TransactionUtil.begin] set transaction timeout to : " + timeout + " seconds", module);
                }

                // begin the transaction
                ut.begin();
                Debug.logVerbose("[TransactionUtil.begin] transaction begun", module);

                // reset the timeout to the default
                if (timeout > 0) {
                    ut.setTransactionTimeout(0);
                }

                // reset the transaction stamps, just in case...
                clearTransactionStamps();
                // initialize the start stamp
                getTransactionStartStamp();
                // set the tx begin stack placeholder
                setTransactionBeginStack();

                // initialize the debug resource
                if (debugResources) {
                    DebugXaResource dxa = new DebugXaResource();
                    try {
                        dxa.enlist();
                    } catch (XAException e) {
                        Debug.logError(e, module);
                    }
                }

                return true;
            } catch (NotSupportedException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Not Supported error, could not begin transaction (probably a nesting problem)", e);
            } catch (SystemException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not begin transaction", e);
            }
        } else {
            Debug.logInfo("[TransactionUtil.begin] no user transaction, so no transaction begun", module);
            return false;
        }
    }

    /** Gets the status of the transaction in the current thread IF
     * transactions are available, otherwise returns STATUS_NO_TRANSACTION */
    public static int getStatus() throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();
        if (ut != null) {
            try {
                return ut.getStatus();
            } catch (SystemException e) {
                throw new GenericTransactionException("System error, could not get status", e);
            }
        } else {
            return STATUS_NO_TRANSACTION;
        }
    }

    public static boolean isTransactionInPlace() throws GenericTransactionException {
        int status = getStatus();
        if (status == STATUS_NO_TRANSACTION) {
            return false;
        } else {
            return true;
        }
    }


    /** Commits the transaction in the current thread IF transactions are available
     *  AND if beganTransaction is true
     */
    public static void commit(boolean beganTransaction) throws GenericTransactionException {
        if (beganTransaction) {
            TransactionUtil.commit();
        }
    }

    /** Commits the transaction in the current thread IF transactions are available */
    public static void commit() throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();

        if (ut != null) {
            try {
                int status = ut.getStatus();
                Debug.logVerbose("[TransactionUtil.commit] current status : " + getTransactionStateString(status), module);

                if (status != STATUS_NO_TRANSACTION) {
                    ut.commit();

                    // clear out the stamps to keep it clean
                    clearTransactionStamps();
                    // clear out the stack too
                    clearTransactionBeginStack();
                    clearSetRollbackOnlyCause();

                    Debug.logVerbose("[TransactionUtil.commit] transaction committed", module);
                } else {
                    Debug.logInfo("[TransactionUtil.commit] Not committing transaction, status is STATUS_NO_TRANSACTION", module);
                }
            } catch (RollbackException e) {
                RollbackOnlyCause rollbackOnlyCause = getSetRollbackOnlyCause();

                if (rollbackOnlyCause != null) {
                    // the transaction is now definitely over, so clear stuff as normal now that we have the info from it that we want
                    clearTransactionStamps();
                    clearTransactionBeginStack();
                    clearSetRollbackOnlyCause();
                    
                    Debug.logError(e, "Rollback Only was set when trying to commit transaction here; throwing rollbackOnly cause exception", module);
                    throw new GenericTransactionException("Roll back error, could not commit transaction, was rolled back instead because of: " + rollbackOnlyCause.getCauseMessage(), rollbackOnlyCause.getCauseThrowable());
                } else {
                    Throwable t = e.getCause() == null ? e : e.getCause();
                    throw new GenericTransactionException("Roll back error (with no rollbackOnly cause found), could not commit transaction, was rolled back instead: " + t.toString(), t);
                }
            } catch (HeuristicMixedException e) {
                Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Could not commit transaction, HeuristicMixed exception: " + t.toString(), t);
            } catch (HeuristicRollbackException e) {
                Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Could not commit transaction, HeuristicRollback exception: " + t.toString(), t);
            } catch (SystemException e) {
                Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not commit transaction: " + t.toString(), t);
            }
        } else {
            Debug.logInfo("[TransactionUtil.commit] UserTransaction is null, not commiting", module);
        }
    }

    /** @deprecated */
    public static void rollback(boolean beganTransaction) throws GenericTransactionException {
        Debug.logWarning("WARNING: called rollback without debug/error info; it is recommended to always pass this to make otherwise tricky bugs much easier to track down.", module);
        rollback(beganTransaction, null, null);
    }
    
    /** Rolls back transaction in the current thread IF transactions are available
     *  AND if beganTransaction is true; if beganTransaction is not true,
     *  setRollbackOnly is called to insure that the transaction will be rolled back
     */
    public static void rollback(boolean beganTransaction, String causeMessage, Throwable causeThrowable) throws GenericTransactionException {
        if (beganTransaction) {
            TransactionUtil.rollback();
        } else {
            TransactionUtil.setRollbackOnly(causeMessage, causeThrowable);
        }
    }

    /** Rolls back transaction in the current thread IF transactions are available */
    public static void rollback() throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();

        if (ut != null) {
            try {
                int status = ut.getStatus();
                Debug.logVerbose("[TransactionUtil.rollback] current status : " + getTransactionStateString(status), module);

                if (status != STATUS_NO_TRANSACTION) {
                    //if (Debug.infoOn()) Thread.dumpStack();
                    if (Debug.infoOn()) {
                        Exception newE = new Exception("Stack Trace");
                        Debug.logError(newE, "[TransactionUtil.rollback]", module);
                    }

                    // clear out the stamps to keep it clean
                    clearTransactionStamps();
                    // clear out the stack too
                    clearTransactionBeginStack();
                    clearSetRollbackOnlyCause();

                    ut.rollback();
                    Debug.logInfo("[TransactionUtil.rollback] transaction rolled back", module);
                } else {
                    Debug.logInfo("[TransactionUtil.rollback] transaction not rolled back, status is STATUS_NO_TRANSACTION", module);
                }
            } catch (SystemException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not rollback transaction", e);
            }
        } else {
            Debug.logInfo("[TransactionUtil.rollback] No UserTransaction, transaction not rolled back", module);
        }
    }

    /** Makes a rollback the only possible outcome of the transaction in the current thread IF transactions are available */
    public static void setRollbackOnly(String causeMessage, Throwable causeThrowable) throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();
        if (ut != null) {
            try {
                int status = ut.getStatus();
                Debug.logVerbose("[TransactionUtil.setRollbackOnly] current code : " + getTransactionStateString(status), module);

                if (status != STATUS_NO_TRANSACTION) {
                    if (status != STATUS_MARKED_ROLLBACK) {
                        if (Debug.warningOn()) Debug.logWarning(new Exception(causeMessage), "[TransactionUtil.setRollbackOnly] Calling transaction setRollbackOnly; this stack trace shows where this is happening:", module);
                        ut.setRollbackOnly();
                        setSetRollbackOnlyCause(causeMessage, causeThrowable);
                    } else {
                        Debug.logInfo("[TransactionUtil.setRollbackOnly] transaction rollback only not set, rollback only is already set.", module);
                    }
                } else {
                    Debug.logInfo("[TransactionUtil.setRollbackOnly] transaction rollback only not set, status is STATUS_NO_TRANSACTION", module);
                }
            } catch (SystemException e) {
                //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not set rollback only on transaction", e);
            }
        } else {
            Debug.logInfo("[TransactionUtil.setRollbackOnly] No UserTransaction, transaction rollback only not set", module);
        }
    }

    public static Transaction suspend() throws GenericTransactionException {
        try {
            if (TransactionUtil.getStatus() == TransactionUtil.STATUS_ACTIVE) {
                TransactionManager txMgr = TransactionFactory.getTransactionManager();
                if (txMgr != null ) {
                    pushTransactionBeginStackSave(clearTransactionBeginStack());
                    pushSetRollbackOnlyCauseSave(clearSetRollbackOnlyCause());
                    Transaction trans = txMgr.suspend();
                    pushSuspendedTransaction(trans);
                    return trans;
                } else {
                    return null;
                }
            } else {
                Debug.logWarning("No transaction active, so not suspending.", module);
                return null;
            }
        } catch (SystemException e) {
            throw new GenericTransactionException("System error, could not suspend transaction", e);
        }
    }

    public static void resume(Transaction parentTx) throws GenericTransactionException {
        if (parentTx == null) return;
        try {
            TransactionManager txMgr = TransactionFactory.getTransactionManager();
            if (txMgr != null ) {
                setTransactionBeginStack(popTransactionBeginStackSave());
                setSetRollbackOnlyCause(popSetRollbackOnlyCauseSave());
                txMgr.resume(parentTx);
                removeSuspendedTransaction(parentTx);
            }
        } catch (InvalidTransactionException e) {
            /* NOTE: uncomment this for Weblogic Application Server
            // this is a work-around for non-standard Weblogic functionality; for more information see: http://www.onjava.com/pub/a/onjava/2005/07/20/transactions.html?page=3
            if (parentTx instanceof weblogic.transaction.ClientTransactionManager) {
                // WebLogic 8 and above
                ((weblogic.transaction.ClientTransactionManager) parentTx).forceResume(transaction);
            } else if (parentTx instanceof weblogic.transaction.TransactionManager) {
                // WebLogic 7
                ((weblogic.transaction.TransactionManager) parentTx).forceResume(transaction);
            } else {
                throw new GenericTransactionException("System error, could not resume transaction", e);
            }
            */
            throw new GenericTransactionException("System error, could not resume transaction", e);
        } catch (SystemException e) {
            throw new GenericTransactionException("System error, could not resume transaction", e);
        }
    }

    /** Sets the timeout of the transaction in the current thread IF transactions are available */
    public static void setTransactionTimeout(int seconds) throws GenericTransactionException {
        UserTransaction ut = TransactionFactory.getUserTransaction();
        if (ut != null) {
            try {
                ut.setTransactionTimeout(seconds);
            } catch (SystemException e) {
                throw new GenericTransactionException("System error, could not set transaction timeout", e);
            }
        }
    }

    /** Enlists the given XAConnection and if a transaction is active in the current thread, returns a plain JDBC Connection */
    public static Connection enlistConnection(XAConnection xacon) throws GenericTransactionException {
        if (xacon == null) {
            return null;
        }
        try {
            XAResource resource = xacon.getXAResource();
            TransactionUtil.enlistResource(resource);
            return xacon.getConnection();
        } catch (SQLException e) {
            throw new GenericTransactionException("SQL error, could not enlist connection in transaction even though transactions are available", e);
        }
    }

    public static void enlistResource(XAResource resource) throws GenericTransactionException {
        if (resource == null) {
            return;
        }

        try {
            TransactionManager tm = TransactionFactory.getTransactionManager();
            if (tm != null && tm.getStatus() == STATUS_ACTIVE) {
                Transaction tx = tm.getTransaction();
                if (tx != null) {
                     tx.enlistResource(resource);
                }
            }
        } catch (RollbackException e) {
            //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
            throw new GenericTransactionException("Roll Back error, could not enlist resource in transaction even though transactions are available, current transaction rolled back", e);
        } catch (SystemException e) {
            //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
            throw new GenericTransactionException("System error, could not enlist resource in transaction even though transactions are available", e);
        }
    }

    public static String getTransactionStateString(int state) {
        switch (state) {
            case Status.STATUS_ACTIVE:
                return "Transaction Active (" + state + ")";
            case Status.STATUS_COMMITTED:
                return "Transaction Committed (" + state + ")";
            case Status.STATUS_COMMITTING:
                return "Transaction Committing (" + state + ")";
            case Status.STATUS_MARKED_ROLLBACK:
                return "Transaction Marked Rollback (" + state + ")";
            case Status.STATUS_NO_TRANSACTION:
                return "No Transaction (" + state + ")";
            case Status.STATUS_PREPARED:
                return "Transaction Prepared (" + state + ")";
            case Status.STATUS_PREPARING:
                return "Transaction Preparing (" + state + ")";
            case Status.STATUS_ROLLEDBACK:
                return "Transaction Rolledback (" + state + ")";
            case Status.STATUS_ROLLING_BACK:
                return "Transaction Rolling Back (" + state + ")";
            case Status.STATUS_UNKNOWN:
                return "Transaction Status Unknown (" + state + ")";
            default:
                return "Not a valid state code (" + state + ")";
        }
    }

    public static void logRunningTx() {
        if (debugResources) {
            if (debugResMap != null && debugResMap.size() > 0) {
                Iterator i = debugResMap.keySet().iterator();
                while (i.hasNext()) {
                    Object o = i.next();
                    DebugXaResource dxa = (DebugXaResource) debugResMap.get(o);
                    dxa.log();
                }
            }
        }
    }

    public static void registerSynchronization(Synchronization sync) throws GenericTransactionException {
        if (sync == null) {
            return;
        }

        try {
            TransactionManager tm = TransactionFactory.getTransactionManager();
            if (tm != null && tm.getStatus() == STATUS_ACTIVE) {
                Transaction tx = tm.getTransaction();
                if (tx != null) {
                    tx.registerSynchronization(sync);
                }
            }
        } catch (RollbackException e) {
            //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
            throw new GenericTransactionException("Roll Back error, could not register synchronization in transaction even though transactions are available, current transaction rolled back", e);
        } catch (SystemException e) {
            //This is Java 1.4 only, but useful for certain debuggins: Throwable t = e.getCause() == null ? e : e.getCause();
            throw new GenericTransactionException("System error, could not register synchronization in transaction even though transactions are available", e);
        }
    }

    // =======================================
    // =======================================
    private static ThreadLocal suspendedTxStack = new ThreadLocal();

    /** BE VERY CARFUL WHERE YOU CALL THIS!! */
    public static int cleanSuspendedTransactions() throws GenericTransactionException {
        Transaction trans = null;
        int num = 0;
        while ((trans = popSuspendedTransaction()) != null) {
            resume(trans);
            rollback();
            num++;
        }
        // no transaction stamps to remember anymore ;-)
        clearTransactionStartStampStack();
        return num;
    }
    public static boolean suspendedTransactionsHeld() {
        List tl = (List) suspendedTxStack.get();
        if (tl != null && tl.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
    protected static void pushSuspendedTransaction(Transaction t) {
        List tl = (List) suspendedTxStack.get();
        if (tl == null) {
            tl = new LinkedList();
            suspendedTxStack.set(tl);
        }
        tl.add(0, t);
        // save the current transaction start stamp
        pushTransactionStartStamp(t);
    }
    protected static Transaction popSuspendedTransaction() {
        List tl = (List) suspendedTxStack.get();
        if (tl != null && tl.size() > 0) {
            // restore the transaction start stamp
            popTransactionStartStamp();
            return (Transaction) tl.remove(0);
        } else {
            return null;
        }
    }
    protected static void removeSuspendedTransaction(Transaction t) {
        List tl = (List) suspendedTxStack.get();
        if (tl != null && tl.size() > 0) {
            tl.remove(t);
            popTransactionStartStamp(t);
        }
    }

    // =======================================
    // =======================================
    private static ThreadLocal transactionBeginStack = new ThreadLocal();
    private static ThreadLocal transactionBeginStackSave = new ThreadLocal();

    private static void pushTransactionBeginStackSave(Exception e) {
        List el = (List) transactionBeginStackSave.get();
        if (el == null) {
            el = new LinkedList();
            transactionBeginStackSave.set(el);
        }
        el.add(0, e);
    }
    private static Exception popTransactionBeginStackSave() {
        List el = (List) transactionBeginStackSave.get();
        if (el != null && el.size() > 0) {
            return (Exception) el.remove(0);
        } else {
            return null;
        }
    }

    private static void setTransactionBeginStack() {
        Exception e = new Exception("Tx Stack Placeholder");
        setTransactionBeginStack(e);
    }
    private static void setTransactionBeginStack(Exception newExc) {
        if (transactionBeginStack.get() != null) {
            Exception e = (Exception) transactionBeginStack.get();
            Debug.logWarning(e, "WARNING: In setTransactionBeginStack a stack placeholder was already in place, here is where the transaction began: ", module);
            Exception e2 = new Exception("Current Stack Trace");
            Debug.logWarning(e2, "WARNING: In setTransactionBeginStack a stack placeholder was already in place, here is the current location: ", module);
        }
        transactionBeginStack.set(newExc);
    }
    private static Exception clearTransactionBeginStack() {
        Exception e = (Exception) transactionBeginStack.get();
        if (e == null) {
            Exception e2 = new Exception("Current Stack Trace");
            Debug.logWarning(e2, "WARNING: In clearTransactionBeginStack no stack placeholder was in place, here is the current location: ", module);
            return null;
        } else {
            transactionBeginStack.set(null);
            return e;
        }
    }
    public static Exception getTransactionBeginStack() {
        Exception e = (Exception) transactionBeginStack.get();
        if (e == null) {
            Exception e2 = new Exception("Current Stack Trace");
            Debug.logWarning(e2, "WARNING: In getTransactionBeginStack no stack placeholder was in place, here is the current location: ", module);
        }
        return e;
    }

    // =======================================
    // =======================================
    private static class RollbackOnlyCause {
        protected String causeMessage;
        protected Throwable causeThrowable;
        public RollbackOnlyCause(String causeMessage, Throwable causeThrowable) {
            this.causeMessage = causeMessage;
            this.causeThrowable = causeThrowable;
        }
        public String getCauseMessage() { return this.causeMessage + (this.causeThrowable == null ? "" : this.causeThrowable.toString()); }
        public Throwable getCauseThrowable() { return this.causeThrowable; }
        public void logError(String message) { Debug.logError(this.getCauseThrowable(), (message == null ? "" : message) + this.getCauseMessage(), module); }
        public boolean isEmpty() { return (UtilValidate.isEmpty(this.getCauseMessage()) && this.getCauseThrowable() == null); }
    }
    
    private static ThreadLocal setRollbackOnlyCause = new ThreadLocal();
    private static ThreadLocal setRollbackOnlyCauseSave = new ThreadLocal();

    private static void pushSetRollbackOnlyCauseSave(RollbackOnlyCause e) {
        List el = (List) setRollbackOnlyCauseSave.get();
        if (el == null) {
            el = new LinkedList();
            setRollbackOnlyCauseSave.set(el);
        }
        el.add(0, e);
    }
    private static RollbackOnlyCause popSetRollbackOnlyCauseSave() {
        List el = (List) setRollbackOnlyCauseSave.get();
        if (el != null && el.size() > 0) {
            return (RollbackOnlyCause) el.remove(0);
        } else {
            return null;
        }
    }

    private static void setSetRollbackOnlyCause(String causeMessage, Throwable causeThrowable) {
        RollbackOnlyCause roc = new RollbackOnlyCause(causeMessage, causeThrowable);
        setSetRollbackOnlyCause(roc);
    }
    private static void setSetRollbackOnlyCause(RollbackOnlyCause newRoc) {
        if (setRollbackOnlyCause.get() != null) {
            RollbackOnlyCause roc = (RollbackOnlyCause) setRollbackOnlyCause.get();
            roc.logError("WARNING: In setSetRollbackOnlyCause a stack placeholder was already in place, here is the original rollbackOnly cause: ");
            Exception e2 = new Exception("Current Stack Trace");
            Debug.logWarning(e2, "WARNING: In setSetRollbackOnlyCause a stack placeholder was already in place, here is the current location: ", module);
        }
        setRollbackOnlyCause.set(newRoc);
    }
    private static RollbackOnlyCause clearSetRollbackOnlyCause() {
        RollbackOnlyCause roc = (RollbackOnlyCause) setRollbackOnlyCause.get();
        if (roc == null) {
            /* this is an obnoxious message, leaving out for now; could be added manually if a problem with this is suspected
            if (Debug.verboseOn()) {
                // for this in particular, unlike the begin location, normally there will not be a setRollbackOnlyCause, so don't complain about it except in verbose
                Debug.logVerbose(new Exception("Current Stack Trace"), "In clearSetRollbackOnlyCause no stack placeholder was in place, here is the current location: ", module);
            }
            */
            return null;
        } else {
            setRollbackOnlyCause.set(null);
            return roc;
        }
    }
    public static RollbackOnlyCause getSetRollbackOnlyCause() {
        if (setRollbackOnlyCause.get() == null) {
            Exception e = new Exception("Current Stack Trace");
            Debug.logWarning(e, "WARNING: In getSetRollbackOnlyCause no stack placeholder was in place, here is the current location: ", module);
        }
        return (RollbackOnlyCause) setRollbackOnlyCause.get();
    }

    // =======================================
    // =======================================
    
    /**
     * Maintain the suspended transactions together with their timestamps
     */
    private static ThreadLocal suspendedTxStartStamps = new ThreadLocal() {
        public Object initialValue() {
            return new ListOrderedMap();
        }
    };
    
    /**
    * Put the stamp to remember later
    * @param t transaction just suspended
    */
    private static void pushTransactionStartStamp(Transaction t) {
        Map map = (Map) suspendedTxStartStamps.get();
        Timestamp stamp = (Timestamp) transactionStartStamp.get();
        if (stamp != null) {
            map.put(t, stamp);
        } else {
            Debug.logError("Error in transaction handling - no start stamp to push.", module);
        }
    }


    /**
    * Method called when the suspended stack gets cleaned by {@link #cleanSuspendedTransactions()}.
    */
    private static void clearTransactionStartStampStack() {
        ((Map) suspendedTxStartStamps.get()).clear();
    }

    /**
    * Remove the stamp of the specified transaction from stack (when resuming)
    * and set it as current start stamp.
    * @param t transaction just resumed
    */
    private static void popTransactionStartStamp(Transaction t) {
        Map map = (Map) suspendedTxStartStamps.get();
        if (map.size() > 0) {
            Timestamp stamp = (Timestamp) map.remove(t);
            if (stamp != null) {
                transactionStartStamp.set(stamp);
            } else {
                Debug.logError("Error in transaction handling - no saved start stamp found - using NOW.", module);
                transactionStartStamp.set(UtilDateTime.nowTimestamp());
            }
        }
    }

    /**
    * Remove the stamp from stack (when resuming)
    */
    private static void popTransactionStartStamp() {
        ListOrderedMap map = (ListOrderedMap) suspendedTxStartStamps.get();
        if (map.size() > 0) {
            transactionStartStamp.set(map.remove(map.lastKey()));
        } else {
            Debug.logError("Error in transaction handling - no saved start stamp found - using NOW.", module);
            transactionStartStamp.set(UtilDateTime.nowTimestamp());
        }
    }

    private static ThreadLocal transactionStartStamp = new ThreadLocal();
    private static ThreadLocal transactionLastNowStamp = new ThreadLocal();

    public static Timestamp getTransactionStartStamp() {
        Timestamp curStamp = (Timestamp) transactionStartStamp.get();
        if (curStamp == null) {
            curStamp = UtilDateTime.nowTimestamp();
            transactionStartStamp.set(curStamp);

            // we know this is the first time set for this transaction, so make sure the StampClearSync is registered
            try {
                registerSynchronization(new StampClearSync());
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Error registering StampClearSync synchronization, stamps will still be reset if begin/commit/rollback are call through TransactionUtil, but not if otherwise", module);
            }
        }
        return curStamp;
    }

    public static Timestamp getTransactionUniqueNowStamp() {
        Timestamp lastNowStamp = (Timestamp) transactionLastNowStamp.get();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        // check for an overlap with the lastNowStamp, or if the lastNowStamp is in the future because of incrementing to make each stamp unique
        if (lastNowStamp != null && (lastNowStamp.equals(nowTimestamp) || lastNowStamp.after(nowTimestamp))) {
            nowTimestamp = new Timestamp(lastNowStamp.getTime() + 1);
        }

        transactionLastNowStamp.set(nowTimestamp);
        return nowTimestamp;
    }

    protected static void clearTransactionStamps() {
        transactionStartStamp.set(null);
        transactionLastNowStamp.set(null);
    }

    public static class StampClearSync implements Synchronization {
        public void afterCompletion(int status) {
            TransactionUtil.clearTransactionStamps();
        }

        public void beforeCompletion() {
        }
    }
}
