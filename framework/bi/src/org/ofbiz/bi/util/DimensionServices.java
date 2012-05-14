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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

public class DimensionServices {

    public static final String module = DimensionServices.class.getName();
    public static final String resource = "BiUiLabels";

    public static Map<String, Object> getDimensionIdFromNaturalKey(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        String dimensionEntityName = (String) context.get("dimensionEntityName");
        Map<String, ? extends Object> naturalKeyFields = UtilGenerics.cast(context.get("naturalKeyFields"));
        GenericValue lastDimensionValue = null;
        try {
            // TODO: improve performance
            lastDimensionValue = EntityUtil.getFirst(delegator.findByAnd(dimensionEntityName, UtilMisc.toMap(naturalKeyFields), UtilMisc.toList("-createdTxStamp"), false));
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(gee.getMessage());
        }
        if (UtilValidate.isNotEmpty(lastDimensionValue)) {
            resultMap.put("dimensionId", lastDimensionValue.getString("dimensionId"));
        }
        return resultMap;
    }

    public static Map<String, Object> storeGenericDimension(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        GenericValue dimensionValue = (GenericValue) context.get("dimensionValue");
        List<String> naturalKeyFields = UtilGenerics.checkList(context.get("naturalKeyFields"), String.class);
        String updateMode = (String) context.get("updateMode");
        Locale locale = (Locale) context.get("locale");

        try {
            Map<String, Object> andCondition = FastMap.newInstance();
            for (String naturalKeyField: naturalKeyFields) {
                andCondition.put(naturalKeyField, dimensionValue.get(naturalKeyField));
            }
            if (andCondition.isEmpty()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "BusinessIntelligenceNaturalKeyWithourDimension", UtilMisc.toMap("naturalKeyFields", naturalKeyFields, "dimensionValue", dimensionValue), locale));
            }
            List<GenericValue> existingDimensionValues = null;
            try {
                existingDimensionValues = delegator.findByAnd(dimensionValue.getEntityName(), UtilMisc.toMap(andCondition), null, false);
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(gee.getMessage());
            }
            if (UtilValidate.isEmpty(existingDimensionValues)) {
                dimensionValue.set("dimensionId", delegator.getNextSeqId(dimensionValue.getEntityName()));
                dimensionValue.create();
            } else {
                if ("TYPE1".equals(updateMode)) {
                    // update all the rows with the new values
                    for (GenericValue existingDimensionValue: existingDimensionValues) {
                        GenericValue updatedValue = delegator.makeValue(dimensionValue.getEntityName(), dimensionValue);
                        updatedValue.set("dimensionId", existingDimensionValue.getString("dimensionId"));
                        updatedValue.store();
                    }
                } else if ("TYPE2".equals(updateMode)) {
                    // TODO: create a new record and update somewhere the from/thru dates of the old row
                    dimensionValue.set("dimensionId", delegator.getNextSeqId(dimensionValue.getEntityName()));
                    dimensionValue.create();
                } else {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "BusinessIntelligenceUpdateModeStillNotSupported", UtilMisc.toMap("updateMode", updateMode), locale));
                }
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(gee.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

}
