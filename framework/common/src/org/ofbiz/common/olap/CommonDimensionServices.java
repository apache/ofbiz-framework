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
package org.ofbiz.common.olap;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.transaction.xa.XAException;

import javolution.util.FastMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import static org.ofbiz.base.util.UtilGenerics.checkList;
import static org.ofbiz.base.util.UtilGenerics.checkMap;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.ServiceXaWrapper;
import org.ofbiz.service.mail.MimeMessageWrapper;

/**
 * Common Services
 */
public class CommonDimensionServices {

    public final static String module = CommonDimensionServices.class.getName();

    /*
     * OLAP Dimension
     * Service used to initialize the Date dimension (DateDimension).
     * The DateDimension entity is a nearly constant dimension ("Slowly Changing Dimension" or SCD):
     * the default strategy to handle data change is "Type 1" (i.e. overwrite the values).
     */
    public static Map loadDateDimension(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
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
        java.sql.Date currentDate = new java.sql.Date(calendar.getTimeInMillis());
        while (currentDate.compareTo(thruDate) <= 0) {
            GenericValue dateValue = null;
            try {
                dateValue = EntityUtil.getFirst(delegator.findByAnd("DateDimension", UtilMisc.toMap("dateValue", currentDate)));
            } catch (GenericEntityException gee) {
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
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(gee.getMessage());
            }
            calendar.add(Calendar.DATE, 1);
            currentDate = new java.sql.Date(calendar.getTimeInMillis());
        }
        return ServiceUtil.returnSuccess();
    }

}
