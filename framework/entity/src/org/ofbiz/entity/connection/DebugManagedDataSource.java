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

package org.ofbiz.entity.connection;

import org.apache.commons.dbcp.managed.ManagedDataSource;
import org.apache.commons.dbcp.managed.TransactionRegistry;
import org.apache.commons.pool.ObjectPool;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.ofbiz.base.util.Debug;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DebugManagedDataSource extends ManagedDataSource {

    public static final String module = DebugManagedDataSource.class.getName();

    public DebugManagedDataSource() {
        super();
    }
    public DebugManagedDataSource(ObjectPool pool, TransactionRegistry transactionRegistry) {
        super(pool, transactionRegistry);
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (Debug.verboseOn()) {
            if (super._pool instanceof GenericObjectPool) {
                GenericObjectPool objectPool = (GenericObjectPool)super._pool;
                Debug.logVerbose("Borrowing a connection from the pool; used/total: " + objectPool.getNumActive() + "/" + objectPool.getNumActive() + objectPool.getNumIdle() + "; min idle/max idle/max total: " + objectPool.getMinIdle() + "/" + objectPool.getMaxIdle() + "/" + objectPool.getMaxActive(), module);
            } else {
                Debug.logVerbose("Borrowing a connection from the pool; used/total: " + super._pool.getNumActive() + "/" + (super._pool.getNumActive() + super._pool.getNumIdle()), module);
            }
        }
        return super.getConnection();
    }

    public Map<String, Object> getInfo() {
        Map<String, Object> dataSourceInfo = new HashMap<String, Object>();
        dataSourceInfo.put("poolNumActive", super._pool.getNumActive());
        dataSourceInfo.put("poolNumIdle", super._pool.getNumIdle());
        dataSourceInfo.put("poolNumTotal", (super._pool.getNumIdle() + super._pool.getNumActive()));
        if (super._pool instanceof GenericObjectPool) {
            GenericObjectPool objectPool = (GenericObjectPool)super._pool;
            dataSourceInfo.put("poolMaxActive", objectPool.getMaxActive());
            dataSourceInfo.put("poolMaxIdle", objectPool.getMaxIdle());
            dataSourceInfo.put("poolMaxWait", objectPool.getMaxWait());
            dataSourceInfo.put("poolMinEvictableIdleTimeMillis", objectPool.getMinEvictableIdleTimeMillis());
            dataSourceInfo.put("poolMinIdle", objectPool.getMinIdle());
        }
        return dataSourceInfo;
    }

}
