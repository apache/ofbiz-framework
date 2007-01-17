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

//import java.util.*;
//import java.sql.*;
//import org.w3c.dom.Element;

//import org.ofbiz.entity.*;
//import org.ofbiz.base.util.*;

// For Tyrex 0.9.8.5
// import tyrex.resource.jdbc.xa.*;

// For Tyrex 0.9.7.0
// import tyrex.jdbc.xa.*;

/**
 * Tyrex ConnectionFactory - central source for JDBC connections from Tyrex
 */
public class TyrexConnectionFactory {
    public static final String module = TyrexConnectionFactory.class.getName();
}
/*
public class TyrexConnectionFactory {
    public static final String module = TyrexConnectionFactory.class.getName();

    // protected static UtilCache dsCache = new UtilCache("entity.TyrexDataSources", 0, 0);
    protected static Map dsCache = new HashMap();

    public static Connection getConnection(String helperName, Element inlineJdbcElement) throws SQLException, GenericEntityException {
        boolean usingTyrex = true;

        if (usingTyrex) {
            EnabledDataSource ds;

            // try once
            ds = (EnabledDataSource) dsCache.get(helperName);
            if (ds != null) {
                return TransactionFactory.getCursorConnection(helperName, TransactionUtil.enlistConnection(ds.getXAConnection()));
            }

            synchronized (TyrexConnectionFactory.class) {
                // try again inside the synch just in case someone when through while we were waiting
                ds = (EnabledDataSource) dsCache.get(helperName);
                if (ds != null) {
                    return TransactionUtil.enlistConnection(ds.getXAConnection());
                }

                ds = new EnabledDataSource();
                ds.setDriverClassName(inlineJdbcElement.getAttribute("jdbc-driver"));
                ds.setDriverName(inlineJdbcElement.getAttribute("jdbc-uri"));
                ds.setUser(inlineJdbcElement.getAttribute("jdbc-username"));
                ds.setPassword(inlineJdbcElement.getAttribute("jdbc-password"));
                ds.setDescription(helperName);

                String transIso = inlineJdbcElement.getAttribute("isolation-level");

                if (transIso != null && transIso.length() > 0)
                    ds.setIsolationLevel(transIso);

                ds.setLogWriter(Debug.getPrintWriter());

                dsCache.put(helperName, ds);
                return TransactionFactory.getCursorConnection(helperName, TransactionUtil.enlistConnection(ds.getXAConnection()));
            }
        }

        return null;
    }
    
    public static void closeAll() {
        Set cacheKeys = dsCache.keySet();
        Iterator i = cacheKeys.iterator();
        while (i.hasNext()) {
            String helperName = (String) i.next();
            EnabledDataSource ed = (EnabledDataSource) dsCache.remove(helperName);
            ed = null;   
        }                                                                             
    }   
}
*/

