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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.sql.XAConnection;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.jdbc.CursorConnection;

/**
 * <p>Transaction Utility to help with some common transaction tasks
 * <p>Provides a wrapper around the transaction objects to allow for changes in underlying implementations in the future.
 */
public final class TransactionUtil implements Status {
    // Debug module name
    public static final String module = TransactionUtil.class.getName();

    private static ThreadLocal<List<Transaction>> suspendedTxStack = new ThreadLocal<List<Transaction>>();
    private static ThreadLocal<List<Exception>> suspendedTxLocationStack = new ThreadLocal<List<Exception>>();
    private static ThreadLocal<Exception> transactionBeginStack = new ThreadLocal<Exception>();
    private static ThreadLocal<List<Exception>> transactionBeginStackSave = new ThreadLocal<List<Exception>>();
    private static ThreadLocal<RollbackOnlyCause> setRollbackOnlyCause = new ThreadLocal<RollbackOnlyCause>();
    private static ThreadLocal<List<RollbackOnlyCause>> setRollbackOnlyCauseSave = new ThreadLocal<List<RollbackOnlyCause>>();
    private static ThreadLocal<Timestamp> transactionStartStamp = new ThreadLocal<Timestamp>();
    private static ThreadLocal<Timestamp> transactionLastNowStamp = new ThreadLocal<Timestamp>();

    private static final boolean debugResources = readDebugResources();
    public static Map<Xid, DebugXaResource> debugResMap = Collections.<Xid, DebugXaResource>synchronizedMap(new HashMap<Xid, DebugXaResource>());
    // in order to improve performance allThreadsTransactionBeginStack and allThreadsTransactionBeginStackSave are only maintained when logging level INFO is on
    private static Map<Long, Exception> allThreadsTransactionBeginStack = Collections.<Long, Exception>synchronizedMap(new HashMap<Long, Exception>());
    private static Map<Long, List<Exception>> allThreadsTransactionBeginStackSave = Collections.<Long, List<Exception>>synchronizedMap(new HashMap<Long, List<Exception>>());

    private TransactionUtil () {}
    public static <V> V doNewTransaction(Callable<V> callable, String ifErrorMessage, int timeout, boolean printException) throws GenericEntityException {
        return noTransaction(inTransaction(callable, ifErrorMessage, timeout, printException)).call();
    }

    public static <V> V doTransaction(Callable<V> callable, String ifErrorMessage, int timeout, boolean printException) throws GenericEntityException {
        return inTransaction(callable, ifErrorMessage, timeout, printException).call();
    }

    public static <V> NoTransaction<V> noTransaction(Callable<V> callable) {
        return new NoTransaction<V>(callable);
    }

    // This syntax is groovy compatible, with the primary(callable) as the first arg.
    // You could do:
    // use (TransactionUtil) {
    //   Callable callable = ....
    //   Object result = callable.noTransaction().inTransaction(ifError, timeout, print).call()
    // }
    public static <V> InTransaction<V> inTransaction(Callable<V> callable, String ifErrorMessage, int timeout, boolean printException) {
        return new InTransaction<V>(callable, ifErrorMessage, timeout, printException);
    }

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
    public static boolean begin(int timeout) throws GenericTransactionException {
        UserTransaction ut = TransactionFactoryLoader.getInstance().getUserTransaction();
        if (ut != null) {
            try {
                int currentStatus = ut.getStatus();
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Current status : " + getTransactionStateString(currentStatus), module);
                }
                if (currentStatus == Status.STATUS_ACTIVE) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Active transaction in place, so no transaction begun", module);
                    }
                    return false;
                } else if (currentStatus == Status.STATUS_MARKED_ROLLBACK) {
                    Exception e = getTransactionBeginStack();
                    if (e != null) {
                        Debug.logWarning(e, "Active transaction marked for rollback in place, so no transaction begun; this stack trace shows when the exception began: ", module);
                    } else {
                        Debug.logWarning("Active transaction marked for rollback in place, so no transaction begun", module);
                    }

                    RollbackOnlyCause roc = getSetRollbackOnlyCause();
                    // do we have a cause? if so, throw special exception
                    if (UtilValidate.isNotEmpty(roc)) {
                        throw new GenericTransactionException("The current transaction is marked for rollback, not beginning a new transaction and aborting current operation; the rollbackOnly was caused by: " + roc.getCauseMessage(), roc.getCauseThrowable());
                    } else {
                        return false;
                    }
                }

                internalBegin(ut, timeout);

                // reset the transaction stamps, just in case...
                clearTransactionStamps();
                // initialize the start stamp
                getTransactionStartStamp();
                // set the tx begin stack placeholder
                setTransactionBeginStack();

                // initialize the debug resource
                if (debugResources()) {
                    DebugXaResource dxa = new DebugXaResource();
                    try {
                        dxa.enlist();
                    } catch (XAException e) {
                        Debug.logError(e, module);
                    }
                }

                return true;
            } catch (NotSupportedException e) {
                throw new GenericTransactionException("Not Supported error, could not begin transaction (probably a nesting problem)", e);
            } catch (SystemException e) {
                throw new GenericTransactionException("System error, could not begin transaction", e);
            }
        } else {
            if (Debug.infoOn()) Debug.logInfo("No user transaction, so no transaction begun", module);
            return false;
        }
    }

    protected static void internalBegin(UserTransaction ut, int timeout) throws SystemException, NotSupportedException {
        // set the timeout for THIS transaction
        if (timeout > 0) {
            ut.setTransactionTimeout(timeout);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Set transaction timeout to : " + timeout + " seconds", module);
            }
        }

        // begin the transaction
        ut.begin();
        if (Debug.verboseOn()) {
            Debug.logVerbose("Transaction begun", module);
        }

        // reset the timeout to the default
        if (timeout > 0) {
            ut.setTransactionTimeout(0);
        }
    }

    /** Gets the status of the transaction in the current thread IF
     * transactions are available, otherwise returns STATUS_NO_TRANSACTION */
    public static int getStatus() throws GenericTransactionException {
        UserTransaction ut = TransactionFactoryLoader.getInstance().getUserTransaction();
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

    public static String getStatusString() throws GenericTransactionException {
        return getTransactionStateString(getStatus());
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
        UserTransaction ut = TransactionFactoryLoader.getInstance().getUserTransaction();

        if (ut != null) {
            try {
                int status = ut.getStatus();
                Debug.logVerbose("Current status : " + getTransactionStateString(status), module);

                if (status != STATUS_NO_TRANSACTION && status != STATUS_COMMITTING && status != STATUS_COMMITTED && status != STATUS_ROLLING_BACK && status != STATUS_ROLLEDBACK) {
                    ut.commit();

                    // clear out the stamps to keep it clean
                    clearTransactionStamps();
                    // clear out the stack too
                    clearTransactionBeginStack();
                    clearSetRollbackOnlyCause();

                    Debug.logVerbose("Transaction committed", module);
                } else {
                    Debug.logWarning("Not committing transaction, status is " + getStatusString(), module);
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
            } catch (IllegalStateException e) {
                Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Could not commit transaction, IllegalStateException exception: " + t.toString(), t);
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
            Debug.logInfo("UserTransaction is null, not committing", module);
        }
    }

    /** Rolls back transaction in the current thread IF transactions are available
     *  AND if beganTransaction is true; if beganTransaction is not true,
     *  setRollbackOnly is called to insure that the transaction will be rolled back
     */
    public static void rollback(boolean beganTransaction, String causeMessage, Throwable causeThrowable) throws GenericTransactionException {
        if (beganTransaction) {
            TransactionUtil.rollback(causeThrowable);
        } else {
            TransactionUtil.setRollbackOnly(causeMessage, causeThrowable);
        }
    }

    /** Rolls back transaction in the current thread IF transactions are available */
    public static void rollback() throws GenericTransactionException {
        rollback(null);
    }

    /** Rolls back transaction in the current thread IF transactions are available */
    public static void rollback(Throwable causeThrowable) throws GenericTransactionException {
        UserTransaction ut = TransactionFactoryLoader.getInstance().getUserTransaction();

        if (ut != null) {
            try {
                int status = ut.getStatus();
                Debug.logVerbose("Current status : " + getTransactionStateString(status), module);

                if (status != STATUS_NO_TRANSACTION) {
                    //if (Debug.infoOn()) Thread.dumpStack();
                    if (causeThrowable == null && Debug.infoOn()) {
                        Exception newE = new Exception("Stack Trace");
                        Debug.logError(newE, "[TransactionUtil.rollback]", module);
                    }

                    // clear out the stamps to keep it clean
                    clearTransactionStamps();
                    // clear out the stack too
                    clearTransactionBeginStack();
                    clearSetRollbackOnlyCause();

                    ut.rollback();
                    Debug.logInfo("Transaction rolled back", module);
                } else {
                    Debug.logWarning("Transaction not rolled back, status is STATUS_NO_TRANSACTION", module);
                }
            } catch (IllegalStateException e) {
                Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Could not rollback transaction, IllegalStateException exception: " + t.toString(), t);
            } catch (SystemException e) {
                Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not rollback transaction: " + t.toString(), t);
            }
        } else {
            Debug.logInfo("No UserTransaction, transaction not rolled back", module);
        }
    }

    /** Makes a rollback the only possible outcome of the transaction in the current thread IF transactions are available */
    public static void setRollbackOnly(String causeMessage, Throwable causeThrowable) throws GenericTransactionException {
        UserTransaction ut = TransactionFactoryLoader.getInstance().getUserTransaction();
        if (ut != null) {
            try {
                int status = ut.getStatus();
                Debug.logVerbose("Current code : " + getTransactionStateString(status), module);

                if (status != STATUS_NO_TRANSACTION) {
                    if (status != STATUS_MARKED_ROLLBACK) {
                        if (Debug.warningOn()) {
                            Debug.logWarning(new Exception(causeMessage), "Calling transaction setRollbackOnly; this stack trace shows where this is happening:", module);
                        }
                        ut.setRollbackOnly();
                        setSetRollbackOnlyCause(causeMessage, causeThrowable);
                    } else {
                        Debug.logInfo("Transaction rollback only not set, rollback only is already set.", module);
                    }
                } else {
                    Debug.logWarning("Transaction rollback only not set, status is STATUS_NO_TRANSACTION", module);
                }
            } catch (IllegalStateException e) {
                Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("Could not set rollback only on transaction, IllegalStateException exception: " + t.toString(), t);
            } catch (SystemException e) {
                Throwable t = e.getCause() == null ? e : e.getCause();
                throw new GenericTransactionException("System error, could not set rollback only on transaction: " + t.toString(), t);
            }
        } else {
            Debug.logInfo("No UserTransaction, transaction rollback only not set", module);
        }
    }

    public static Transaction suspend() throws GenericTransactionException {
        try {
            if (TransactionUtil.getStatus() != STATUS_NO_TRANSACTION) {
                TransactionManager txMgr = TransactionFactoryLoader.getInstance().getTransactionManager();
                if (txMgr != null) {
                    pushTransactionBeginStackSave(clearTransactionBeginStack());
                    pushSetRollbackOnlyCauseSave(clearSetRollbackOnlyCause());
                    Transaction trans = txMgr.suspend();
                    pushSuspendedTransaction(trans);
                    return trans;
                } else {
                    return null;
                }
            } else {
                Debug.logWarning("No transaction in place, so not suspending.", module);
                return null;
            }
        } catch (SystemException e) {
            throw new GenericTransactionException("System error, could not suspend transaction", e);
        }
    }

    public static void resume(Transaction parentTx) throws GenericTransactionException {
        if (parentTx == null) {
            return;
        }
        TransactionManager txMgr = TransactionFactoryLoader.getInstance().getTransactionManager();
        try {
            if (txMgr != null) {
                setTransactionBeginStack(popTransactionBeginStackSave());
                setSetRollbackOnlyCause(popSetRollbackOnlyCauseSave());
                txMgr.resume(parentTx);
                removeSuspendedTransaction(parentTx);
            }
        } catch (InvalidTransactionException e) {
            throw new GenericTransactionException("System error, could not resume transaction", e);
        } catch (SystemException e) {
            throw new GenericTransactionException("System error, could not resume transaction", e);
        }
    }

    /** Sets the timeout of the transaction in the current thread IF transactions are available */
    public static void setTransactionTimeout(int seconds) throws GenericTransactionException {
        UserTransaction ut = TransactionFactoryLoader.getInstance().getUserTransaction();
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
            TransactionManager tm = TransactionFactoryLoader.getInstance().getTransactionManager();
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
        /*
         * javax.transaction.Status
         * STATUS_ACTIVE           0
         * STATUS_MARKED_ROLLBACK  1
         * STATUS_PREPARED         2
         * STATUS_COMMITTED        3
         * STATUS_ROLLEDBACK       4
         * STATUS_UNKNOWN          5
         * STATUS_NO_TRANSACTION   6
         * STATUS_PREPARING        7
         * STATUS_COMMITTING       8
         * STATUS_ROLLING_BACK     9
         */
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

    private static boolean readDebugResources() {
        try {
            return EntityConfig.getInstance().getDebugXaResources().getValue();
        } catch (GenericEntityConfException gece) {
            Debug.logWarning(gece, module);
        }
        return false;
    }

    public static boolean debugResources() {
        return debugResources;
    }

    public static void logRunningTx() {
        if (debugResources()) {
            if (UtilValidate.isNotEmpty(debugResMap)) {
                for (DebugXaResource dxa: debugResMap.values()) {
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
            TransactionManager tm = TransactionFactoryLoader.getInstance().getTransactionManager();
            if (tm != null && tm.getStatus() == STATUS_ACTIVE) {
                Transaction tx = tm.getTransaction();
                if (tx != null) {
                    tx.registerSynchronization(sync);
                }
            }
        } catch (RollbackException e) {
            throw new GenericTransactionException("Roll Back error, could not register synchronization in transaction even though transactions are available, current transaction rolled back", e);
        } catch (SystemException e) {
            throw new GenericTransactionException("System error, could not register synchronization in transaction even though transactions are available", e);
        }
    }

    // =======================================
    // SUSPENDED TRANSACTIONS
    // =======================================
    /** BE VERY CAREFUL WHERE YOU CALL THIS!! */
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
        List<Transaction> tl = suspendedTxStack.get();
        return UtilValidate.isNotEmpty(tl);
    }

    public static List<Transaction> getSuspendedTxStack() {
        List<Transaction> tl = suspendedTxStack.get();
        if (tl == null) {
            tl = new LinkedList<Transaction>();
            suspendedTxStack.set(tl);
        }
        return tl;
    }

    public static List<Exception> getSuspendedTxLocationsStack() {
        List<Exception> tl = suspendedTxLocationStack.get();
        if (tl == null) {
            tl = new LinkedList<Exception>();
            suspendedTxLocationStack.set(tl);
        }
        return tl;
    }

    protected static void pushSuspendedTransaction(Transaction t) {
        List<Transaction> tl = getSuspendedTxStack();
        tl.add(0, t);
        List<Exception> stls = getSuspendedTxLocationsStack();
        stls.add(0, new Exception("TX Suspend Location"));
        // save the current transaction start stamp
        pushTransactionStartStamp(t);
    }

    protected static Transaction popSuspendedTransaction() {
        List<Transaction> tl = suspendedTxStack.get();
        if (UtilValidate.isNotEmpty(tl)) {
            // restore the transaction start stamp
            popTransactionStartStamp();
            List<Exception> stls = suspendedTxLocationStack.get();
            if (UtilValidate.isNotEmpty(stls)) {
                stls.remove(0);
            }
            return tl.remove(0);
        } else {
            return null;
        }
    }

    protected static void removeSuspendedTransaction(Transaction t) {
        List<Transaction> tl = suspendedTxStack.get();
        if (UtilValidate.isNotEmpty(tl)) {
            tl.remove(t);
            List<Exception> stls = suspendedTxLocationStack.get();
            if (UtilValidate.isNotEmpty(stls)) {
                stls.remove(0);
            }
            popTransactionStartStamp(t);
        }
    }

    // =======================================
    // TRANSACTION BEGIN STACK
    // =======================================
    private static void pushTransactionBeginStackSave(Exception e) {
        // use the ThreadLocal one because it is more reliable than the all threads Map
        List<Exception> el = transactionBeginStackSave.get();
        if (el == null) {
            el = new LinkedList<Exception>();
            transactionBeginStackSave.set(el);
        }
        el.add(0, e);

        if (Debug.infoOn()) {
            Long curThreadId = Thread.currentThread().getId();
            List<Exception> ctEl = allThreadsTransactionBeginStackSave.get(curThreadId);
            if (ctEl == null) {
                ctEl = new LinkedList<Exception>();
                allThreadsTransactionBeginStackSave.put(curThreadId, ctEl);
            }
            ctEl.add(0, e);
        }
    }

    private static Exception popTransactionBeginStackSave() {
        if (Debug.infoOn()) {
            // do the unofficial all threads Map one first, and don't do a real return
            Long curThreadId = Thread.currentThread().getId();
            List<Exception> ctEl = allThreadsTransactionBeginStackSave.get(curThreadId);
            if (UtilValidate.isNotEmpty(ctEl)) {
                ctEl.remove(0);
            }
        }
        // then do the more reliable ThreadLocal one
        List<Exception> el = transactionBeginStackSave.get();
        if (UtilValidate.isNotEmpty(el)) {
            return el.remove(0);
        } else {
            return null;
        }
    }

    public static int getTransactionBeginStackSaveSize() {
        List<Exception> el = transactionBeginStackSave.get();
        if (el != null) {
            return el.size();
        } else {
            return 0;
        }
    }

    public static List<Exception> getTransactionBeginStackSave() {
        List<Exception> el = transactionBeginStackSave.get();
        List<Exception> elClone = new LinkedList<Exception>();
        elClone.addAll(el);
        return elClone;
    }

    public static Map<Long, List<Exception>> getAllThreadsTransactionBeginStackSave() {
        Map<Long, List<Exception>> attbssMap = allThreadsTransactionBeginStackSave;
        Map<Long, List<Exception>> attbssMapClone = new HashMap<Long, List<Exception>>();
        attbssMapClone.putAll(attbssMap);
        return attbssMapClone;
    }

    public static void printAllThreadsTransactionBeginStacks() {
        if (!Debug.infoOn()) {
            return;
        }

        for (Map.Entry<Long, Exception> attbsMapEntry : allThreadsTransactionBeginStack.entrySet()) {
            Long curThreadId = attbsMapEntry.getKey();
            Exception transactionBeginStack = attbsMapEntry.getValue();
            List<Exception> txBeginStackList = allThreadsTransactionBeginStackSave.get(curThreadId);

            Debug.logInfo(transactionBeginStack, "===================================================\n===================================================\n Current tx begin stack for thread [" + curThreadId + "]:", module);

            if (UtilValidate.isNotEmpty(txBeginStackList)) {
                int stackLevel = 0;
                for (Exception stack : txBeginStackList) {
                    Debug.logInfo(stack, "===================================================\n===================================================\n Tx begin stack history for thread [" + curThreadId + "] history number [" + stackLevel + "]:", module);
                    stackLevel++;
                }
            } else {
                Debug.logInfo("========================================== No tx begin stack history found for thread [" + curThreadId + "]", module);
            }
        }
    }

    private static void setTransactionBeginStack() {
        Exception e = new Exception("Tx Stack Placeholder");
        setTransactionBeginStack(e);
    }

    private static void setTransactionBeginStack(Exception newExc) {
        if (transactionBeginStack.get() != null) {
            Exception e = transactionBeginStack.get();
            Debug.logWarning(e, "In setTransactionBeginStack a stack placeholder was already in place, here is where the transaction began: ", module);
            Exception e2 = new Exception("Current Stack Trace");
            Debug.logWarning(e2, "In setTransactionBeginStack a stack placeholder was already in place, here is the current location: ", module);
        }
        transactionBeginStack.set(newExc);
        if (Debug.infoOn()) {
            Long curThreadId = Thread.currentThread().getId();
            allThreadsTransactionBeginStack.put(curThreadId, newExc);
        }
    }

    private static Exception clearTransactionBeginStack() {
        if (Debug.infoOn()) {
            Long curThreadId = Thread.currentThread().getId();
            allThreadsTransactionBeginStack.remove(curThreadId);
        }
        Exception e = transactionBeginStack.get();
        if (e == null) {
            Exception e2 = new Exception("Current Stack Trace");
            Debug.logWarning(e2, "In clearTransactionBeginStack no stack placeholder was in place, here is the current location: ", module);
            return null;
        } else {
            transactionBeginStack.set(null);
            return e;
        }
    }

    public static Exception getTransactionBeginStack() {
        Exception e = transactionBeginStack.get();
        if (e == null) {
            Exception e2 = new Exception("Current Stack Trace");
            Debug.logWarning(e2, "In getTransactionBeginStack no stack placeholder was in place, here is the current location: ", module);
        }
        return e;
    }

    // =======================================
    // ROLLBACK ONLY CAUSE
    // =======================================
    private static class RollbackOnlyCause {
        protected String causeMessage;
        protected Throwable causeThrowable;

        public RollbackOnlyCause(String causeMessage, Throwable causeThrowable) {
            this.causeMessage = causeMessage;
            this.causeThrowable = causeThrowable;
        }
        public String getCauseMessage() {
            return this.causeMessage + (this.causeThrowable == null ? "" : this.causeThrowable.toString());
        }

        public Throwable getCauseThrowable() {
            return this.causeThrowable;
        }

        public void logError(String message) {
            Debug.logError(this.getCauseThrowable(), (message == null ? "" : message) + this.getCauseMessage(), module);
        }

        public boolean isEmpty() {
            return (UtilValidate.isEmpty(this.getCauseMessage()) && this.getCauseThrowable() == null);
        }
    }

    private static void pushSetRollbackOnlyCauseSave(RollbackOnlyCause e) {
        List<RollbackOnlyCause> el = setRollbackOnlyCauseSave.get();
        if (el == null) {
            el = new LinkedList<RollbackOnlyCause>();
            setRollbackOnlyCauseSave.set(el);
        }
        el.add(0, e);
    }

    private static RollbackOnlyCause popSetRollbackOnlyCauseSave() {
        List<RollbackOnlyCause> el = setRollbackOnlyCauseSave.get();
        if (UtilValidate.isNotEmpty(el)) {
            return el.remove(0);
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
            RollbackOnlyCause roc = setRollbackOnlyCause.get();
            roc.logError("In setSetRollbackOnlyCause a stack placeholder was already in place, here is the original rollbackOnly cause: ");
            Exception e2 = new Exception("Current Stack Trace");
            Debug.logWarning(e2, "In setSetRollbackOnlyCause a stack placeholder was already in place, here is the current location: ", module);
        }
        setRollbackOnlyCause.set(newRoc);
    }

    private static RollbackOnlyCause clearSetRollbackOnlyCause() {
        RollbackOnlyCause roc = setRollbackOnlyCause.get();
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
            Debug.logWarning(e, "In getSetRollbackOnlyCause no stack placeholder was in place, here is the current location: ", module);
        }
        return setRollbackOnlyCause.get();
    }

    // =======================================
    // SUSPENDED TRANSACTIONS START TIMESTAMPS
    // =======================================

    /**
     * Maintain the suspended transactions together with their timestamps
     */
    private static ThreadLocal<Map<Transaction, Timestamp>> suspendedTxStartStamps = new ThreadLocal<Map<Transaction, Timestamp>>() {
        @Override
        public Map<Transaction, Timestamp> initialValue() {
            return UtilGenerics.checkMap(new ListOrderedMap());
        }
    };

    /**
    * Put the stamp to remember later
    * @param t transaction just suspended
    */
    private static void pushTransactionStartStamp(Transaction t) {
        Map<Transaction, Timestamp> map = suspendedTxStartStamps.get();
        Timestamp stamp = transactionStartStamp.get();
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
        suspendedTxStartStamps.get().clear();
    }

    /**
    * Remove the stamp of the specified transaction from stack (when resuming)
    * and set it as current start stamp.
    * @param t transaction just resumed
    */
    private static void popTransactionStartStamp(Transaction t) {
        Map<Transaction, Timestamp> map = suspendedTxStartStamps.get();
        if (map.size() > 0) {
            Timestamp stamp = map.remove(t);
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
            transactionStartStamp.set((Timestamp) map.remove(map.lastKey()));
        } else {
            Debug.logError("Error in transaction handling - no saved start stamp found - using NOW.", module);
            transactionStartStamp.set(UtilDateTime.nowTimestamp());
        }
    }

    public static Timestamp getTransactionStartStamp() {
        Timestamp curStamp = transactionStartStamp.get();
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
        Timestamp lastNowStamp = transactionLastNowStamp.get();
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

    public static final class NoTransaction<V> implements Callable<V> {
        private final Callable<V> callable;

        protected NoTransaction(Callable<V> callable) {
            this.callable = callable;
        }

        public V call() throws GenericEntityException {
            Transaction suspended = TransactionUtil.suspend();
            try {
                try {
                    return callable.call();
                } catch (Throwable t) {
                    while (t.getCause() != null) {
                        t = t.getCause();
                    }
                    throw t;
                }
            } catch (GenericEntityException e) {
                throw e;
            } catch (Error e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new GenericEntityException(t);
            } finally {
                TransactionUtil.resume(suspended);
            }
        }
    }

    public static final class InTransaction<V> implements Callable<V> {
        private final Callable<V> callable;
        private final String ifErrorMessage;
        private final int timeout;
        private final boolean printException;

        protected InTransaction(Callable<V> callable, String ifErrorMessage, int timeout, boolean printException) {
            this.callable = callable;
            this.ifErrorMessage = ifErrorMessage;
            this.timeout = timeout;
            this.printException = printException;
        }

        public V call() throws GenericEntityException {
            boolean tx = TransactionUtil.begin(timeout);
            Throwable transactionAbortCause = null;
            try {
                try {
                    return callable.call();
                } catch (Throwable t) {
                    while (t.getCause() != null) {
                        t = t.getCause();
                    }
                    throw t;
                }
            } catch (Error e) {
                transactionAbortCause = e;
                throw e;
            } catch (RuntimeException e) {
                transactionAbortCause = e;
                throw e;
            } catch (Throwable t) {
                transactionAbortCause = t;
                throw new GenericEntityException(t);
            } finally {
                if (transactionAbortCause == null) {
                    TransactionUtil.commit(tx);
                } else {
                    if (printException) {
                        transactionAbortCause.printStackTrace();
                    }
                    TransactionUtil.rollback(tx, ifErrorMessage, transactionAbortCause);
                }
            }
        }
    }

    public static Connection getCursorConnection(GenericHelperInfo helperInfo, Connection con) {
        Datasource datasourceInfo = EntityConfig.getDatasource(helperInfo.getHelperBaseName());
        if (datasourceInfo == null) {
            Debug.logWarning("Could not find configuration for " + helperInfo.getHelperBaseName() + " datasource.", module);
            return con;
        } else if (datasourceInfo.getUseProxyCursor()) {
            try {
                if (datasourceInfo.getResultFetchSize() > 1)
                    con = CursorConnection.newCursorConnection(con, datasourceInfo.getProxyCursorName(), datasourceInfo.getResultFetchSize());
            } catch (Exception ex) {
                Debug.logWarning(ex, "Error creating the cursor connection proxy " + helperInfo.getHelperBaseName() + " datasource.", module);
            }
        }
        return con;
    }

}
