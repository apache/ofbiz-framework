/*
 * $Id: CursorStatement.java 5462 2005-08-05 18:35:48Z jonesde $
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

/**
 *
 * @version    $Rev$
 * @since      3.1
 */
public class CursorStatement extends AbstractCursorHandler {
    
    protected ResultSet currentResultSet;
    protected Statement stmt;
    protected boolean beganTransaction;
    protected boolean autoCommit;

    protected CursorStatement(Statement stmt, String cursorName, int fetchSize) throws GenericTransactionException, SQLException {
        super(cursorName, fetchSize);
        this.stmt = stmt;
        beganTransaction = TransactionUtil.begin();
        autoCommit = stmt.getConnection().getAutoCommit();
        stmt.getConnection().setAutoCommit(false);
        System.err.println("beganTransaction=" + beganTransaction + ", autoCommit=" + autoCommit);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("close".equals(method.getName())) {
            stmt.getConnection().setAutoCommit(autoCommit);
            TransactionUtil.commit(beganTransaction);
            stmt.close();
            return null;
        } else if ("execute".equals(method.getName())) {
        } else if ("executeQuery".equals(method.getName()) && args == null) {
            PreparedStatement pstmt = (PreparedStatement) stmt;
            pstmt.executeUpdate();
            currentResultSet = CursorResultSet.newCursorResultSet(stmt, cursorName, fetchSize);
            return currentResultSet;
        } else if ("executeQuery".equals(method.getName()) && args != null) {
            args[0] = "DECLARE " + cursorName + " CURSOR FOR " + args[0];
            System.err.println("query=" + args[0]);
            if (stmt.execute((String) args[0])) {
                throw new SQLException("DECLARE returned a ResultSet");
            }
            currentResultSet = CursorResultSet.newCursorResultSet(stmt, cursorName, fetchSize);
            return currentResultSet;
        } else if ("getMoreResults".equals(method.getName())) {
            boolean hasMoreResults = stmt.getMoreResults();
            if (hasMoreResults) {
                currentResultSet = stmt.getResultSet();
            } else {
                currentResultSet = null;
            }
            return hasMoreResults ? Boolean.TRUE : Boolean.FALSE;
        } else if ("getResultSet".equals(method.getName())) {
            return currentResultSet;
        } else if ("getCursorName".equals(method.getName())) {
            return getCursorName();
        } else if ("setCursorName".equals(method.getName())) {
            setCursorName((String) args[0]);
        } else if ("getFetchSize".equals(method.getName())) {
            return new Integer(getFetchSize());
        } else if ("setFetchSize".equals(method.getName())) {
            setFetchSize(((Integer) args[0]).intValue());
        }
        return super.invoke(stmt, proxy, method, args);
    }

    public static Statement newCursorStatement(Statement stmt, String cursorName, int fetchSize) throws Exception {
        return (Statement) newHandler(new CursorStatement(stmt, cursorName, fetchSize), Statement.class);
    }

    public static PreparedStatement newCursorPreparedStatement(PreparedStatement pstmt, String cursorName, int fetchSize) throws Exception {
        return (PreparedStatement) newHandler(new CursorStatement(pstmt, cursorName, fetchSize), PreparedStatement.class);
    }
}
