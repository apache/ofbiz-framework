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
import java.sql.Timestamp;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.base.util.*;

// get the payment types
paymentTypes = delegator.findList("PaymentType", null, null, ["description"], null, false);
context.paymentTypes = paymentTypes;

// get the payment statuses
paymentStatuses = delegator.findByAnd("StatusItem", [statusTypeId : "PMNT_STATUS"], ["sequenceId", "description"]);
context.paymentStatuses = paymentStatuses;

// get the payment method types
paymentMethodTypes = delegator.findList("PaymentMethodType", null, null, ["description"], null, false);
context.paymentMethodTypes = paymentMethodTypes;

// current selected status
currentStatusId = request.getParameter("paymentStatusId");
if (currentStatusId) {
    currentStatus = delegator.findByPrimaryKey("StatusItem", [statusId : currentStatusId]);
    context.currentStatus = currentStatus;
}

// current selected payment method
currentMethodId = request.getParameter("paymentMethodTypeId");
if (currentMethodId) {
    currentMethod = delegator.findByPrimaryKey("PaymentMethodType", [paymentMethodTypeId : currentMethodId]);
    context.currentMethod = currentMethod;
}


// create the fromDate for calendar
fromCal = Calendar.getInstance();
fromCal.setTime(new java.util.Date());
//fromCal.set(Calendar.DAY_OF_WEEK, fromCal.getActualMinimum(Calendar.DAY_OF_WEEK));
fromCal.set(Calendar.HOUR_OF_DAY, fromCal.getActualMinimum(Calendar.HOUR_OF_DAY));
fromCal.set(Calendar.MINUTE, fromCal.getActualMinimum(Calendar.MINUTE));
fromCal.set(Calendar.SECOND, fromCal.getActualMinimum(Calendar.SECOND));
fromCal.set(Calendar.MILLISECOND, fromCal.getActualMinimum(Calendar.MILLISECOND));
fromTs = new Timestamp(fromCal.getTimeInMillis());
fromStr = fromTs.toString();
fromStr = fromStr.substring(0, fromStr.indexOf('.'));
context.fromDateStr = fromStr;

// create the thruDate for calendar
toCal = Calendar.getInstance();
toCal.setTime(new java.util.Date());
//toCal.set(Calendar.DAY_OF_WEEK, toCal.getActualMaximum(Calendar.DAY_OF_WEEK));
toCal.set(Calendar.HOUR_OF_DAY, toCal.getActualMaximum(Calendar.HOUR_OF_DAY));
toCal.set(Calendar.MINUTE, toCal.getActualMaximum(Calendar.MINUTE));
toCal.set(Calendar.SECOND, toCal.getActualMaximum(Calendar.SECOND));
toCal.set(Calendar.MILLISECOND, toCal.getActualMaximum(Calendar.MILLISECOND));
toTs = new Timestamp(toCal.getTimeInMillis());
toStr = toTs.toString();
context.thruDateStr = toStr;

// get the lookup flag
lookupFlag = request.getParameter("lookupFlag");

// blank param list
paramList = "";

paymentList = null;
if (lookupFlag) {
    paramList += "&lookupFlag=" + lookupFlag;
    lookupErrorMessage = null;   
    andExprs = [];
    entityName = "Payment"; 

    // define the main condition
    mainCond = null;
    
    // now do the filtering
    if (!lookupErrorMessage) {               
        paymentType = request.getParameter("paymentType");
        paymentStatus = request.getParameter("paymentStatusId");
        paymentMethodType = request.getParameter("paymentMethodTypeId");
        fromPartyId = request.getParameter("fromPartyId");
        toPartyId = request.getParameter("toPartyId");
        minDate = request.getParameter("minDate");
        maxDate = request.getParameter("maxDate");

        if (!paymentType) paymentType = "ANY";
        if (!paymentStatus) paymentStatus = "ANY";
        if (!paymentMethodType) paymentMethodType = "ANY";
                
        paramList += "&paymentTypeId=" + paymentType;        
        if (!"ANY".equals(paymentType)) {            
            andExprs.add(EntityCondition.makeCondition("paymentTypeId", EntityOperator.EQUALS, paymentType));
        }
        paramList += "&paymentStatusId=" + paymentStatus;
        if (!"ANY".equals(paymentStatus)) {            
            andExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, paymentStatus));
        }
        paramList += "&paymentMethodTypeId=" + paymentMethodType;
        if (!"ANY".equals(paymentMethodType)) {            
            andExprs.add(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, paymentMethodType));
        }

        if (fromPartyId) {
            paramList += "&fromPartyId=" + fromPartyId;
            andExprs.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, fromPartyId));
            context.fromPartyId = fromPartyId;
        }

        if (toPartyId) {
            paramList += "&toPartyId=" + toPartyId;
            andExprs.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, toPartyId));
            context.toPartyId = toPartyId;
        }

        if (minDate && minDate.length() > 8) {            
            minDate = minDate.trim();
            if (minDate.length() < 14) minDate = minDate + " " + "00:00:00.000";
            paramList += "&minDate=" + minDate;
            andExprs.add(EntityCondition.makeCondition("effectiveDate", EntityOperator.GREATER_THAN_EQUAL_TO, ObjectType.simpleTypeConvert(minDate, "Timestamp", null, null)));        
        }
        if (maxDate && maxDate.length() > 8) {
            maxDate = maxDate.trim();
            if (maxDate.length() < 14) maxDate = maxDate + " " + "23:59:59.999";
            paramList += "&maxDate=" + maxDate;
            andExprs.add(EntityCondition.makeCondition("effectiveDate", EntityOperator.LESS_THAN_EQUAL_TO, ObjectType.simpleTypeConvert(maxDate, "Timestamp", null, null)));
        }

        mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);        

    }
    
    if ((!lookupErrorMessage) && mainCond) {
        // do the lookup
        paymentList = delegator.findList(entityName, mainCond, null, ["-effectiveDate"], null, false);
        Debug.log("" + paymentList);
    }

    context.paymentList = paymentList;
    
    if (lookupErrorMessage) {
        context.lookupErrorMessage = lookupErrorMessage;
    }
}

context.paramList = paramList;

// set the page parameters
viewIndex = 0;
try {
    viewIndex = Integer.valueOf((String) request.getParameter("VIEW_INDEX")).intValue();
} catch (Exception e) {
    viewIndex = 0;
}

viewSize = 20;
try {
    viewSize = Integer.valueOf((String) request.getParameter("VIEW_SIZE")).intValue();
} catch (Exception e) {
    viewSize = 20;
}

listSize = 0;
if (paymentList) {
    listSize = paymentList.size();
}

lowIndex = viewIndex * viewSize;
highIndex = (viewIndex + 1) * viewSize;
if (listSize < highIndex) {
    highIndex = listSize;
}
context.viewIndex = viewIndex;
context.listSize = listSize;
context.highIndex = highIndex;
context.lowIndex = lowIndex;
context.viewSize = viewSize;
