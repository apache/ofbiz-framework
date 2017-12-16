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

package org.apache.ofbiz.entity.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

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

    public static final String module = DumbTransactionFactory.class.getName();

    public TransactionManager getTransactionManager() {
        return new TransactionManager() {
            public void begin() throws NotSupportedException, SystemException {
            }

            public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
            }

            public int getStatus() throws SystemException {
                return TransactionUtil.STATUS_NO_TRANSACTION;
            }

            public Transaction getTransaction() throws SystemException {
                return null;
            }

            public void resume(Transaction transaction) throws InvalidTransactionException, IllegalStateException, SystemException {
            }

            public void rollback() throws IllegalStateException, SecurityException, SystemException {
            }

            public void setRollbackOnly() throws IllegalStateException, SystemException {
            }

            public void setTransactionTimeout(int i) throws SystemException {
            }

            public Transaction suspend() throws SystemException {
                return null;
            }
        };
    }

    public UserTransaction getUserTransaction() {
        return new UserTransaction() {
            public void begin() throws NotSupportedException, SystemException {
            }

            public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
            }

            public int getStatus() throws SystemException {
                return TransactionUtil.STATUS_NO_TRANSACTION;
            }

            public void rollback() throws IllegalStateException, SecurityException, SystemException {
            }

            public void setRollbackOnly() throws IllegalStateException, SystemException {
            }

            public void setTransactionTimeout(int i) throws SystemException {
            }
        };
    }

    public String getTxMgrName() {
        return "dumb";
    }

    public Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException {
        Datasource datasourceInfo = EntityConfig.getDatasource(helperInfo.getHelperBaseName());

        if (datasourceInfo.getInlineJdbc() != null) {
            Connection otherCon = ConnectionFactoryLoader.getInstance().getConnection(helperInfo, datasourceInfo.getInlineJdbc());
            return TransactionUtil.getCursorConnection(helperInfo, otherCon);
        }
        Debug.logError("Dumb/Empty is the configured transaction manager but no inline-jdbc element was specified in the " + helperInfo.getHelperBaseName() + " datasource. Please check your configuration", module);
        return null;
    }

    public void shutdown() {}
}
