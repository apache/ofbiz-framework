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
package org.apache.ofbiz.entity.jdbc;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ofbiz.base.util.Debug;


public class CursorResultSet extends AbstractCursorHandler {

    private static final String MODULE = CursorResultSet.class.getName();
    private ResultSet rs;
    private Statement stmt;
    private String query;

    protected CursorResultSet(Statement stmt, String cursorName, int fetchSize) throws SQLException {
        super(cursorName, fetchSize);
        this.stmt = stmt;
        query = "FETCH FORWARD " + fetchSize + " IN " + cursorName;
        Debug.logInfo("executing page fetch(1)", MODULE);
        rs = stmt.executeQuery(query);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("close".equals(method.getName())) {
            close();
            return null;
        } else if ("next".equals(method.getName())) {
            return next() ? Boolean.TRUE : Boolean.FALSE;
        }
        return super.invoke(rs, proxy, method, args);
    }

    /**
     * Next boolean.
     * @return the boolean
     * @throws SQLException the sql exception
     */
    protected boolean next() throws SQLException {
        if (rs.next()) return true;
        Debug.logInfo("executing page fetch(2)", MODULE);
        rs = stmt.executeQuery(query);
        return rs.next();
    }

    /**
     * Close.
     * @throws SQLException the sql exception
     */
    protected void close() throws SQLException {
        stmt.executeUpdate("CLOSE " + getCursorName());
        rs.close();
    }

    public static ResultSet newCursorResultSet(Statement stmt, String cursorName, int fetchSize) throws SQLException, Exception {
        return newHandler(new CursorResultSet(stmt, cursorName, fetchSize), ResultSet.class);
    }
}
