/*
 *  Copyright (c) 2003-2006 The Open For Business Project - www.ofbiz.org
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
 *
 *  @author Leon Torres (leon@opensourcestrategies.com)
 */

package org.ofbiz.accounting;

import org.ofbiz.service.GenericServiceException;

/**
 * Accounting Exceptions are to be distinguished from other exceptions as
 * serious problems in configuration, execution or logic in the Accounting
 * components that could lead to errors in Invoices, the General Ledger,
 * and Payments. When one of these occurs, make sure to reconcile the
 * affected data.
 */
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
