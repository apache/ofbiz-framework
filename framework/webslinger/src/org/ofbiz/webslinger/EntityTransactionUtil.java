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
package org.ofbiz.webslinger;

import javax.transaction.Transaction;

import org.ofbiz.entity.transaction.TransactionUtil;
import java.util.concurrent.Callable;

public class EntityTransactionUtil {
    public static <V> V doNewTransaction(String ifErrorMessage, Callable<V> callable) throws Throwable {
        Transaction tx = TransactionUtil.suspend();
        try {
            return doTransaction(ifErrorMessage, callable);
        } finally {
            TransactionUtil.resume(tx);
        }
    }

    public static <V> V doTransaction(String ifErrorMessage, Callable<V> callable) throws Throwable {
        boolean tx = TransactionUtil.begin();
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
            throw t;
        } finally {
            if (transactionAbortCause == null) {
                TransactionUtil.commit(tx);
            } else {
                transactionAbortCause.printStackTrace();
                TransactionUtil.rollback(tx, ifErrorMessage, transactionAbortCause);
            }
        }
    }
}
