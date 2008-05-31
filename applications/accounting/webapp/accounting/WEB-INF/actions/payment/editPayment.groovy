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

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.widget.html.HtmlFormWrapper;

paymentId = request.getParameter("paymentId") ?:request.getAttribute("paymentId");

payment = null;

if (paymentId) {
     payment = delegator.findByPrimaryKey("Payment", [paymentId : paymentId]);
}
context.payment = payment;

currentType = null;
currentStatus = null;
currentMethod = null;
paymentApplications = null;
if (payment) {
    // get the current type
    currentType = payment.getRelatedOne("PaymentType");
    // get the current status
    currentStatus = payment.getRelatedOne("StatusItem");
    // get the current method
    currentMethod = payment.getRelatedOne("PaymentMethodType");
    // get the payment's applications
    paymentApplications = payment.getRelated("PaymentApplication");
    HtmlFormWrapper paymentApplicationsWrapper = new HtmlFormWrapper("component://accounting/webapp/accounting/payment/PaymentForms.xml", "PaymentApplicationsList", request, response);
    paymentApplicationsWrapper.putInContext("entityList", paymentApplications);
    paymentApplicationsWrapper.putInContext("uiLabelMap", request.getAttribute("uiLabelMap"));
    context.paymentApplicationsWrapper = paymentApplicationsWrapper;
    HtmlFormWrapper editPaymentApplicationWrapper = new HtmlFormWrapper("component://accounting/webapp/accounting/payment/PaymentForms.xml", "EditPaymentApplication", request, response);
    editPaymentApplicationWrapper.putInContext("paymentApplication", null);
    editPaymentApplicationWrapper.putInContext("paymentId", paymentId);
    editPaymentApplicationWrapper.putInContext("uiLabelMap", request.getAttribute("uiLabelMap"));
    context.editPaymentApplicationWrapper = editPaymentApplicationWrapper;
}
context.currentType = currentType;
context.currentStatus = currentStatus;
context.currentMethod = currentMethod;
context.paymentApplications = paymentApplications;

// get the payment types
paymentTypes = delegator.findList("PaymentType", null, null, ["description"], null, false);
context.paymentTypes = paymentTypes;

// get the payment statuses
paymentStatuses = delegator.findByAnd("StatusItem", [statusTypeId : "PMNT_STATUS"], ["sequenceId", "description"]);
context.paymentStatuses = paymentStatuses;

// get the payment method types
paymentMethodTypes = delegator.findList("PaymentMethodType", null, null, ["description"], null, false);
context.paymentMethodTypes = paymentMethodTypes;
