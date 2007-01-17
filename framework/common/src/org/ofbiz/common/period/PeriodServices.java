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

package org.ofbiz.common.period;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

public class PeriodServices {
    public static String module = PeriodServices.class.getName();
    
    /* find the date of the last closed CustomTimePeriod, or, if none available, the earliest date available of any
     * CustomTimePeriod
     */
    public static Map findLastClosedDate(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String organizationPartyId = (String) context.get("organizationPartyId"); // input parameters
        String periodTypeId = (String) context.get("periodTypeId");
        Timestamp findDate = (Timestamp) context.get("findDate");
        
        // default findDate to now
        if (findDate == null) {
            findDate = UtilDateTime.nowTimestamp();
        }
        
        Timestamp lastClosedDate = null;          // return parameters
        GenericValue lastClosedTimePeriod = null;
        Map result = ServiceUtil.returnSuccess();
        
        try {
            // try to get the ending date of the most recent accounting time period before findDate which has been closed
            List findClosedConditions = UtilMisc.toList(new EntityExpr("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                    new EntityExpr("thruDate", EntityOperator.LESS_THAN_EQUAL_TO, findDate),
                    new EntityExpr("isClosed", EntityOperator.EQUALS, "Y"));
            if ((periodTypeId != null) && !(periodTypeId.equals(""))) {
                // if a periodTypeId was supplied, use it
                findClosedConditions.add(new EntityExpr("periodTypeId", EntityOperator.EQUALS, periodTypeId));
            }
            List closedTimePeriods = delegator.findByCondition("CustomTimePeriod", new EntityConditionList(findClosedConditions, EntityOperator.AND), 
                    UtilMisc.toList("customTimePeriodId", "periodTypeId", "isClosed", "fromDate", "thruDate"), 
                    UtilMisc.toList("thruDate DESC"));

            if ((closedTimePeriods != null) && (closedTimePeriods.size() > 0) && (((GenericValue) closedTimePeriods.get(0)).get("thruDate") != null)) {
                lastClosedTimePeriod = (GenericValue) closedTimePeriods.get(0);
                lastClosedDate = UtilDateTime.toTimestamp(lastClosedTimePeriod.getDate("thruDate"));
            } else {
                // uh oh, no time periods have been closed?  in that case, just find the earliest beginning of a time period for this organization
                // and optionally, for this period type
                Map findParams = UtilMisc.toMap("organizationPartyId", organizationPartyId);
                if ((periodTypeId != null) && !(periodTypeId.equals(""))) {
                    findParams.put("periodTypeId", periodTypeId);
                }
                List timePeriods = delegator.findByAnd("CustomTimePeriod", findParams, UtilMisc.toList("fromDate ASC")); 
                if ((timePeriods != null) && (timePeriods.size() > 0) && (((GenericValue) timePeriods.get(0)).get("fromDate") != null)) {
                    lastClosedDate = UtilDateTime.toTimestamp(((GenericValue) timePeriods.get(0)).getDate("fromDate"));
                } else {
                    return ServiceUtil.returnError("Cannot get a starting date for net income");
                }
            }
            
            result.put("lastClosedTimePeriod", lastClosedTimePeriod);  // ok if this is null - no time periods have been closed
            result.put("lastClosedDate", lastClosedDate);  // should have a value - not null
            return result;
        } catch (GenericEntityException ex) {
            return(ServiceUtil.returnError(ex.getMessage()));
        }
    }
}
