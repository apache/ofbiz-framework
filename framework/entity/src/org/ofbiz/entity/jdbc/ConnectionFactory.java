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
package org.ofbiz.entity.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.w3c.dom.Element;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.transaction.MinervaConnectionFactory;
import org.ofbiz.entity.transaction.TransactionFactory;

/**
 * ConnectionFactory - central source for JDBC connections
 *
 */
public class ConnectionFactory {
    // Debug module name
    public static final String module = ConnectionFactory.class.getName();

    public static Connection getConnection(String driverName, String connectionUrl, Properties props, String userName, String password) throws SQLException {
        // first register the JDBC driver with the DriverManager
        if (driverName != null) {
            ConnectionFactory.loadDriver(driverName);
        }

        try {
            if (userName != null && userName.length() > 0)
                return DriverManager.getConnection(connectionUrl, userName, password);
            else if (props != null)
                return DriverManager.getConnection(connectionUrl, props);
            else
                return DriverManager.getConnection(connectionUrl);
        } catch (SQLException e) {
            Debug.logError(e, "SQL Error obtaining JDBC connection", module);
            throw e;
        }
    }

    public static Connection getConnection(String connectionUrl, String userName, String password) throws SQLException {
        return getConnection(null, connectionUrl, null, userName, password);
    }

    public static Connection getConnection(String connectionUrl, Properties props) throws SQLException {
        return getConnection(null, connectionUrl, props, null, null);
    }

    public static Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        // Debug.logVerbose("Getting a connection", module);

        Connection con = TransactionFactory.getConnection(helperName);
        if (con == null) {
            Debug.logError("******* ERROR: No database connection found for helperName \"" + helperName + "\"", module);
        }
        return con;
    }
    
    public static Connection tryGenericConnectionSources(String helperName, Element inlineJdbcElement) throws SQLException, GenericEntityException {
        // Minerva Based
        try {
            Connection con = MinervaConnectionFactory.getConnection(helperName, inlineJdbcElement);
            if (con != null) return con;
        } catch (Exception ex) {
            Debug.logError(ex, "There was an error getting a Minerva datasource.", module);
        }

        /* DEJ20040103 XAPool still seems to have some serious issues and isn't working right, of course we may not be using it right, but I don't really feel like trying to track it down now
        // XAPool & JOTM Based
        try {
            Connection con = XaPoolConnectionFactory.getConnection(helperName, inlineJdbcElement);
            if (con != null) return con;
        } catch (Exception ex) {
            Debug.logError(ex, "There was an error getting a Minerva datasource.", module);
        }
        */

        /* DEJ20050103 This pretty much never works anyway, so leaving out to reduce error messages when things go bad
        // next try DBCP
        try {
            Connection con = DBCPConnectionFactory.getConnection(helperName, inlineJdbcElement);
            if (con != null) return con;
        } catch (Exception ex) {
            Debug.logError(ex, "There was an error getting a DBCP datasource.", module);
        }
        
        // Default to plain JDBC.
        String driverClassName = inlineJdbcElement.getAttribute("jdbc-driver");
        if (driverClassName != null && driverClassName.length() > 0) {
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class clazz = loader.loadClass(driverClassName);
                clazz.newInstance();
            } catch (ClassNotFoundException e) {
                Debug.logWarning(e, "Could not find JDBC driver class named " + driverClassName, module);
                return null;
            } catch (java.lang.IllegalAccessException e) {
                Debug.logWarning(e, "Not allowed to access JDBC driver class named " + driverClassName, module);
                return null;
            } catch (java.lang.InstantiationException e) {
                Debug.logWarning(e, "Could not create new instance of JDBC driver class named " + driverClassName, module);
                return null;
            }
            return DriverManager.getConnection(inlineJdbcElement.getAttribute("jdbc-uri"),
                    inlineJdbcElement.getAttribute("jdbc-username"), inlineJdbcElement.getAttribute("jdbc-password"));
        }
        */

        return null;
    }

    public static void loadDriver(String driverName) throws SQLException {
        if (DriverManager.getDriver(driverName) == null) {
            try {
                Driver driver = (Driver) Class.forName(driverName, true, Thread.currentThread().getContextClassLoader()).newInstance();
                DriverManager.registerDriver(driver);
            } catch (ClassNotFoundException e) {
                Debug.logWarning(e, "Unable to load driver [" + driverName + "]", module);
            } catch (InstantiationException e) {
                Debug.logWarning(e, "Unable to instantiate driver [" + driverName + "]", module);
            } catch (IllegalAccessException e) {
                Debug.logWarning(e, "Illegal access exception [" + driverName + "]", module);
            }
        }
    }

    public static void unloadDriver(String driverName) throws SQLException {
        Driver driver = DriverManager.getDriver(driverName);
        if (driver != null) {
            DriverManager.deregisterDriver(driver);
        }
    }
}
