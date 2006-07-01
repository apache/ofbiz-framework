/*
 * $Id: CursorConnection.java 5462 2005-08-05 18:35:48Z jonesde $
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 *
 * @version    $Rev$
 * @since      3.1
 */
public class CursorConnection extends AbstractCursorHandler {

    public static Connection newCursorConnection(Connection con, String cursorName, int pageSize) throws Exception {
        return (Connection) newHandler(new CursorConnection(con, cursorName, pageSize), Connection.class);
    }

    protected Connection con;

    protected CursorConnection(Connection con, String cursorName, int fetchSize) {
        super(cursorName, fetchSize);
        this.con = con;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("prepareStatement")) {
            System.err.println("prepareStatement");
            args[0] = "DECLARE " + cursorName + " CURSOR FOR " + args[0];
            PreparedStatement pstmt = (PreparedStatement) method.invoke(con, args);
            return CursorStatement.newCursorPreparedStatement(pstmt, cursorName, fetchSize);
        } else if (method.getName().equals("createStatement")) {
            System.err.println("createStatement");
            Statement stmt = (Statement) method.invoke(con, args);
            return CursorStatement.newCursorStatement(stmt, cursorName, fetchSize);
        }
        return super.invoke(con, proxy, method, args);
    }
}
