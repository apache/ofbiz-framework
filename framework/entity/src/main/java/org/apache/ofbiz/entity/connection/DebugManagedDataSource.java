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

package org.apache.ofbiz.entity.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbcp2.managed.ManagedDataSource;
import org.apache.commons.dbcp2.managed.TransactionRegistry;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.ofbiz.base.util.Debug;

public class DebugManagedDataSource<C extends Connection> extends ManagedDataSource<C> {

    public static final String module = DebugManagedDataSource.class.getName();

    public DebugManagedDataSource(ObjectPool<C> pool, TransactionRegistry transactionRegistry) {
        super(pool, transactionRegistry);
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (Debug.verboseOn()) {
            if (super.getPool() instanceof GenericObjectPool) {
                GenericObjectPool<?> objectPool = (GenericObjectPool<?>)super.getPool();
                Debug.logVerbose("Borrowing a connection from the pool; used/idle/total: " + objectPool.getNumActive() + "/" + objectPool.getNumIdle() + "/" + (objectPool.getNumActive() + objectPool.getNumIdle()) + "; min idle/max idle/max total: " + objectPool.getMinIdle() + "/" + objectPool.getMaxIdle() + "/" + objectPool.getMaxTotal(), module);
            } else {
                if (Debug.verboseOn()) Debug.logVerbose("Borrowing a connection from the pool; used/idle/total: " + super.getPool().getNumActive() + "/" + super.getPool().getNumIdle() + "/" + (super.getPool().getNumActive() + super.getPool().getNumIdle()), module);
            }
        }
        return super.getConnection();
    }

    public Map<String, Object> getInfo() {
        Map<String, Object> dataSourceInfo = new HashMap<>();
        dataSourceInfo.put("poolNumActive", super.getPool().getNumActive());
        dataSourceInfo.put("poolNumIdle", super.getPool().getNumIdle());
        dataSourceInfo.put("poolNumTotal", (super.getPool().getNumIdle() + super.getPool().getNumActive()));
        if (super.getPool() instanceof GenericObjectPool) {
            GenericObjectPool<?> objectPool = (GenericObjectPool<?>)super.getPool();
            dataSourceInfo.put("poolMaxActive", objectPool.getMaxTotal());
            dataSourceInfo.put("poolMaxIdle", objectPool.getMaxIdle());
            dataSourceInfo.put("poolMaxWait", objectPool.getMaxWaitMillis());
            dataSourceInfo.put("poolMinEvictableIdleTimeMillis", objectPool.getMinEvictableIdleTimeMillis());
            dataSourceInfo.put("poolMinIdle", objectPool.getMinIdle());
        }
        return dataSourceInfo;
    }

    // Ensures that the close() method does not throw an InterruptedException
    // to conform to the AutoCloseable interface.
    // This is fixing what should be done in org.apache.commons.dbcp2.PoolingDataSource.
    @Override
    public void close() throws SQLException, RuntimeException {
        try {
            super.close();
        } catch (SQLException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Debug.log(e);
        }
    }

}
