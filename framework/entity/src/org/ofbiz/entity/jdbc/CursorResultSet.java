/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.entity.jdbc;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @version    $Rev$
 * @since      3.1
 */
public class CursorResultSet extends AbstractCursorHandler {

    protected ResultSet rs;
    protected Statement stmt;
    protected String query;

    protected CursorResultSet(Statement stmt, String cursorName, int fetchSize) throws SQLException {
        super(cursorName, fetchSize);
        this.stmt = stmt;
        query = "FETCH FORWARD " + fetchSize + " IN " + cursorName;
        System.err.println("executing page fetch(1)");
        rs = stmt.executeQuery(query);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("close".equals(method.getName())) {
            close();
            return null;
        } else if ("next".equals(method.getName())) {
            return next() ? Boolean.TRUE : Boolean.FALSE;
        }
        return super.invoke(rs, proxy, method, args);
    }

    protected boolean next() throws SQLException {
        if (rs.next()) return true;
        System.err.println("executing page fetch(2)");
        rs = stmt.executeQuery(query);
        return rs.next();
    }

    protected void close() throws SQLException {
        stmt.executeUpdate("CLOSE " + cursorName);
        rs.close();
    }

    public static ResultSet newCursorResultSet(Statement stmt, String cursorName, int fetchSize) throws SQLException, Exception {
        return (ResultSet) newHandler(new CursorResultSet(stmt, cursorName, fetchSize), ResultSet.class);
    }
}
