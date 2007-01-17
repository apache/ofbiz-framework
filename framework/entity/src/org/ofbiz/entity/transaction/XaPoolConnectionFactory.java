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

import org.enhydra.jdbc.pool.StandardXAPoolDataSource;
import org.enhydra.jdbc.standard.StandardXADataSource;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.entity.GenericEntityException;
import org.w3c.dom.Element;

/**
 * JotmFactory - Central source for JOTM JDBC Objects
 */
public class XaPoolConnectionFactory {
        
    public static final String module = XaPoolConnectionFactory.class.getName();                
        
    protected static Map dsCache = new HashMap();
    
    public static Connection getConnection(String helperName, Element jotmJdbcElement) throws SQLException, GenericEntityException {                               
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
            String wrapperClass = jotmJdbcElement.getAttribute("pool-xa-wrapper-class");
            
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
            
            ds.setDriverName(jotmJdbcElement.getAttribute("jdbc-driver"));
            ds.setUrl(jotmJdbcElement.getAttribute("jdbc-uri"));
            ds.setUser(jotmJdbcElement.getAttribute("jdbc-username"));
            ds.setPassword(jotmJdbcElement.getAttribute("jdbc-password"));
            ds.setDescription(helperName);  
            ds.setTransactionManager(TransactionFactory.getTransactionManager()); 
            
            String transIso = jotmJdbcElement.getAttribute("isolation-level");
            if (transIso != null && transIso.length() > 0) {
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
                pds.setMaxSize(new Integer(jotmJdbcElement.getAttribute("pool-maxsize")).intValue());
                pds.setMinSize(new Integer(jotmJdbcElement.getAttribute("pool-minsize")).intValue());
                pds.setSleepTime(new Long(jotmJdbcElement.getAttribute("pool-sleeptime")).longValue());
                pds.setLifeTime(new Long(jotmJdbcElement.getAttribute("pool-lifetime")).longValue());
                pds.setDeadLockMaxWait(new Long(jotmJdbcElement.getAttribute("pool-deadlock-maxwait")).longValue());
                pds.setDeadLockRetryWait(new Long(jotmJdbcElement.getAttribute("pool-deadlock-retrywait")).longValue());
                
                // set the test statement to test connections
                String testStmt = jotmJdbcElement.getAttribute("pool-jdbc-test-stmt");
                if (testStmt != null && testStmt.length() > 0) {
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
