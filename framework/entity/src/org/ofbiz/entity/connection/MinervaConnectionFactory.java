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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.transaction.TransactionFactory;
import org.ofbiz.minerva.pool.jdbc.xa.XAPoolDataSource;
import org.ofbiz.minerva.pool.jdbc.xa.wrapper.XADataSourceImpl;
import org.w3c.dom.Element;

/**
 * MinervaConnectionFactory - Central source for Minerva JDBC Objects
 */
public class MinervaConnectionFactory implements ConnectionFactoryInterface {

    public static final String module = MinervaConnectionFactory.class.getName();
    protected static Map<String, XAPoolDataSource> dsCache = FastMap.newInstance();

    public Connection getConnection(GenericHelperInfo helperInfo, Element jdbcElement) throws SQLException, GenericEntityException {
        XAPoolDataSource pds = dsCache.get(helperInfo.getHelperFullName());
        if (pds != null) {
            return TransactionFactory.getCursorConnection(helperInfo, pds.getConnection());
        }

        synchronized (MinervaConnectionFactory.class) {
            pds = dsCache.get(helperInfo.getHelperFullName());
            if (pds != null) {
                return pds.getConnection();
            } else {
                pds = new XAPoolDataSource();
                pds.setPoolName(helperInfo.getHelperFullName());
            }

            XADataSourceImpl ds = new XADataSourceImpl();

            if (ds == null) {
                throw new GenericEntityException("XADataSource was not created, big problem!");
            }
            
            String jdbcUri = UtilValidate.isNotEmpty(helperInfo.getOverrideJdbcUri()) ? helperInfo.getOverrideJdbcUri() : jdbcElement.getAttribute("jdbc-uri");
            String jdbcUsername = UtilValidate.isNotEmpty(helperInfo.getOverrideUsername()) ? helperInfo.getOverrideUsername() : jdbcElement.getAttribute("jdbc-username");
            String jdbcPassword = UtilValidate.isNotEmpty(helperInfo.getOverridePassword()) ? helperInfo.getOverridePassword() : jdbcElement.getAttribute("jdbc-password");

            ds.setDriver(jdbcElement.getAttribute("jdbc-driver"));
            ds.setURL(jdbcUri);

            String transIso = jdbcElement.getAttribute("isolation-level");
            if (UtilValidate.isNotEmpty(transIso)) {
                if ("Serializable".equals(transIso)) {
                    pds.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                } else if ("RepeatableRead".equals(transIso)) {
                    pds.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                } else if ("ReadUncommitted".equals(transIso)) {
                    pds.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                } else if ("ReadCommitted".equals(transIso)) {
                    pds.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                } else if ("None".equals(transIso)) {
                    pds.setTransactionIsolation(Connection.TRANSACTION_NONE);
                }
            }

            // set the datasource in the pool
            pds.setDataSource(ds);
            pds.setJDBCUser(jdbcUsername);
            pds.setJDBCPassword(jdbcPassword);

            // set the transaction manager in the pool
            pds.setTransactionManager(TransactionFactory.getTransactionManager());

            // configure the pool settings
            try {
                pds.setMaxSize(Integer.parseInt(jdbcElement.getAttribute("pool-maxsize")));
            } catch (NumberFormatException nfe) {
                Debug.logError("Problems with pool settings [pool-maxsize=" + jdbcElement.getAttribute("pool-maxsize") + "]; the values MUST be numbers, using default of 20.", module);
                pds.setMaxSize(20);
            } catch (Exception e) {
                Debug.logError(e, "Problems with pool settings", module);
                pds.setMaxSize(20);
            }
            try {
                pds.setMinSize(Integer.parseInt(jdbcElement.getAttribute("pool-minsize")));
            } catch (NumberFormatException nfe) {
                Debug.logError("Problems with pool settings [pool-minsize=" + jdbcElement.getAttribute("pool-minsize") + "]; the values MUST be numbers, using default of 5.", module);
                pds.setMinSize(2);
            } catch (Exception e) {
                Debug.logError(e, "Problems with pool settings", module);
                pds.setMinSize(2);
            }

            if (jdbcElement.hasAttribute("idle-maxsize")) {
                Debug.logWarning("idle-maxsize is not supported by MinervaConnectionFactory, ignoring value", module);
            }

            // cache the pool
            dsCache.put(helperInfo.getHelperFullName(), pds);

            return TransactionFactory.getCursorConnection(helperInfo, pds.getConnection());
        }
    }

    public void closeAll() {
        for (String helperName: dsCache.keySet()) {
            XAPoolDataSource pds = dsCache.remove(helperName);
            pds.close();
        }
    }

    // static methods for webtools
    public static <X> Set<X> getPooledData(String helperName) throws GenericEntityException {
        XAPoolDataSource pds = dsCache.get(helperName);
        if (pds == null) {
            Debug.logError("No pool found for helper name [" + helperName + "]", module);
            return new HashSet<X>();
        } else {
            return UtilGenerics.cast(pds.getPooledObjectRecords(0)); // 0 to return all (in use and waiting)
        }
    }

    public static String getPoolName(String helperName) throws GenericEntityException {
        XAPoolDataSource pds = dsCache.get(helperName);
        if (pds == null) {
            Debug.logError("No pool found for helper name [" + helperName + "]", module);
            return null;
        }
        return pds.getPoolDataString();
    }
}
