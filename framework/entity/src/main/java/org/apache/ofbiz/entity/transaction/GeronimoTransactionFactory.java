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

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;

import org.apache.geronimo.transaction.log.UnrecoverableLog;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.apache.geronimo.transaction.manager.TransactionLog;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.jdbc.ConnectionFactoryLoader;

public class GeronimoTransactionFactory implements TransactionFactory {

    public static final String module = GeronimoTransactionFactory.class.getName();

    private static int defaultTransactionTimeoutSeconds = 60;
    private static TransactionLog transactionLog;
    private static GeronimoTransactionManager geronimoTransactionManager;

    static {
        // creates an instance of Geronimo transaction context, etc with a local transaction factory which is not bound to a registry
        try {
            transactionLog = new UnrecoverableLog();
            geronimoTransactionManager = new GeronimoTransactionManager(defaultTransactionTimeoutSeconds, new XidFactoryImpl(), transactionLog);
        } catch (XAException e) {
            Debug.logError(e, "Error initializing Geronimo transaction manager: " + e.toString(), module);
        }
    }

    /*
     * @see org.apache.ofbiz.entity.transaction.TransactionFactory#getTransactionManager()
     */
    public TransactionManager getTransactionManager() {
        return geronimoTransactionManager;
    }

    /*
     * @see org.apache.ofbiz.entity.transaction.TransactionFactory#getUserTransaction()
     */
    public UserTransaction getUserTransaction() {
        return geronimoTransactionManager;
    }

    public String getTxMgrName() {
        return "geronimo";
    }

    public Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException {
        Datasource datasourceInfo = EntityConfig.getDatasource(helperInfo.getHelperBaseName());

        if (datasourceInfo != null && datasourceInfo.getInlineJdbc() != null) {
            return ConnectionFactoryLoader.getInstance().getConnection(helperInfo, datasourceInfo.getInlineJdbc());
        } else {
            Debug.logError("Geronimo is the configured transaction manager but no inline-jdbc element was specified in the " + helperInfo.getHelperBaseName() + " datasource. Please check your configuration", module);
            return null;
        }
    }

    public void shutdown() {
        ConnectionFactoryLoader.getInstance().closeAll();
        /*
        if (transactionContextManager != null) {
            // TODO: need to do anything for this?
            transactionContextManager = null;
        }
        */
    }
}
