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
package org.ofbiz.shark.transaction;

import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

import org.enhydra.shark.api.ApplicationMappingTransaction;
import org.enhydra.shark.api.ParticipantMappingTransaction;
import org.enhydra.shark.api.TransactionException;
import org.enhydra.shark.api.UserTransaction;
import org.enhydra.shark.api.RepositoryTransaction;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.internal.transaction.SharkInternalTransaction;
import org.enhydra.shark.api.internal.working.WfProcessInternal;
import org.enhydra.shark.api.internal.working.WfResourceInternal;

/**
 * Shark JTA Transaction Implementation
 */
public class JtaTransaction implements SharkInternalTransaction, UserTransaction,
        ApplicationMappingTransaction, ParticipantMappingTransaction, RepositoryTransaction {

    public static final String module = JtaTransaction.class.getName();
    public static final int transactionTimeout = 120;

    protected Map resourceMap = new HashMap();
    protected Map processMap = new HashMap();

    protected boolean beganTransaction = false;
    protected boolean active = false;
    protected boolean enabled = true;

    public JtaTransaction() {
        if (enabled) {
            try {
                this.beganTransaction = TransactionUtil.begin(transactionTimeout);
                active = true;
            } catch (GenericTransactionException e) {
                Debug.logError(e, module);
            }
        }
    }

    public void commit() throws TransactionException {
        if (active) {
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException e) {
                Debug.logError(e, module);
                throw new TransactionException(e);
            }
            active = false;
        } else {
            Debug.logError(new Exception(), "No active transaction; unable to commit", module);
            //throw new TransactionException("No active transaction");
        }
    }

    public void rollback() throws TransactionException {
        if (active) {
            try {
                TransactionUtil.rollback(beganTransaction, "Transaction rollback from Shark", null);
            } catch (GenericTransactionException e) {
                Debug.logError(e, module);
                throw new TransactionException(e);
            }
            active = false;
        } else {
            Debug.logError(new Exception(), "No active transaction; unable to rollback", module);
            //throw new TransactionException("No active transaction");
        }
    }

    public void release() throws TransactionException {
        if (active) {
            this.commit();
        }        
    }

    public void addToTransaction(String procId, WfProcessInternal proc) throws RootException {
        this.processMap.put(procId, proc);
    }

    public void addToTransaction(String resId, WfResourceInternal res) throws RootException {
        this.resourceMap.put(resId, res);
    }

    public void removeProcess(String procId) throws RootException {
        this.processMap.remove(procId);
    }

    public void removeResource(String resId) throws RootException {
        this.resourceMap.remove(resId);
    }

    public WfProcessInternal getProcess(String procId) throws RootException {
        return (WfProcessInternal) this.processMap.get(procId);
    }

    public WfResourceInternal getResource(String resId) throws RootException {
        return (WfResourceInternal) this.resourceMap.get(resId);
    }
}
