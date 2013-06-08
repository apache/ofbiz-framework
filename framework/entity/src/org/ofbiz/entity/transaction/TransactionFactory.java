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
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.config.model.Datasource;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.CursorConnection;

/**
 * TransactionFactory - central source for JTA objects
 */
public class TransactionFactory {

    public static final String module = TransactionFactory.class.getName();
    private static final AtomicReference<TransactionFactoryInterface> txFactoryRef = new AtomicReference<TransactionFactoryInterface>(null);

    private static TransactionFactoryInterface createTransactionFactoryInterface() throws Exception {
        String className = EntityConfigUtil.getTxFactoryClass();
        if (className == null) {
            throw new IllegalStateException("Could not find transaction factory class name definition");
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> tfClass = loader.loadClass(className);
        return (TransactionFactoryInterface) tfClass.newInstance();
    }

    private static TransactionFactoryInterface getTransactionFactory() {
        TransactionFactoryInterface instance = txFactoryRef.get();
        if (instance == null) {
            try {
                instance = createTransactionFactoryInterface();
                if (!txFactoryRef.compareAndSet(null, instance)) {
                    instance = txFactoryRef.get();
                }
            } catch (Exception e) {
                Debug.logError(e, "Exception thrown while creating TransactionFactoryInterface instance: ", module);
                throw new IllegalStateException("Error loading TransactionFactory class: " + e);
            }
        }
        return instance;
    }

    public static TransactionManager getTransactionManager() {
        return getTransactionFactory().getTransactionManager();
    }

    public static UserTransaction getUserTransaction() {
        return getTransactionFactory().getUserTransaction();
    }

    public static String getTxMgrName() {
        return getTransactionFactory().getTxMgrName();
    }

    public static Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException {
        return getTransactionFactory().getConnection(helperInfo);
    }

    public static void shutdown() {
        getTransactionFactory().shutdown();
    }

    public static Connection getCursorConnection(GenericHelperInfo helperInfo, Connection con) {
        Datasource datasourceInfo = EntityConfigUtil.getDatasource(helperInfo.getHelperBaseName());
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
