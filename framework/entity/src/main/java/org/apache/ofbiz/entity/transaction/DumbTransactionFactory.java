/*
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

package org.apache.ofbiz.entity.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.jdbc.ConnectionFactoryLoader;

/**
 * A dumb, non-working transaction manager.
 */
public class DumbTransactionFactory implements TransactionFactory {

    private static final String MODULE = DumbTransactionFactory.class.getName();

    @Override
    public TransactionManager getTransactionManager() {
        return new TransactionManager() {
            @Override
            public void begin() throws NotSupportedException, SystemException {
            }

            @Override
            public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
                    IllegalStateException, SystemException {
            }

            @Override
            public int getStatus() throws SystemException {
                return TransactionUtil.STATUS_NO_TRANSACTION;
            }

            @Override
            public Transaction getTransaction() throws SystemException {
                return null;
            }

            @Override
            public void resume(Transaction transaction) throws InvalidTransactionException, IllegalStateException, SystemException {
            }

            @Override
            public void rollback() throws IllegalStateException, SecurityException, SystemException {
            }

            @Override
            public void setRollbackOnly() throws IllegalStateException, SystemException {
            }

            @Override
            public void setTransactionTimeout(int i) throws SystemException {
            }

            @Override
            public Transaction suspend() throws SystemException {
                return null;
            }
        };
    }

    @Override
    public UserTransaction getUserTransaction() {
        return new UserTransaction() {
            @Override
            public void begin() throws NotSupportedException, SystemException {
            }

            @Override
            public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
                    IllegalStateException, SystemException {
            }

            @Override
            public int getStatus() throws SystemException {
                return TransactionUtil.STATUS_NO_TRANSACTION;
            }

            @Override
            public void rollback() throws IllegalStateException, SecurityException, SystemException {
            }

            @Override
            public void setRollbackOnly() throws IllegalStateException, SystemException {
            }

            @Override
            public void setTransactionTimeout(int i) throws SystemException {
            }
        };
    }

    @Override
    public String getTxMgrName() {
        return "dumb";
    }

    @Override
    public Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException {
        Datasource datasourceInfo = EntityConfig.getDatasource(helperInfo.getHelperBaseName());

        if (datasourceInfo.getInlineJdbc() != null) {
            Connection otherCon = ConnectionFactoryLoader.getInstance().getConnection(helperInfo, datasourceInfo.getInlineJdbc());
            return TransactionUtil.getCursorConnection(helperInfo, otherCon);
        }
        Debug.logError("Dumb/Empty is the configured transaction manager but no inline-jdbc element was specified in the "
                + helperInfo.getHelperBaseName() + " datasource. Please check your configuration", MODULE);
        return null;
    }

    @Override
    public void shutdown() { }
}
