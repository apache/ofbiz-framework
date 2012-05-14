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
package org.ofbiz.common.status;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;

import static org.ofbiz.base.util.UtilGenerics.checkList;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * StatusServices
 */
public class StatusServices {

    public static final String module = StatusServices.class.getName();
    public static final String resource = "CommonUiLabels";

    public static Map<String, Object> getStatusItems(DispatchContext ctx, Map<String, ?> context) {
        Delegator delegator = ctx.getDelegator();
        List<String> statusTypes = checkList(context.get("statusTypeIds"), String.class);
        Locale locale = (Locale) context.get("locale");
        if (UtilValidate.isEmpty(statusTypes)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "CommonStatusMandatory", locale));
        }

        List<GenericValue> statusItems = FastList.newInstance();
        for (String statusTypeId: statusTypes) {
            try {
                List<GenericValue> myStatusItems = delegator.findByAnd("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeId), UtilMisc.toList("sequenceId"), true);
                statusItems.addAll(myStatusItems);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        Map<String, Object> ret = FastMap.newInstance();
        ret.put("statusItems",statusItems);
        return ret;
    }

    public static Map<String, Object> getStatusValidChangeToDetails(DispatchContext ctx, Map<String, ?> context) {
        Delegator delegator = ctx.getDelegator();
        List<GenericValue> statusValidChangeToDetails = null;
        String statusId = (String) context.get("statusId");
        try {
            statusValidChangeToDetails = delegator.findByAnd("StatusValidChangeToDetail", UtilMisc.toMap("statusId", statusId), UtilMisc.toList("sequenceId"), true);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        Map<String, Object> ret = ServiceUtil.returnSuccess();
        if (statusValidChangeToDetails != null) {
            ret.put("statusValidChangeToDetails", statusValidChangeToDetails);
        }
        return ret;
    }
}
