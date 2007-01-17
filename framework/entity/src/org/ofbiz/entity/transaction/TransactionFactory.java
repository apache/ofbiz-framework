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
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.jdbc.CursorConnection;

/**
 * TransactionFactory - central source for JTA objects
 */
public class TransactionFactory {

    public static final String module = TransactionFactory.class.getName();
    public static TransactionFactoryInterface transactionFactory = null;

    public static TransactionFactoryInterface getTransactionFactory() {
        if (transactionFactory == null) { // don't want to block here
            synchronized (TransactionFactory.class) {
                // must check if null again as one of the blocked threads can still enter
                if (transactionFactory == null) {
                    try {
                        String className = EntityConfigUtil.getTxFactoryClass();

                        if (className == null) {
                            throw new IllegalStateException("Could not find transaction factory class name definition");
                        }
                        Class tfClass = null;

                        if (className != null && className.length() > 0) {
                            try {
                                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                                tfClass = loader.loadClass(className);
                            } catch (ClassNotFoundException e) {
                                Debug.logWarning(e, module);
                                throw new IllegalStateException("Error loading TransactionFactory class \"" + className + "\": " + e.getMessage());
                            }
                        }

                        try {
                            transactionFactory = (TransactionFactoryInterface) tfClass.newInstance();
                        } catch (IllegalAccessException e) {
                            Debug.logWarning(e, module);
                            throw new IllegalStateException("Error loading TransactionFactory class \"" + className + "\": " + e.getMessage());
                        } catch (InstantiationException e) {
                            Debug.logWarning(e, module);
                            throw new IllegalStateException("Error loading TransactionFactory class \"" + className + "\": " + e.getMessage());
                        }
                    } catch (SecurityException e) {
                        Debug.logError(e, module);
                        throw new IllegalStateException("Error loading TransactionFactory class: " + e.getMessage());
                    }
                }
            }
        }
        return transactionFactory;
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

    public static Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        return getTransactionFactory().getConnection(helperName);
    }

    public static void shutdown() {
        getTransactionFactory().shutdown();
    }

    public static Connection getCursorConnection(String helperName, Connection con) {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);
        if (datasourceInfo == null) {
            Debug.logWarning("Could not find configuration for " + helperName + " datasource.", module);
            return con;
        } else if (datasourceInfo.useProxyCursor) {
            try {
                if (datasourceInfo.resultFetchSize > 1)
                    con = CursorConnection.newCursorConnection(con, datasourceInfo.cursorName, datasourceInfo.resultFetchSize);
            } catch (Exception ex) {
                Debug.logWarning(ex, "Error creating the cursor connection proxy " + helperName + " datasource.", module);
            }
        }
        return con;
    }
}
