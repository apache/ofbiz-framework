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

package org.apache.ofbiz.accounting;

import org.apache.ofbiz.service.GenericServiceException;

/**
 * Accounting Exceptions are to be distinguished from other exceptions as
 * serious problems in configuration, execution or logic in the Accounting
 * components that could lead to errors in Invoices, the General Ledger,
 * and Payments. When one of these occurs, make sure to reconcile the
 * affected data.
 */
@SuppressWarnings("serial")
public class AccountingException extends GenericServiceException {

    public AccountingException() {
        super();
    }

    public AccountingException(Throwable throwable) {
        super(throwable);
    }

    public AccountingException(String string) {
        super(string);
    }

    public AccountingException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
