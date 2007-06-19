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

package org.ofbiz.workeffort.workeffort;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * WorkEffortServices - WorkEffort related Services
 */
public class WorkEffortServices {
    
    public static final String module = WorkEffortServices.class.getName();

    public static Map getWorkEffortAssignedTasks(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        List validWorkEfforts = null;

        if (userLogin != null && userLogin.get("partyId") != null) {
            try {
                validWorkEfforts = delegator.findByAnd("WorkEffortAndPartyAssign",
                            UtilMisc.toList(new EntityExpr("partyId", EntityOperator.EQUALS, userLogin.get("partyId")),
                                new EntityExpr("workEffortTypeId", EntityOperator.EQUALS, "TASK"),
                                new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"),
                                new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"),
                                new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"),
                                new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED")),
                            UtilMisc.toList("priority"));
                validWorkEfforts.addAll(delegator.findByAnd("WorkEffortAndPartyAssign",
                        UtilMisc.toList(new EntityExpr("partyId", EntityOperator.EQUALS, userLogin.get("partyId")),
                            new EntityExpr("workEffortTypeId", EntityOperator.EQUALS, "PROD_ORDER_TASK"),
                            new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CANCELLED "),
                            new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_COMPLETED"),
                            new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CLOSED")),
                        UtilMisc.toList("createdDate DESC")));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError("Error finding desired WorkEffort records: " + e.toString());
            }
        }

        Map result = new HashMap();
        if (validWorkEfforts == null) validWorkEfforts = new LinkedList();
        result.put("tasks", validWorkEfforts);
        return result;
    }

    public static Map getWorkEffortAssignedActivities(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        List validWorkEfforts = null;

        if (userLogin != null && userLogin.get("partyId") != null) {
            try {
                List constraints = new LinkedList();

                constraints.add(new EntityExpr("partyId", EntityOperator.EQUALS, userLogin.get("partyId")));
                constraints.add(new EntityExpr("workEffortTypeId", EntityOperator.EQUALS, "ACTIVITY"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));
                constraints.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "WF_COMPLETED"));
                constraints.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "WF_TERMINATED"));
                constraints.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "WF_ABORTED"));
                validWorkEfforts = delegator.findByAnd("WorkEffortAndPartyAssign", constraints, UtilMisc.toList("priority"));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError("Error finding desired WorkEffort records: " + e.toString());
            }
        }

        Map result = new HashMap();
        if (validWorkEfforts == null) validWorkEfforts = new LinkedList();
        result.put("activities", validWorkEfforts);
        return result;
    }

    public static Map getWorkEffortAssignedActivitiesByRole(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        List roleWorkEfforts = null;

        if (userLogin != null && userLogin.get("partyId") != null) {
            try {
                List constraints = new LinkedList();

                constraints.add(new EntityExpr("partyId", EntityOperator.EQUALS, userLogin.get("partyId")));
                constraints.add(new EntityExpr("workEffortTypeId", EntityOperator.EQUALS, "ACTIVITY"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));
                constraints.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "WF_COMPLETED"));
                constraints.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "WF_TERMINATED"));
                constraints.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "WF_ABORTED"));
                roleWorkEfforts = delegator.findByAnd("WorkEffortPartyAssignByRole", constraints, UtilMisc.toList("priority"));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError("Error finding desired WorkEffort records: " + e.toString());
            }
        }

        Map result = new HashMap();
        if (roleWorkEfforts == null) roleWorkEfforts = new LinkedList();
        result.put("roleActivities", roleWorkEfforts);
        return result;
    }

    public static Map getWorkEffortAssignedActivitiesByGroup(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        List groupWorkEfforts = null;

        if (userLogin != null && userLogin.get("partyId") != null) {
            try {
                List constraints = new LinkedList();

                constraints.add(new EntityExpr("partyId", EntityOperator.EQUALS, userLogin.get("partyId")));
                constraints.add(new EntityExpr("workEffortTypeId", EntityOperator.EQUALS, "ACTIVITY"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"));
                constraints.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));
                constraints.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "WF_COMPLETED"));
                constraints.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "WF_TERMINATED"));
                constraints.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "WF_ABORTED"));
                groupWorkEfforts = delegator.findByAnd("WorkEffortPartyAssignByGroup", constraints, UtilMisc.toList("priority"));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError("Error finding desired WorkEffort records: " + e.toString());
            }
        }

        Map result = new HashMap();
        if (groupWorkEfforts == null) groupWorkEfforts = new LinkedList();
        result.put("groupActivities", groupWorkEfforts);
        return result;
    }
    
    public static Map getWorkEffort(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");    
        Security security = ctx.getSecurity();
        Map resultMap = new HashMap();
        
        String workEffortId = (String) context.get("workEffortId");    
        GenericValue workEffort = null;
        
        try {
            workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        
        Boolean canView = null;
        Collection workEffortPartyAssignments = null;
        Boolean tryEntity = null;
        GenericValue currentStatus = null;
        
        if (workEffort == null) {
            tryEntity = Boolean.FALSE;
            canView = Boolean.TRUE;
        
            String statusId = (String) context.get("currentStatusId");
        
            if (statusId != null && statusId.length() > 0) {
                try {
                    currentStatus = delegator.findByPrimaryKeyCache("StatusItem", UtilMisc.toMap("statusId", statusId));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        } else {
            // get a collection of workEffortPartyAssignments, if empty then this user CANNOT view the event, unless they have permission to view all
            if (userLogin != null && userLogin.get("partyId") != null && workEffortId != null) {
                try {
                    workEffortPartyAssignments = delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("workEffortId", workEffortId, "partyId", userLogin.get("partyId")));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
            canView = (workEffortPartyAssignments != null && workEffortPartyAssignments.size() > 0) ? Boolean.TRUE : Boolean.FALSE;
            if (!canView.booleanValue() && security.hasEntityPermission("WORKEFFORTMGR", "_VIEW", userLogin)) {
                canView = Boolean.TRUE;
            }
        
            tryEntity = Boolean.TRUE;
        
            if (workEffort.get("currentStatusId") != null) {
                try {
                    currentStatus = delegator.findByPrimaryKeyCache("StatusItem", UtilMisc.toMap("statusId", workEffort.get("currentStatusId")));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        }
        
        if (workEffortId != null) resultMap.put("workEffortId", workEffortId);
        if (workEffort != null) resultMap.put("workEffort", workEffort);
        if (canView != null) resultMap.put("canView", canView);
        if (workEffortPartyAssignments != null) resultMap.put("partyAssigns", workEffortPartyAssignments);
        if (tryEntity != null) resultMap.put("tryEntity", tryEntity);
        if (currentStatus != null) resultMap.put("currentStatusItem", currentStatus);
        return resultMap;
    } 
        
    private static List getWorkEffortEvents(DispatchContext ctx, Timestamp startStamp, Timestamp endStamp, String partyId, String facilityId, String fixedAssetId) {
        Set partyIds = new HashSet();
        partyIds.add(partyId);
        return getWorkEffortEvents(ctx, startStamp, endStamp, partyIds, facilityId, fixedAssetId);
    }

    private static List getWorkEffortEvents(DispatchContext ctx, Timestamp startStamp, Timestamp endStamp, Collection partyIds, String facilityId, String fixedAssetId) {
        GenericDelegator delegator = ctx.getDelegator();
        List validWorkEfforts = new ArrayList();
        try {
            List entityExprList = UtilMisc.toList(
                    new EntityExpr("estimatedCompletionDate", EntityOperator.GREATER_THAN_EQUAL_TO, startStamp),
                    new EntityExpr("estimatedStartDate", EntityOperator.LESS_THAN, endStamp));
            // Filter out all the canceled work efforts
            entityExprList.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));
            entityExprList.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CANCELLED"));
            
            List typesList = UtilMisc.toList(new EntityExpr("workEffortTypeId", EntityOperator.EQUALS, "EVENT"));
            if (partyIds != null && partyIds.size() > 0) {
                entityExprList.add(new EntityExpr("partyId", EntityOperator.IN, partyIds));
            }
            if (UtilValidate.isNotEmpty(facilityId)) {
                entityExprList.add(new EntityExpr("facilityId", EntityOperator.EQUALS, facilityId));
                typesList.add(new EntityExpr("workEffortTypeId", EntityOperator.EQUALS, "PROD_ORDER_HEADER"));
                entityExprList.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CREATED"));
                entityExprList.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_COMPLETED"));
                entityExprList.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CLOSED"));
            }
            if (UtilValidate.isNotEmpty(fixedAssetId)) {
                entityExprList.add(new EntityExpr("fixedAssetId", EntityOperator.EQUALS, fixedAssetId));
                typesList.add(new EntityExpr("workEffortTypeId", EntityOperator.EQUALS, "PROD_ORDER_TASK"));
                entityExprList.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CREATED"));
                entityExprList.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_COMPLETED"));
                entityExprList.add(new EntityExpr("currentStatusId", EntityOperator.NOT_EQUAL, "PRUN_CLOSED"));
            }
            EntityCondition typesCondition = null;
            if (typesList.size() == 1) {
                typesCondition = (EntityExpr)typesList.get(0);
            } else {
                typesCondition = new EntityConditionList(typesList, EntityJoinOperator.OR);
            }
            entityExprList.add(typesCondition);

            List tempWorkEfforts = null;
            if (partyIds != null && partyIds.size() > 0) {
                tempWorkEfforts = delegator.findByAnd("WorkEffortAndPartyAssign", entityExprList, UtilMisc.toList("estimatedStartDate"));
            } else {
                tempWorkEfforts = delegator.findByAnd("WorkEffort", entityExprList, UtilMisc.toList("estimatedStartDate"));
            }

            // This block needs to be here to filter duplicate workeffort ids when 
            // more than one of the selected party ids is assigned to the WorkEffort
            
            Set tempWeKeys = new HashSet();
            Iterator tempWorkEffortIter = tempWorkEfforts.iterator();
            while (tempWorkEffortIter.hasNext()) {
                GenericValue tempWorkEffort = (GenericValue) tempWorkEffortIter.next();
                String tempWorkEffortId = tempWorkEffort.getString("workEffortId");
                if (tempWeKeys.contains(tempWorkEffortId)) {
                    tempWorkEffortIter.remove();
                } else {
                    tempWeKeys.add(tempWorkEffortId);
                }
            }
            
            validWorkEfforts = new ArrayList(tempWorkEfforts);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return validWorkEfforts;        
    }

    public static Map getWorkEffortEventsByPeriod(DispatchContext ctx, Map context) {
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");    
        Locale locale = (Locale) context.get("locale");
        
        Timestamp startDay = (Timestamp) context.get("start");
        Integer numPeriodsInteger = (Integer) context.get("numPeriods");

        String partyId = (String) context.get("partyId");
        Collection partyIds = (Collection) context.get("partyIds");
        String facilityId = (String) context.get("facilityId");
        String fixedAssetId = (String) context.get("fixedAssetId");
        Boolean filterOutCanceledEvents = (Boolean) context.get("filterOutCanceledEvents");
        if (filterOutCanceledEvents == null) {
        	filterOutCanceledEvents = Boolean.FALSE;
        }
        
        //To be returned, the max concurrent entries for a single period
        int maxConcurrentEntries = 0;
                
        TimeZone curTz = TimeZone.getDefault();

        Integer periodTypeObject = (Integer) context.get("periodType");
        int periodType = 0;
        if (periodTypeObject != null) {
            periodType = periodTypeObject.intValue();
        }
        
        int numPeriods = 0;
        if(numPeriodsInteger != null) numPeriods = numPeriodsInteger.intValue();
        
        // get a timestamp (date) for the beginning of today and for beginning of numDays+1 days from now
        Timestamp startStamp = UtilDateTime.getDayStart(startDay, curTz, locale);
        Timestamp endStamp = UtilDateTime.adjustTimestamp(startStamp, periodType, 1, curTz, locale);
        long periodLen = endStamp.getTime() - startStamp.getTime();
        endStamp = UtilDateTime.adjustTimestamp(startStamp, periodType, numPeriods, curTz, locale);
        
        // Get the WorkEfforts
        List validWorkEfforts = null;
        Collection partyIdsToUse = partyIds;
        if (partyIdsToUse == null) partyIdsToUse = new HashSet();
        
        if (UtilValidate.isNotEmpty(partyId)) {
            if (partyId.equals(userLogin.getString("partyId")) || security.hasEntityPermission("WORKEFFORTMGR", "_VIEW", userLogin)) {
                partyIdsToUse.add(partyId);
            } else {
                return ServiceUtil.returnError("You do not have permission to view information for party with ID [" + partyId + "], you must be logged in as a user associated with this party, or have the WORKEFFORTMGR_VIEW or WORKEFFORTMGR_ADMIN permissions.");
            }
        } else {
            // if a facilityId or a fixedAssetId are not specified, don't set a default partyId...
            if (UtilValidate.isEmpty(facilityId) && UtilValidate.isEmpty(fixedAssetId)) {
                partyIdsToUse.add(userLogin.getString("partyId"));
            }
        }
                
        // Use the View Entity
        if (partyIdsToUse.size() > 0 || UtilValidate.isNotEmpty(facilityId) || UtilValidate.isNotEmpty(fixedAssetId)) {
            validWorkEfforts = getWorkEffortEvents(ctx, startStamp, endStamp, partyIdsToUse, facilityId, fixedAssetId);
        }
        if (filterOutCanceledEvents.booleanValue()) {
        	validWorkEfforts = EntityUtil.filterOutByCondition(validWorkEfforts, new EntityExpr("currentStatusId", EntityOperator.EQUALS, "EVENT_CANCELLED"));
        }
        
        // Split the WorkEffort list into a map with entries for each period, period start is the key
        List periods = new ArrayList();
        if (validWorkEfforts != null) {
        
            // For each day in the set we check all work efforts to see if they fall within range
            for (int i = 0; i < numPeriods; i++) {
                Timestamp curPeriodStart = UtilDateTime.adjustTimestamp(startStamp, periodType, i, curTz, locale);
                Timestamp curPeriodEnd = UtilDateTime.adjustTimestamp(curPeriodStart, periodType, 1, curTz, locale);
                List curWorkEfforts = new ArrayList();
                Map entry = new HashMap();
                for (int j = 0; j < validWorkEfforts.size(); j++) {
                    
                    GenericValue workEffort = (GenericValue) validWorkEfforts.get(j);
                    // Debug.log("Got workEffort: " + workEffort.toString(), module);
            
                    Timestamp estimatedStartDate = workEffort.getTimestamp("estimatedStartDate");
                    Timestamp estimatedCompletionDate = workEffort.getTimestamp("estimatedCompletionDate");
            
                    if (estimatedStartDate == null || estimatedCompletionDate == null) continue;
                    
                    if (estimatedStartDate.compareTo(curPeriodEnd) < 0 && estimatedCompletionDate.compareTo(curPeriodStart) > 0) {
                        //Debug.logInfo("Task start: "+estimatedStartDate+" Task end: "+estimatedCompletionDate+" Period start: "+curPeriodStart+" Period end: "+curPeriodEnd, module);
                       
                        Map calEntry = new HashMap();
                        calEntry.put("workEffort",workEffort);
                                               
                        long length = ((estimatedCompletionDate.after(endStamp) ? endStamp.getTime() : estimatedCompletionDate.getTime()) - (estimatedStartDate.before(startStamp) ? startStamp.getTime() : estimatedStartDate.getTime()));
                        int periodSpan = (int) Math.ceil((double) length / periodLen);                                                
                        calEntry.put("periodSpan", new Integer(periodSpan));

                        if(i == 0) calEntry.put("startOfPeriod", Boolean.TRUE); //If this is the first priod any valid entry is starting here
                        else {
                            boolean startOfPeriod = ((estimatedStartDate.getTime() - curPeriodStart.getTime()) >= 0);                            
                            calEntry.put("startOfPeriod", new Boolean(startOfPeriod));
                        }
                        curWorkEfforts.add(calEntry);
                    }
        
                    // if startDate is after hourEnd, continue to the next day, we haven't gotten to this one yet...
                    if (estimatedStartDate.after(curPeriodEnd)) break;
                    
                    // if completionDate is before the hourEnd, remove from list, we are done with it
                    if (estimatedCompletionDate.before(curPeriodEnd)) {
                        validWorkEfforts.remove(j);
                        j--;
                    }
                }
                //For calendar we want to include empty periods aswell
                //if (curWorkEfforts.size() > 0)  
                int numEntries = curWorkEfforts.size();
                if(numEntries > maxConcurrentEntries) maxConcurrentEntries = numEntries;
                entry.put("start",curPeriodStart);
                entry.put("end",curPeriodEnd);                
                entry.put("calendarEntries",curWorkEfforts);
                periods.add(entry);
            }
        }
        Map result = new HashMap();
        result.put("periods", periods);
        result.put("maxConcurrentEntries", new Integer(maxConcurrentEntries));
        return result;
    }
    
    public static Map getProductManufacturingSummaryByFacility(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        String productId = (String) context.get("productId");
        String facilityId = (String) context.get("facilityId"); // optional
        
        Map summaryInByFacility = new HashMap();
        Map summaryOutByFacility = new HashMap();
        try {
            //
            // Information about the running production runs that are going
            // to produce units of productId by facility.
            //
            List findIncomingProductionRunsConds = new LinkedList();

            findIncomingProductionRunsConds.add(new EntityExpr("productId", EntityOperator.EQUALS, productId));
            findIncomingProductionRunsConds.add(new EntityExpr("statusId", EntityOperator.EQUALS, "WEGS_CREATED"));
            findIncomingProductionRunsConds.add(new EntityExpr("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUN_PROD_DELIV"));
            if (facilityId != null) {
                findIncomingProductionRunsConds.add(new EntityExpr("facilityId", EntityOperator.EQUALS, facilityId));
            }

            List findIncomingProductionRunsStatusConds = new LinkedList();
            findIncomingProductionRunsStatusConds.add(new EntityExpr("currentStatusId", EntityOperator.EQUALS, "PRUN_CREATED"));
            findIncomingProductionRunsStatusConds.add(new EntityExpr("currentStatusId", EntityOperator.EQUALS, "PRUN_SCHEDULED"));
            findIncomingProductionRunsStatusConds.add(new EntityExpr("currentStatusId", EntityOperator.EQUALS, "PRUN_DOC_PRINTED"));
            findIncomingProductionRunsStatusConds.add(new EntityExpr("currentStatusId", EntityOperator.EQUALS, "PRUN_RUNNING"));
            findIncomingProductionRunsStatusConds.add(new EntityExpr("currentStatusId", EntityOperator.EQUALS, "PRUN_COMPLETED"));
            findIncomingProductionRunsConds.add(new EntityConditionList(findIncomingProductionRunsStatusConds, EntityOperator.OR));

            EntityConditionList findIncomingProductionRunsCondition = new EntityConditionList(findIncomingProductionRunsConds, EntityOperator.AND);

            List incomingProductionRuns = delegator.findByCondition("WorkEffortAndGoods", findIncomingProductionRunsCondition, null, UtilMisc.toList("-estimatedCompletionDate"));
            Iterator incomingProductionRunsIter = incomingProductionRuns.iterator();
            while (incomingProductionRunsIter.hasNext()) {
                GenericValue incomingProductionRun = (GenericValue)incomingProductionRunsIter.next();

                double producedQtyTot = 0.0;
                if (incomingProductionRun.getString("currentStatusId").equals("PRUN_COMPLETED")) {
                    List inventoryItems = delegator.findByAnd("WorkEffortAndInventoryProduced", UtilMisc.toMap("productId", productId, "workEffortId", incomingProductionRun.getString("workEffortId")));
                    Iterator inventoryItemsIter = inventoryItems.iterator();
                    while (inventoryItemsIter.hasNext()) {
                        GenericValue inventoryItem = (GenericValue)inventoryItemsIter.next();
                        GenericValue inventoryItemDetail = EntityUtil.getFirst(delegator.findByAnd("InventoryItemDetail", UtilMisc.toMap("inventoryItemId", inventoryItem.getString("inventoryItemId")), UtilMisc.toList("inventoryItemDetailSeqId")));
                        if (inventoryItemDetail != null && inventoryItemDetail.get("quantityOnHandDiff") != null) {
                            Double inventoryItemQty = inventoryItemDetail.getDouble("quantityOnHandDiff");
                            producedQtyTot = producedQtyTot + inventoryItemQty.doubleValue();
                        }
                    }
                }
                double estimatedQuantity = 0.0;
                if (incomingProductionRun.get("estimatedQuantity") != null) {
                    estimatedQuantity = incomingProductionRun.getDouble("estimatedQuantity").doubleValue();
                }
                double remainingQuantity = estimatedQuantity - producedQtyTot; // the qty that still needs to be produced
                if (remainingQuantity > 0) {
                    incomingProductionRun.set("estimatedQuantity", new Double(remainingQuantity));
                } else {
                    continue;
                }
                String weFacilityId = incomingProductionRun.getString("facilityId");

                Map quantitySummary = (Map)summaryInByFacility.get(weFacilityId);
                if (quantitySummary == null) {
                    quantitySummary = new HashMap();
                    quantitySummary.put("facilityId", weFacilityId);
                    summaryInByFacility.put(weFacilityId, quantitySummary);
                }
                Double remainingQuantityTot = (Double)quantitySummary.get("estimatedQuantityTotal");
                if (remainingQuantityTot == null) {
                    quantitySummary.put("estimatedQuantityTotal", new Double(remainingQuantity));
                } else {
                    quantitySummary.put("estimatedQuantityTotal", new Double(remainingQuantity + remainingQuantityTot.doubleValue()));
                }

                List incomingProductionRunList = (List)quantitySummary.get("incomingProductionRunList");
                if (incomingProductionRunList == null) {
                    incomingProductionRunList = new LinkedList();
                    quantitySummary.put("incomingProductionRunList", incomingProductionRunList);
                }
                incomingProductionRunList.add(incomingProductionRun);
            }
            //
            // Information about the running production runs that are going
            // to consume units of productId by facility.
            //
            List findOutgoingProductionRunsConds = new LinkedList();

            findOutgoingProductionRunsConds.add(new EntityExpr("productId", EntityOperator.EQUALS, productId));
            findOutgoingProductionRunsConds.add(new EntityExpr("statusId", EntityOperator.EQUALS, "WEGS_CREATED"));
            findOutgoingProductionRunsConds.add(new EntityExpr("workEffortGoodStdTypeId", EntityOperator.EQUALS, "PRUNT_PROD_NEEDED"));
            if (facilityId != null) {
                findOutgoingProductionRunsConds.add(new EntityExpr("facilityId", EntityOperator.EQUALS, facilityId));
            }

            List findOutgoingProductionRunsStatusConds = new LinkedList();
            findOutgoingProductionRunsStatusConds.add(new EntityExpr("currentStatusId", EntityOperator.EQUALS, "PRUN_CREATED"));
            findOutgoingProductionRunsStatusConds.add(new EntityExpr("currentStatusId", EntityOperator.EQUALS, "PRUN_SCHEDULED"));
            findOutgoingProductionRunsStatusConds.add(new EntityExpr("currentStatusId", EntityOperator.EQUALS, "PRUN_DOC_PRINTED"));
            findOutgoingProductionRunsStatusConds.add(new EntityExpr("currentStatusId", EntityOperator.EQUALS, "PRUN_RUNNING"));
            findOutgoingProductionRunsConds.add(new EntityConditionList(findOutgoingProductionRunsStatusConds, EntityOperator.OR));

            EntityConditionList findOutgoingProductionRunsCondition = new EntityConditionList(findOutgoingProductionRunsConds, EntityOperator.AND);
            List outgoingProductionRuns = delegator.findByCondition("WorkEffortAndGoods", findOutgoingProductionRunsCondition, null, UtilMisc.toList("-estimatedStartDate"));
            Iterator outgoingProductionRunsIter = outgoingProductionRuns.iterator();
            while (outgoingProductionRunsIter.hasNext()) {
                GenericValue outgoingProductionRun = (GenericValue)outgoingProductionRunsIter.next();

                String weFacilityId = outgoingProductionRun.getString("facilityId");
                Double neededQuantity = outgoingProductionRun.getDouble("estimatedQuantity");
                if (neededQuantity == null) {
                    neededQuantity = new Double(0);
                }

                Map quantitySummary = (Map)summaryOutByFacility.get(weFacilityId);
                if (quantitySummary == null) {
                    quantitySummary = new HashMap();
                    quantitySummary.put("facilityId", weFacilityId);
                    summaryOutByFacility.put(weFacilityId, quantitySummary);
                }
                Double remainingQuantityTot = (Double)quantitySummary.get("estimatedQuantityTotal");
                if (remainingQuantityTot == null) {
                    quantitySummary.put("estimatedQuantityTotal", neededQuantity);
                } else {
                    quantitySummary.put("estimatedQuantityTotal", new Double(neededQuantity.doubleValue() + remainingQuantityTot.doubleValue()));
                }

                List outgoingProductionRunList = (List)quantitySummary.get("outgoingProductionRunList");
                if (outgoingProductionRunList == null) {
                    outgoingProductionRunList = new LinkedList();
                    quantitySummary.put("outgoingProductionRunList", outgoingProductionRunList);
                }
                outgoingProductionRunList.add(outgoingProductionRun);
            }

        } catch(GenericEntityException gee) {
            return ServiceUtil.returnError("Error retrieving manufacturing data for productId [" + productId + "]: " + gee.getMessage());
        }
        Map resultMap = ServiceUtil.returnSuccess();
        resultMap.put("summaryInByFacility", summaryInByFacility);
        resultMap.put("summaryOutByFacility", summaryOutByFacility);
        return resultMap;
    }
}
