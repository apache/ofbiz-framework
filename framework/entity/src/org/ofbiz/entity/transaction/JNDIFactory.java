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
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import javolution.util.FastMap;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.JNDIContextFactory;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.w3c.dom.Element;

/**
 * Central source for Tyrex JTA objects from JNDI
 */
public class JNDIFactory implements TransactionFactoryInterface {

    // Debug module name
    public static final String module = JNDIFactory.class.getName();

    static TransactionManager transactionManager = null;
    static UserTransaction userTransaction = null;

    // protected static UtilCache dsCache = new UtilCache("entity.JndiDataSources", 0, 0);
    protected static Map<String, DataSource> dsCache = FastMap.newInstance();

    public TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            synchronized (JNDIFactory.class) {
                // try again inside the synch just in case someone when through while we were waiting
                if (transactionManager == null) {
                    try {
                        String jndiName = EntityConfigUtil.getTxFactoryTxMgrJndiName();
                        String jndiServerName = EntityConfigUtil.getTxFactoryTxMgrJndiServerName();

                        if (UtilValidate.isNotEmpty(jndiName)) {
                            // if (Debug.verboseOn()) Debug.logVerbose("[JNDIFactory.getTransactionManager] Trying JNDI name " + jndiName, module);

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
                                Debug.logWarning("[JNDIFactory.getTransactionManager] Failed to find TransactionManager named " + jndiName + " in JNDI.", module);
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
            synchronized (JNDIFactory.class) {
                // try again inside the synch just in case someone when through while we were waiting
                if (userTransaction == null) {
                    try {
                        String jndiName = EntityConfigUtil.getTxFactoryUserTxJndiName();
                        String jndiServerName = EntityConfigUtil.getTxFactoryUserTxJndiServerName();

                        if (UtilValidate.isNotEmpty(jndiName)) {
                            // if (Debug.verboseOn()) Debug.logVerbose("[JNDIFactory.getTransactionManager] Trying JNDI name " + jndiName, module);

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
                                Debug.logWarning("[JNDIFactory.getUserTransaction] Failed to find UserTransaction named " + jndiName + " in JNDI.", module);
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
        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperInfo.getHelperBaseName());

        if (datasourceInfo.jndiJdbcElement != null) {
            Element jndiJdbcElement = datasourceInfo.jndiJdbcElement;
            String jndiName = jndiJdbcElement.getAttribute("jndi-name");
            String jndiServerName = jndiJdbcElement.getAttribute("jndi-server-name");
            Connection con = getJndiConnection(jndiName, jndiServerName);
            if (con != null) return TransactionFactory.getCursorConnection(helperInfo, con);
        } else {
           // Debug.logError("JNDI loaded is the configured transaction manager but no jndi-jdbc element was specified in the " + helperName + " datasource. Please check your configuration.", module);
        }

        if (datasourceInfo.inlineJdbcElement != null) {
            Connection otherCon = ConnectionFactory.getManagedConnection(helperInfo, datasourceInfo.inlineJdbcElement);
            return TransactionFactory.getCursorConnection(helperInfo, otherCon);
        } else {
            //no real need to print an error here
            return null;
        }
    }

    public static Connection getJndiConnection(String jndiName, String jndiServerName) throws SQLException, GenericEntityException {
        // if (Debug.verboseOn()) Debug.logVerbose("Trying JNDI name " + jndiName, module);
        DataSource ds;

        ds = dsCache.get(jndiName);
        if (ds != null) {
            if (ds instanceof XADataSource) {
                XADataSource xads = (XADataSource) ds;

                return TransactionUtil.enlistConnection(xads.getXAConnection());
            } else {
                return ds.getConnection();
            }
        }

        synchronized (ConnectionFactory.class) {
            // try again inside the synch just in case someone when through while we were waiting
            ds = dsCache.get(jndiName);
            if (ds != null) {
                if (ds instanceof XADataSource) {
                    XADataSource xads = (XADataSource) ds;

                    return TransactionUtil.enlistConnection(xads.getXAConnection());
                } else {
                    return ds.getConnection();
                }
            }

            try {
                if (Debug.infoOn()) Debug.logInfo("Doing JNDI lookup for name " + jndiName, module);
                InitialContext ic = JNDIContextFactory.getInitialContext(jndiServerName);

                if (ic != null) {
                    ds = (DataSource) ic.lookup(jndiName);
                } else {
                    Debug.logWarning("Initial Context returned was NULL for server name " + jndiServerName, module);
                }

                if (ds != null) {
                    if (Debug.verboseOn()) Debug.logVerbose("Got a Datasource object.", module);
                    dsCache.put(jndiName, ds);
                    Connection con;

                    if (ds instanceof XADataSource) {
                        if (Debug.infoOn()) Debug.logInfo("Got XADataSource for name " + jndiName, module);
                        XADataSource xads = (XADataSource) ds;
                        XAConnection xac = xads.getXAConnection();

                        con = TransactionUtil.enlistConnection(xac);
                    } else {
                        if (Debug.infoOn()) Debug.logInfo("Got DataSource for name " + jndiName, module);

                        con = ds.getConnection();
                    }

                    /* NOTE: This code causes problems because settting the transaction isolation level after a transaction has started is a no-no
                     * The question is: how should we do this?
                     String isolationLevel = jndiJdbcElement.getAttribute("isolation-level");
                     if (UtilValidate.isNotEmpty(isolationLevel)) {
                     if ("Serializable".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                     } else if ("RepeatableRead".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                     } else if ("ReadUncommitted".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                     } else if ("ReadCommitted".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                     } else if ("None".equals(isolationLevel)) {
                     con.setTransactionIsolation(Connection.TRANSACTION_NONE);
                     }
                     }
                     */

                    // if (con != null) if (Debug.infoOn()) Debug.logInfo("[ConnectionFactory.getConnection] Got JNDI connection with catalog: " + con.getCatalog(), module);
                    return con;
                } else {
                    Debug.logError("Datasource returned was NULL.", module);
                }
            } catch (NamingException ne) {
                Debug.logWarning(ne, "[ConnectionFactory.getConnection] Failed to find DataSource named " + jndiName + " in JNDI server with name " + jndiServerName + ". Trying normal database.", module);
            } catch (GenericConfigException gce) {
                throw new GenericEntityException("Problems with the JNDI configuration.", gce.getNested());
            }
        }
        return null;
    }

    public void shutdown() {}
}
