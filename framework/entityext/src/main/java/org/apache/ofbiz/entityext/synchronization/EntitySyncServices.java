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
 *******************************************************************************/
package org.apache.ofbiz.entityext.synchronization;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

/**
 * Entity Engine Sync Services
 */
public class EntitySyncServices {

    private static final String MODULE = EntitySyncServices.class.getName();
    private static final String RESOURCE = "EntityExtUiLabels";

    /**
     * Clean EntitySyncRemove Info
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> cleanSyncRemoveInfo(DispatchContext dctx, Map<String, ? extends Object> context) {
        Debug.logInfo("Running cleanSyncRemoveInfo", MODULE);
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        try {
            // find the largest keepRemoveInfoHours value on an EntitySyncRemove and kill everything before that,
            // if none found default to 10 days (240 hours)
            double keepRemoveInfoHours = 24;

            List<GenericValue> entitySyncRemoveList = EntityQuery.use(delegator).from("EntitySync").queryList();
            for (GenericValue entitySyncRemove: entitySyncRemoveList) {
                Double curKrih = entitySyncRemove.getDouble("keepRemoveInfoHours");
                if (curKrih != null) {
                    double curKrihVal = curKrih;
                    if (curKrihVal > keepRemoveInfoHours) {
                        keepRemoveInfoHours = curKrihVal;
                    }
                }
            }


            int keepSeconds = (int) Math.floor(keepRemoveInfoHours * 3600);

            Calendar nowCal = Calendar.getInstance();
            nowCal.setTimeInMillis(System.currentTimeMillis());
            nowCal.add(Calendar.SECOND, -keepSeconds);
            Timestamp keepAfterStamp = new Timestamp(nowCal.getTimeInMillis());

            int numRemoved = delegator.removeByCondition("EntitySyncRemove", EntityCondition.makeCondition(ModelEntity.STAMP_TX_FIELD,
                    EntityOperator.LESS_THAN, keepAfterStamp));
            Debug.logInfo("In cleanSyncRemoveInfo removed [" + numRemoved + "] values with TX timestamp before [" + keepAfterStamp + "]", MODULE);

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error cleaning out EntitySyncRemove info: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "EntityExtErrorCleaningEntitySyncRemove",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }
    }
}
