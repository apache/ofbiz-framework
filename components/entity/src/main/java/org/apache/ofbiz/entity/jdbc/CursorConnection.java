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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.apache.ofbiz.base.util.Debug;


public class CursorConnection extends AbstractCursorHandler {

    private static final String MODULE = CursorConnection.class.getName();
    public static Connection newCursorConnection(Connection con, String cursorName, int pageSize) throws Exception {
        return newHandler(new CursorConnection(con, cursorName, pageSize), Connection.class);
    }

    private Connection con;

    protected CursorConnection(Connection con, String cursorName, int fetchSize) {
        super(cursorName, fetchSize);
        this.con = con;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("prepareStatement".equals(method.getName())) {
            Debug.logInfo("prepareStatement", MODULE);
            args[0] = "DECLARE " + getCursorName() + " CURSOR FOR " + args[0];
            PreparedStatement pstmt = (PreparedStatement) method.invoke(con, args);
            return CursorStatement.newCursorPreparedStatement(pstmt, getCursorName(), getFetchSize());
        } else if ("createStatement".equals(method.getName())) {
            Debug.logInfo("createStatement", MODULE);
            Statement stmt = (Statement) method.invoke(con, args);
            return CursorStatement.newCursorStatement(stmt, getCursorName(), getFetchSize());
        }
        return super.invoke(con, proxy, method, args);
    }
}
