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
 */

/**
*  AddTotalAmount.bsh will create a map with the content of the invoice header
*  and add the field totalAmount to it which contains the total amount of all invoiceItems.
*/ 
import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.*;
import org.ofbiz.accounting.invoice.*;
import java.text.DateFormat;
 
invoiceId = request.getParameter("invoiceId");

double invoiceTotal = 0;
invoice = delegator.findByPrimaryKey("Invoice", [invoiceId : invoiceId]);
newInvoice = [:];

if (invoice) {
    invoiceItems = invoice.getRelated("InvoiceItem");
    if (invoiceItems) {
        invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice);
    }
    newInvoice.putAll(invoice);
}

newInvoice.invoiceAmount = invoiceTotal;
context.invoiceExt = newInvoice;
context.invoiceId = invoiceId;
