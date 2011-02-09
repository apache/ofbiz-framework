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
package org.ofbiz.geronimo;

import java.sql.Connection;
import java.sql.SQLException;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;

import org.apache.geronimo.transaction.log.UnrecoverableLog;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.apache.geronimo.transaction.manager.TransactionLog;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.transaction.TransactionFactoryInterface;

/**
 * GeronimoTransactionFactory
 */
public class GeronimoTransactionFactory implements TransactionFactoryInterface {

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
     * @see org.ofbiz.entity.transaction.TransactionFactoryInterface#getTransactionManager()
     */
    public TransactionManager getTransactionManager() {
        return geronimoTransactionManager;
    }

    /*
     * @see org.ofbiz.entity.transaction.TransactionFactoryInterface#getUserTransaction()
     */
    public UserTransaction getUserTransaction() {
        return geronimoTransactionManager;
    }

    public String getTxMgrName() {
        return "geronimo";
    }

    public Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperInfo.getHelperBaseName());

        if (datasourceInfo != null && datasourceInfo.inlineJdbcElement != null) {
            return ConnectionFactory.getManagedConnection(helperInfo, datasourceInfo.inlineJdbcElement);
        } else {
            Debug.logError("Geronimo is the configured transaction manager but no inline-jdbc element was specified in the " + helperInfo.getHelperBaseName() + " datasource. Please check your configuration", module);
            return null;
        }
    }

    public void shutdown() {
        ConnectionFactory.closeAllManagedConnections();
        /*
        if (transactionContextManager != null) {
            // TODO: need to do anything for this?
            transactionContextManager = null;
        }
        */
    }
}
