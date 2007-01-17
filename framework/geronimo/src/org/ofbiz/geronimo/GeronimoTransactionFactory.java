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
import java.util.Collection;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;

import org.apache.geronimo.transaction.ExtendedTransactionManager;
import org.apache.geronimo.transaction.context.GeronimoTransactionManager;
import org.apache.geronimo.transaction.context.TransactionContextManager;
import org.apache.geronimo.transaction.log.UnrecoverableLog;
import org.apache.geronimo.transaction.manager.TransactionLog;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.geronimo.transaction.manager.XidImporter;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.transaction.MinervaConnectionFactory;
import org.ofbiz.entity.transaction.TransactionFactoryInterface;

/**
 * GeronimoTransactionFactory
 */
public class GeronimoTransactionFactory implements TransactionFactoryInterface {

    public static final String module = GeronimoTransactionFactory.class.getName();        
    
    // just use the transactionManager for this private static XidImporter xidImporter;
    private static ExtendedTransactionManager transactionManager;
    private static TransactionContextManager transactionContextManager;
    private static int defaultTransactionTimeoutSeconds = 60;
    private static TransactionLog transactionLog;
    private static Collection resourceManagers = null;
    private static GeronimoTransactionManager geronimoTransactionManager;

    static {    
        // creates an instance of Geronimo transaction context, etc with a local transaction factory which is not bound to a registry
        try {
            transactionLog = new UnrecoverableLog();
            transactionManager = new TransactionManagerImpl(defaultTransactionTimeoutSeconds, transactionLog, resourceManagers);
            transactionContextManager = new TransactionContextManager(transactionManager, (XidImporter) transactionManager);
            geronimoTransactionManager = new GeronimoTransactionManager(transactionContextManager);
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
    
    public Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);

        if (datasourceInfo != null && datasourceInfo.inlineJdbcElement != null) {
            try {
                Connection con = MinervaConnectionFactory.getConnection(helperName, datasourceInfo.inlineJdbcElement);
                if (con != null) return con;
            } catch (Exception ex) {
                Debug.logError(ex, "Geronimo is the configured transaction manager but there was an error getting a database Connection through Geronimo for the " + helperName + " datasource. Please check your configuration, class path, etc.", module);
            }
        
            Connection otherCon = ConnectionFactory.tryGenericConnectionSources(helperName, datasourceInfo.inlineJdbcElement);
            return otherCon;
        } else {            
            Debug.logError("Geronimo is the configured transaction manager but no inline-jdbc element was specified in the " + helperName + " datasource. Please check your configuration", module);
            return null;
        }
    }
    
    public void shutdown() {
        MinervaConnectionFactory.closeAll();
        if (transactionContextManager != null) {
            // TODO: need to do anything for this?
            transactionContextManager = null;
        }           
    }
}
