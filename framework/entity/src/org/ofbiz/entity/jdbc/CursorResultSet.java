/*
 * $Id: CursorResultSet.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
