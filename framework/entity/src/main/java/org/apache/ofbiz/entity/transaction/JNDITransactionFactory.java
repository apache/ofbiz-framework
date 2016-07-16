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
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.JNDIContextFactory;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.config.model.*;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.jdbc.ConnectionFactoryLoader;

/**
 * Central source for Tyrex JTA objects from JNDI
 */
public class JNDITransactionFactory implements TransactionFactory {

    // Debug module name
    public static final String module = JNDITransactionFactory.class.getName();

    static TransactionManager transactionManager = null;
    static UserTransaction userTransaction = null;

    // protected static UtilCache dsCache = new UtilCache("entity.JndiDataSources", 0, 0);
    protected static ConcurrentHashMap<String, DataSource> dsCache = new ConcurrentHashMap<String, DataSource>();

    public TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            synchronized (JNDITransactionFactory.class) {
                // try again inside the synch just in case someone when through while we were waiting
                if (transactionManager == null) {
                    try {
                        String jndiName = EntityConfig.getInstance().getTransactionFactory().getTransactionManagerJndi().getJndiName();
                        String jndiServerName = EntityConfig.getInstance().getTransactionFactory().getTransactionManagerJndi().getJndiServerName();

                        if (UtilValidate.isNotEmpty(jndiName)) {
                            // if (Debug.verboseOn()) Debug.logVerbose("[JNDITransactionFactory.getTransactionManager] Trying JNDI name " + jndiName, module);

                            try {
                                InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

                                if (ic != null) {
                                    transactionManager = (TransactionManager) ic.lookup(jndiName);
                                }
                            } catch (NamingException ne) {
                                Debug.logWarning(ne, "NamingException while finding TransactionManager named " + jndiName + " in JNDI.", module);
                                transactionManager = null;
                            }
                            if (transactionManager == null) {
                                Debug.logWarning("Failed to find TransactionManager named " + jndiName + " in JNDI.", module);
                            }
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e, module);
                        transactionManager = null;
                    }
                }
            }
        }
        return transactionManager;
    }

    public UserTransaction getUserTransaction() {
        if (userTransaction == null) {
            synchronized (JNDITransactionFactory.class) {
                // try again inside the synch just in case someone when through while we were waiting
                if (userTransaction == null) {
                    try {
                        String jndiName = EntityConfig.getInstance().getTransactionFactory().getUserTransactionJndi().getJndiName();
                        String jndiServerName = EntityConfig.getInstance().getTransactionFactory().getUserTransactionJndi().getJndiServerName();

                        if (UtilValidate.isNotEmpty(jndiName)) {

                            try {
                                InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

                                if (ic != null) {
                                    userTransaction = (UserTransaction) ic.lookup(jndiName);
                                }
                            } catch (NamingException ne) {
                                Debug.logWarning(ne, "NamingException while finding UserTransaction named " + jndiName + " in JNDI.", module);
                                userTransaction = null;
                            }
                            if (userTransaction == null) {
                                Debug.logWarning("Failed to find UserTransaction named " + jndiName + " in JNDI.", module);
                            }
                        }
                    } catch (GeneralException e) {
                        Debug.logError(e, module);
                        transactionManager = null;
                    }
                }
            }
        }
        return userTransaction;
    }

    public String getTxMgrName() {
        return "jndi";
    }

    public Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException {
        Datasource datasourceInfo = EntityConfig.getDatasource(helperInfo.getHelperBaseName());

        if (datasourceInfo.getJndiJdbc() != null) {
            JndiJdbc jndiJdbcElement = datasourceInfo.getJndiJdbc();
            String jndiName = jndiJdbcElement.getJndiName();
            String jndiServerName = jndiJdbcElement.getJndiServerName();
            Connection con = getJndiConnection(jndiName, jndiServerName);
            if (con != null) return TransactionUtil.getCursorConnection(helperInfo, con);
        } else {
           // Debug.logError("JNDI loaded is the configured transaction manager but no jndi-jdbc element was specified in the " + helperName + " datasource. Please check your configuration.", module);
        }

        if (datasourceInfo.getInlineJdbc() != null) {
            Connection otherCon = ConnectionFactoryLoader.getInstance().getConnection(helperInfo, datasourceInfo.getInlineJdbc());
            return TransactionUtil.getCursorConnection(helperInfo, otherCon);
        } else {
            //no real need to print an error here
            return null;
        }
    }

    public static Connection getJndiConnection(String jndiName, String jndiServerName) throws SQLException, GenericEntityException {
        // if (Debug.verboseOn()) Debug.logVerbose("Trying JNDI name " + jndiName, module);
        DataSource ds = dsCache.get(jndiName);
        if (ds != null) {
            if (ds instanceof XADataSource) {
                XADataSource xads = (XADataSource) ds;

                return TransactionUtil.enlistConnection(xads.getXAConnection());
            } else {
                return ds.getConnection();
            }
        }
        try {
            if (Debug.infoOn())
                Debug.logInfo("Doing JNDI lookup for name " + jndiName, module);
            InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

            if (ic != null) {
                ds = (DataSource) ic.lookup(jndiName);
            } else {
                Debug.logWarning("Initial Context returned was NULL for server name " + jndiServerName, module);
            }

            if (ds != null) {
                if (Debug.verboseOn())
                    Debug.logVerbose("Got a Datasource object.", module);
                dsCache.putIfAbsent(jndiName, ds);
                ds = dsCache.get(jndiName);
                Connection con;

                if (ds instanceof XADataSource) {
                    if (Debug.infoOn())
                        Debug.logInfo("Got XADataSource for name " + jndiName, module);
                    XADataSource xads = (XADataSource) ds;
                    XAConnection xac = xads.getXAConnection();

                    con = TransactionUtil.enlistConnection(xac);
                } else {
                    if (Debug.infoOn())
                        Debug.logInfo("Got DataSource for name " + jndiName, module);

                    con = ds.getConnection();
                }
                return con;
            } else {
                Debug.logError("Datasource returned was NULL.", module);
            }
        } catch (NamingException ne) {
            Debug.logWarning(ne, "Failed to find DataSource named " + jndiName + " in JNDI server with name " + jndiServerName + ". Trying normal database.", module);
        } catch (GenericConfigException gce) {
            throw new GenericEntityException("Problems with the JNDI configuration.", gce.getNested());
        }
        return null;
    }

    public void shutdown() {}
}
