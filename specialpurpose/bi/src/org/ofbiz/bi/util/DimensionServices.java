/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.ofbiz.bi.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.util.EntityUtil;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class DimensionServices {
    
    public static final String module = DimensionServices.class.getName();

    public static Map getDimensionIdFromNaturalKey(DispatchContext ctx, Map context) {
        Map resultMap = ServiceUtil.returnSuccess();
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();

        String dimensionEntityName = (String) context.get("dimensionEntityName");
        Map naturalKeyFields = (Map) context.get("naturalKeyFields");
        GenericValue lastDimensionValue = null;
        try {
            // TODO: improve performance
            lastDimensionValue = EntityUtil.getFirst(delegator.findByAnd(dimensionEntityName, naturalKeyFields, UtilMisc.toList("-createdTxStamp")));
        } catch(GenericEntityException gee) {
            return ServiceUtil.returnError(gee.getMessage());
        }
        if (UtilValidate.isNotEmpty(lastDimensionValue)) {
            resultMap.put("dimensionId", lastDimensionValue.getString("dimensionId"));
        }
        return resultMap;
    }

    public static Map storeGenericDimension(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();

        GenericValue dimensionValue = (GenericValue) context.get("dimensionValue");
        List naturalKeyFields = (List) context.get("naturalKeyFields");
        String updateMode = (String) context.get("updateMode");

        try {
            Map andCondition = FastMap.newInstance();
            for (int i = 0; i < naturalKeyFields.size(); i++) {
                String naturalKeyField = (String)naturalKeyFields.get(i);
                andCondition.put(naturalKeyField, dimensionValue.get(naturalKeyField));
            }
            if (andCondition.isEmpty()) {
                return ServiceUtil.returnError("The natural key: " + naturalKeyFields + " is empty in value: " + dimensionValue);
            }
            List existingDimensionValues = null;
            try {
                existingDimensionValues = delegator.findByAnd(dimensionValue.getEntityName(), andCondition);
            } catch(GenericEntityException gee) {
                return ServiceUtil.returnError(gee.getMessage());
            }
            if (UtilValidate.isEmpty(existingDimensionValues)) {
                dimensionValue.set("dimensionId", delegator.getNextSeqId(dimensionValue.getEntityName()));
                dimensionValue.create();
            } else {
                if ("TYPE1".equals(updateMode)) {
                    // update all the rows with the new values
                    for (int i = 0; i < existingDimensionValues.size(); i++) {
                        GenericValue existingDimensionValue = (GenericValue)existingDimensionValues.get(i);
                        GenericValue updatedValue = delegator.makeValue(dimensionValue.getEntityName(), dimensionValue);
                        updatedValue.set("dimensionId", existingDimensionValue.getString("dimensionId"));
                        updatedValue.store();
                    }
                } else if ("TYPE2".equals(updateMode)) {
                    // TODO: create a new record and update somewhere the from/thru dates of the old row
                    dimensionValue.set("dimensionId", delegator.getNextSeqId(dimensionValue.getEntityName()));
                    dimensionValue.create();
                } else {
                    return ServiceUtil.returnError("The update mode: " + updateMode + " is still not supported.");
                }
            }
        } catch(GenericEntityException gee) {
            return ServiceUtil.returnError(gee.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /*
     * Service used to initialize the Date dimension (DateDimension).
     * The DateDimension entity is a nearly constant dimension ("Slowly Changing Dimension" or SCD):
     * the default strategy to handle data change is "Type 1" (i.e. overwrite the values).
     */
    public static Map loadDateDimension(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();

        Date fromDate = (Date) context.get("fromDate");
        Date thruDate = (Date) context.get("thruDate");

        SimpleDateFormat monthNameFormat = new SimpleDateFormat("MMMM");
        SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEEE");
        SimpleDateFormat dayDescriptionFormat = new SimpleDateFormat("MMMM d, yyyy");
        SimpleDateFormat yearMonthDayFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat yearMonthFormat = new SimpleDateFormat("yyyy-MM");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fromDate);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date currentDate = calendar.getTime();
        while (currentDate.compareTo(thruDate) <= 0) {
            GenericValue dateValue = null;
            try {
                dateValue = EntityUtil.getFirst(delegator.findByAnd("DateDimension", UtilMisc.toMap("dateValue", currentDate)));
            } catch(GenericEntityException gee) {
                return ServiceUtil.returnError(gee.getMessage());
            }
            boolean newValue = (dateValue == null);
            if (newValue) {
                dateValue = delegator.makeValue("DateDimension");
                dateValue.set("dimensionId", delegator.getNextSeqId("DateDimension"));
                dateValue.set("dateValue", new java.sql.Date(currentDate.getTime()));
            }
            dateValue.set("description", dayDescriptionFormat.format(currentDate));
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            dateValue.set("dayName", dayNameFormat.format(currentDate));
            dateValue.set("dayOfMonth", new Long(calendar.get(Calendar.DAY_OF_MONTH)));
            dateValue.set("dayOfYear", new Long(calendar.get(Calendar.DAY_OF_YEAR)));
            dateValue.set("monthName", monthNameFormat.format(currentDate));
            
            dateValue.set("monthOfYear", new Long(calendar.get(Calendar.MONTH) + 1));
            dateValue.set("yearName", new Long(calendar.get(Calendar.YEAR)));
            dateValue.set("weekOfMonth", new Long(calendar.get(Calendar.WEEK_OF_MONTH)));
            dateValue.set("weekOfYear", new Long(calendar.get(Calendar.WEEK_OF_YEAR)));
            dateValue.set("weekdayType", (dayOfWeek == 1 || dayOfWeek == 7? "Weekend": "Weekday"));
            dateValue.set("yearMonthDay", yearMonthDayFormat.format(currentDate));
            dateValue.set("yearAndMonth", yearMonthFormat.format(currentDate));
            
            try {
                if (newValue) {
                    dateValue.create();
                } else {
                    dateValue.store();
                }
            } catch(GenericEntityException gee) {
                return ServiceUtil.returnError(gee.getMessage());
            }
            calendar.add(Calendar.DATE, 1);
            currentDate = calendar.getTime();
        }
        return ServiceUtil.returnSuccess();
    }

}
