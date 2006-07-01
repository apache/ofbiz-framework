/*
 * $Id: DumbFactory.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.ofbiz.entity.transaction;

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

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.jdbc.ConnectionFactory;

/**
 * A dumb, non-working transaction manager.
 * 
 * @author     <a href="mailto:plightbo@hotmail.com">Pat Lightbody</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class DumbFactory implements TransactionFactoryInterface {
    
    public static final String module = DumbFactory.class.getName();
    
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
    
    public Connection getConnection(String helperName) throws SQLException, GenericEntityException {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);

        if (datasourceInfo.inlineJdbcElement != null) {
            Connection otherCon = ConnectionFactory.tryGenericConnectionSources(helperName, datasourceInfo.inlineJdbcElement);
            return TransactionFactory.getCursorConnection(helperName, otherCon);
        } else {
            Debug.logError("Dumb/Empty is the configured transaction manager but no inline-jdbc element was specified in the " + helperName + " datasource. Please check your configuration", module);
            return null;
        }
    }
    
    public void shutdown() {}    
}
