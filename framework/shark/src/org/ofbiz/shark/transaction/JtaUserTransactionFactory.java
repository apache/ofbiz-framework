/*
 * $Id: JtaUserTransactionFactory.java 7426 2006-04-26 23:35:58Z jonesde $
 *
 * Copyright 2004-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.shark.transaction;

import org.enhydra.shark.api.internal.usertransaction.UserTransactionFactory;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.UserTransaction;
import org.enhydra.shark.api.TransactionException;
import org.enhydra.shark.api.RootException;

/**
 * Shark JTA User Transaction Factory Implementation
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class JtaUserTransactionFactory implements UserTransactionFactory {

    protected CallbackUtilities callBack = null;

    public void configure(CallbackUtilities callBack) throws RootException {
        this.callBack = callBack;
    }

    public UserTransaction createTransaction() throws TransactionException {
        return new JtaTransaction();
    }    
}
