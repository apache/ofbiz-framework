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
package org.ofbiz.entity.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.enhydra.jdbc.pool.StandardXAPoolDataSource;
import org.enhydra.jdbc.standard.StandardXADataSource;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.transaction.TransactionFactory;
import org.w3c.dom.Element;

/**
 * JotmFactory - Central source for JOTM JDBC Objects
 */
public class XaPoolConnectionFactory {

    public static final String module = XaPoolConnectionFactory.class.getName();

    protected static Map dsCache = new HashMap();

    public static Connection getConnection(String helperName, Element jdbcElement) throws SQLException, GenericEntityException {
        StandardXAPoolDataSource pds = (StandardXAPoolDataSource) dsCache.get(helperName);
        if (pds != null) {
            if (Debug.verboseOn()) Debug.logVerbose(helperName + " pool size: " + pds.pool.getCount(), module);
            return TransactionFactory.getCursorConnection(helperName, pds.getConnection());
        }

        synchronized (XaPoolConnectionFactory.class) {
            pds = (StandardXAPoolDataSource) dsCache.get(helperName);
            if (pds != null) {
                return pds.getConnection();
            }

            // the xapool wrapper class
            String wrapperClass = jdbcElement.getAttribute("pool-xa-wrapper-class");

            StandardXADataSource ds = null;
            try {
                //ds =  new StandardXADataSource();
                ds = (StandardXADataSource) ObjectType.getInstance(wrapperClass);
                pds = new StandardXAPoolDataSource();
            } catch (NoClassDefFoundError e) {
                throw new GenericEntityException("Cannot find xapool.jar");
            } catch (ClassNotFoundException e) {
                throw new GenericEntityException("Cannot load wrapper class: " + wrapperClass, e);
            } catch (InstantiationException e) {
                throw new GenericEntityException("Unable to instantiate " + wrapperClass, e);
            } catch (IllegalAccessException e) {
                throw new GenericEntityException("Problems getting instance of " + wrapperClass, e);
            }

            if (ds == null)
                throw new GenericEntityException("StandardXaDataSource was not created, big problem!");

            ds.setDriverName(jdbcElement.getAttribute("jdbc-driver"));
            ds.setUrl(jdbcElement.getAttribute("jdbc-uri"));
            ds.setUser(jdbcElement.getAttribute("jdbc-username"));
            ds.setPassword(jdbcElement.getAttribute("jdbc-password"));
            ds.setDescription(helperName);
            ds.setTransactionManager(TransactionFactory.getTransactionManager());

            String transIso = jdbcElement.getAttribute("isolation-level");
            if (UtilValidate.isNotEmpty(transIso)) {
                if ("Serializable".equals(transIso)) {
                    ((StandardXADataSource) ds).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                } else if ("RepeatableRead".equals(transIso)) {
                    ((StandardXADataSource) ds).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                } else if ("ReadUncommitted".equals(transIso)) {
                    ((StandardXADataSource) ds).setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                } else if ("ReadCommitted".equals(transIso)) {
                    ((StandardXADataSource) ds).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                } else if ("None".equals(transIso)) {
                    ((StandardXADataSource) ds).setTransactionIsolation(Connection.TRANSACTION_NONE);
                }
            }

            // set the datasource in the pool
            pds.setDataSource(ds);
            pds.setDescription(ds.getDescription());
            pds.setUser(ds.getUser());
            pds.setPassword(ds.getPassword());
            Debug.logInfo("XADataSource: " + ds.getClass().getName() + " attached to pool.", module);

            // set the transaction manager in the pool
            pds.setTransactionManager(TransactionFactory.getTransactionManager());

            // configure the pool settings
            try {
                pds.setMaxSize(Integer.parseInt(jdbcElement.getAttribute("pool-maxsize")));
                pds.setMinSize(Integer.parseInt(jdbcElement.getAttribute("pool-minsize")));
                pds.setSleepTime(Long.parseLong(jdbcElement.getAttribute("pool-sleeptime")));
                pds.setLifeTime(Long.parseLong(jdbcElement.getAttribute("pool-lifetime")));
                pds.setDeadLockMaxWait(Long.parseLong(jdbcElement.getAttribute("pool-deadlock-maxwait")));
                pds.setDeadLockRetryWait(Long.parseLong(jdbcElement.getAttribute("pool-deadlock-retrywait")));

                // set the test statement to test connections
                String testStmt = jdbcElement.getAttribute("pool-jdbc-test-stmt");
                if (UtilValidate.isNotEmpty(testStmt)) {
                    pds.setJdbcTestStmt(testStmt);
                    Debug.logInfo("Set JDBC Test Statement : " + testStmt, module);
                }
            } catch (NumberFormatException nfe) {
                Debug.logError(nfe, "Problems with pool settings; the values MUST be numbers, using defaults.", module);
            } catch (Exception e) {
                Debug.logError(e, "Problems with pool settings", module);
            }

            // cache the pool
            dsCache.put(helperName, pds);

            return TransactionFactory.getCursorConnection(helperName, pds.getConnection());
        }
    }

    public static void closeAll() {
        Set cacheKeys = dsCache.keySet();
        Iterator i = cacheKeys.iterator();
        while (i.hasNext()) {
            String helperName = (String) i.next();
            StandardXAPoolDataSource pds = (StandardXAPoolDataSource) dsCache.remove(helperName);
            pds.shutdown(true);
        }
    }
}
