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
import org.ofbiz.base.util.*;
import org.ofbiz.service.*;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.widget.html.*;
import org.ofbiz.entity.*;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.entity.condition.*;
import org.ofbiz.manufacturing.mrp.MrpServices;
import org.ofbiz.base.util.Debug;

GenericDelegator delegator = request.getAttribute("delegator");
LocalDispatcher dispatcher = request.getAttribute("dispatcher");

productId = request.getParameter("productId");

// get the lookup flag
lookupFlag = request.getParameter("lookupFlag");

// blank param list
paramList = "";
inventoryList = null;

if (lookupFlag != null) {
    paramList = paramList + "&lookupFlag=" + lookupFlag;
    lookupErrorMessage = null;   
    andExprs = new ArrayList();
     
    //define main condition
    mainCond = null;

    // now do the filtering
    
    eventDate = request.getParameter("eventDate");
    if (eventDate != null && eventDate.length() > 8) {            
    eventDate = eventDate.trim();
    if (eventDate.length() < 14) eventDate = eventDate + " " + "00:00:00.000";
    paramList = paramList + "&eventDate=" + eventDate;
        andExprs.add(EntityCondition.makeCondition("eventDate", EntityOperator.GREATER_THAN, eventDate));
    }
    
    if (productId != null && productId.length() > 0) {
            paramList = paramList + "&productId=" + productId;
        if ( productId.length() > 0)
        andExprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
    } 
    andExprs.add(EntityCondition.makeCondition("mrpEventTypeId", EntityOperator.NOT_EQUAL, "INITIAL_QOH"));
    andExprs.add(EntityCondition.makeCondition("mrpEventTypeId", EntityOperator.NOT_EQUAL, "ERROR"));
    andExprs.add(EntityCondition.makeCondition("mrpEventTypeId", EntityOperator.NOT_EQUAL, "REQUIRED_MRP"));

    mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND); 
    
    if ( mainCond != null) {
    // do the lookup
        inventoryList = delegator.findList("MrpEvent", mainCond, null, UtilMisc.toList("productId", "eventDate"), null, false);
    }
    
    context.put("inventoryList", inventoryList);
}
context.put("paramList", paramList);

// set the page parameters
viewIndex = 0;
try {
    viewIndex = Integer.valueOf((String) request.getParameter("VIEW_INDEX")).intValue();
} catch (Exception e) {}
viewSize = 100;
try {
    viewSize = Integer.valueOf((String) request.getParameter("VIEW_SIZE")).intValue();
} catch (Exception e) {}
listSize = 0;
if (inventoryList != null)
    listSize = inventoryList.size();

lowIndex = viewIndex * viewSize;
highIndex = (viewIndex + 1) * viewSize;
if (listSize < highIndex) 
    highIndex = listSize;
if( highIndex < 1 )
    highIndex = 0;
context.put("viewIndex", viewIndex);
context.put("listSize", listSize);
context.put("highIndex", highIndex);
context.put("lowIndex", lowIndex);
context.put("viewSize", viewSize);

