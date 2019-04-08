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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;


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
            return Integer.valueOf(getFetchSize());
        } else if ("setFetchSize".equals(method.getName())) {
            setFetchSize(((Integer) args[0]).intValue());
        }
        return super.invoke(stmt, proxy, method, args);
    }

    public static Statement newCursorStatement(Statement stmt, String cursorName, int fetchSize) throws Exception {
        return newHandler(new CursorStatement(stmt, cursorName, fetchSize), Statement.class);
    }

    public static PreparedStatement newCursorPreparedStatement(PreparedStatement pstmt, String cursorName, int fetchSize) throws Exception {
        return newHandler(new CursorStatement(pstmt, cursorName, fetchSize), PreparedStatement.class);
    }
}
