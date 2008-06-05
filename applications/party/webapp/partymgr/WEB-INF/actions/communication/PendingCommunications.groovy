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

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;

import javolution.util.FastList;

module = "PendingCommunications.groovy";

partyId = userLogin.partyId;

// indicator to display messages FROM this user
fromFlag = parameters.showFromEvents;

// get the sort field
sortField = parameters.sort;
//previous sort field
previousSort = parameters.previousSort;

if (previousSort && sortField && previousSort.equals(sortField)) {
    sortField = sortField.startsWith("-") ? sortField : "-" + sortField;
}

if (!sortField) sortField = previousSort;
if (!sortField) sortField = "entryDate";
context.previousSort = sortField;

// set the page parameters
viewIndex = 1;
try {
    viewIndex = Integer.valueOf((String) parameters.VIEW_INDEX).intValue();
} catch (Exception e) {
    viewIndex = 1;
}
context.viewIndex = viewIndex;

viewSize = 20;
try {
    viewSize = Integer.valueOf((String) parameters.VIEW_SIZE).intValue();
} catch (Exception e) {
    viewSize = 20;
}
if (viewSize > 100) {
    viewSize = 100;
}
context.viewSize = viewSize;

// get the logged in user's roles
partyRoles = delegator.findByAnd("PartyRole", [partyId : partyId]);

// build the party role list
pRolesList = FastList.newInstance();
partyRoles.each { partyRole ->
    if (!partyRole.roleTypeId.equals("_NA_")) {
        pRolesList.add(EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, partyRole.roleTypeId));
    }
}

// add in events with no role attached
pRolesList.add(EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, null));

// limit to just this user's events, or those not attached to a user
partyList = FastList.newInstance();
partyList.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, null));
partyList.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId));
if ("Y".equalsIgnoreCase(fromFlag)) {
    partyList.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, null));
    partyList.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId));
}

// limit to non-completed items
statusList = FastList.newInstance();
statusList.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "COM_COMPLETE"));
statusList.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "COM_RESOLVED"));
statusList.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "COM_REFERRED"));

// build the condition
expressions = FastList.newInstance();
expressions.add(EntityCondition.makeCondition(partyList, EntityOperator.OR));
expressions.add(EntityCondition.makeCondition(pRolesList, EntityOperator.OR));
expressions.add(EntityCondition.makeCondition(statusList, EntityOperator.AND));
condition = EntityCondition.makeCondition(expressions, EntityOperator.AND);

// specific fields to select
fieldsToSelect = null;

// sort order
orderBy = [sortField];

// entity find options
findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, false); 
// note distinct is false because it is not needed for a non-view-entity, and won't work with some databases when selecting a long text/clob field; also slows things down

boolean beganTransaction = false;
try {
    beganTransaction = TransactionUtil.begin();

    // obtain the ELI
    eli = delegator.find("CommunicationEvent", condition, null, fieldsToSelect, orderBy, findOpts);
    
    // get the indexes for the partial list
    lowIndex = (((viewIndex - 1) * viewSize) + 1);
    highIndex = viewIndex * viewSize;
    
    // get the partial list for this page
    eventList = eli.getPartialList(lowIndex, viewSize);
    if (!eventList) {
        eventList = new ArrayList();
    }
    
    // attempt to get the full size
    eli.last();
    eventListSize = eli.currentIndex();
    if (highIndex > eventListSize) {
        highIndex = eventListSize;
    }
    
    // close the list iterator
    eli.close();
    TransactionUtil.commit(beganTransaction);
} catch (Exception e) {
    String errMsg = "Failure in operation, rolling back transaction";
    Debug.logError(e, errMsg, module);
    try {
        // only rollback the transaction if we started one...
        TransactionUtil.rollback(beganTransaction, errMsg, e);
    } catch (Exception e2) {
        Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
    }
    // after rolling back, rethrow the exception
    throw e;
} finally {
    // only commit the transaction if we started one... this will throw an exception if it fails
    TransactionUtil.commit(beganTransaction);
}

    
context.eventList = eventList;
context.eventListSize = eventListSize;
context.highIndex = highIndex;
context.lowIndex = lowIndex;
