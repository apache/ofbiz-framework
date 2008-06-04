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
import java.util.ArrayList;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.accounting.util.UtilAccounting;

// rounding mode
decimals = UtilAccounting.getBigDecimalScale("invoice.decimals");
rounding = UtilAccounting.getBigDecimalRoundingMode("invoice.rounding");
context.decimals = decimals;
context.rounding = rounding;


// first ensure ability to print
if (!security.hasEntityPermission("ACCOUNTING", "_PRINT_CHECKS", session)) {
    context.payments = []; // if no permission, just pass an empty list for now
    return;
}

// list of payments
payments = [];
// in the case of a single payment, the paymentId will be supplied
paymentId = context.paymentId;
if (paymentId) {
    payment = delegator.findByPrimaryKey("Payment", [paymentId : paymentId]);
    if (payment) payments.add(payment);
    context.payments = payments;
    return;
}

// in the case of a multi form, parse the multi data and get all of the selected payments
selected = UtilHttp.parseMultiFormData(parameters);
selected.each {
    payment = delegator.findByPrimaryKey("Payment", [paymentId : it.paymentId]);
    if (payment) payments.add(payment);    
}
context.payments = payments;
