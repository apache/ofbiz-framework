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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.minerva.pool.jdbc.xa.XAPoolDataSource;
import org.ofbiz.minerva.pool.jdbc.xa.wrapper.XADataSourceImpl;
import org.w3c.dom.Element;

/**
 * MinervaConnectionFactory - Central source for Minerva JDBC Objects
 */
public class MinervaConnectionFactory {
        
    public static final String module = MinervaConnectionFactory.class.getName();                
        
    protected static Map dsCache = new HashMap();
    
    public static Connection getConnection(String helperName, Element jotmJdbcElement) throws SQLException, GenericEntityException {                               
        XAPoolDataSource pds = (XAPoolDataSource) dsCache.get(helperName);        
        if (pds != null) {                                  
            return TransactionFactory.getCursorConnection(helperName, pds.getConnection());
        }
        
        synchronized (MinervaConnectionFactory.class) {
            pds = (XAPoolDataSource) dsCache.get(helperName);
            if (pds != null) {                           
                return pds.getConnection();
            } else {
                pds = new XAPoolDataSource();
                pds.setPoolName(helperName);
            }

            XADataSourceImpl ds = new XADataSourceImpl();

            if (ds == null)
                throw new GenericEntityException("XADataSource was not created, big problem!");
            
            ds.setDriver(jotmJdbcElement.getAttribute("jdbc-driver"));
            ds.setURL(jotmJdbcElement.getAttribute("jdbc-uri"));
            
            String transIso = jotmJdbcElement.getAttribute("isolation-level");
            if (transIso != null && transIso.length() > 0) {
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
            pds.setJDBCUser(jotmJdbcElement.getAttribute("jdbc-username"));
            pds.setJDBCPassword(jotmJdbcElement.getAttribute("jdbc-password"));
            
            // set the transaction manager in the pool
            pds.setTransactionManager(TransactionFactory.getTransactionManager());
            
            // configure the pool settings
            try {            
                pds.setMaxSize(Integer.parseInt(jotmJdbcElement.getAttribute("pool-maxsize")));
            } catch (NumberFormatException nfe) {
                Debug.logError("Problems with pool settings [pool-maxsize=" + jotmJdbcElement.getAttribute("pool-maxsize") + "]; the values MUST be numbers, using default of 20.", module);
                pds.setMaxSize(20);
            } catch (Exception e) {
                Debug.logError(e, "Problems with pool settings", module);
                pds.setMaxSize(20);
            }
            try {            
                pds.setMinSize(Integer.parseInt(jotmJdbcElement.getAttribute("pool-minsize")));
            } catch (NumberFormatException nfe) {
                Debug.logError("Problems with pool settings [pool-minsize=" + jotmJdbcElement.getAttribute("pool-minsize") + "]; the values MUST be numbers, using default of 5.", module);
                pds.setMinSize(2);
            } catch (Exception e) {
                Debug.logError(e, "Problems with pool settings", module);
                pds.setMinSize(2);
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
            XAPoolDataSource pds = (XAPoolDataSource) dsCache.remove(helperName);
            pds.close();   
        }                                                                             
    }
}
