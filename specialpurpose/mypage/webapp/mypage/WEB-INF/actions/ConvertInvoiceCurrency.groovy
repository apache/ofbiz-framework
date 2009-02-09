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

import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.*;
import org.ofbiz.accounting.invoice.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import org.ofbiz.base.util.UtilNumber;
import javolution.util.FastList;

conversionRate = BigDecimal.ONE;
ZERO = BigDecimal.ZERO;
int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
invoiceType = delegator.findByPrimaryKey("InvoiceType", ["invoiceTypeId" : invoiceTypeId]);
  if (invoiceType.parentTypeId.equals("SALES_INVOICE")) { 
    otherCurrency = delegator.findByPrimaryKey("Party", ["partyId" : partyId]).preferredCurrencyUomId;
  } else {
    otherCurrency = delegator.findByPrimaryKey("Party", ["partyId" : partyIdFrom]).preferredCurrencyUomId;
  }
  // check if conversion required
  if (currencyUomId && otherCurrency && otherCurrency != currencyUomId && !otherCurrency.equals(currencyUomId)) {
    result = dispatcher.runSync("convertUom", [uomId : currencyUomId, 
                                               uomIdTo : otherCurrency, 
                                               originalValue : BigDecimal.ONE, 
                                               asOfDate : invoiceDate]);
    
    if (result.convertedValue != null) {
        context.total = (org.ofbiz.accounting.invoice.InvoiceWorker.getInvoiceTotal(delegator,invoiceId)).multiply((BigDecimal)result.convertedValue).setScale(decimals, rounding); 
        context.amountToApply = org.ofbiz.accounting.invoice.InvoiceWorker.getInvoiceNotApplied(delegator,invoiceId).multiply((BigDecimal)result.convertedValue).setScale(decimals, rounding);
        context.currencyUomId = otherCurrency;
    }
  } else {
      context.total = (org.ofbiz.accounting.invoice.InvoiceWorker.getInvoiceTotal(delegator,invoiceId)); 
      context.amountToApply = org.ofbiz.accounting.invoice.InvoiceWorker.getInvoiceNotApplied(delegator,invoiceId);
  }
