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
package org.apache.ofbiz.common.telecom;

import static org.apache.ofbiz.base.util.UtilGenerics.checkList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

public class TelecomServices {

    public final static String module = TelecomServices.class.getName();

    public static Map<String, Object> sendTelecomMessage(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Map<String, Object> results = ServiceUtil.returnSuccess();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String telecomMsgTypeEnumId = (String) context.get("telecomMsgTypeEnumId");
        String telecomMethodTypeId = (String) context.get("telecomMethodTypeId");
        String telecomGatewayConfigId = (String) context.get("telecomGatewayConfigId");
        List<String> numbers = checkList(context.get("numbers"), String.class);;
        String message = (String) context.get("message");
        
        String telecomEnabled = EntityUtilProperties.getPropertyValue("general", "telecom.notifications.enabled", delegator);
        if (!"Y".equals(telecomEnabled)) {
            Debug.logImportant("Telecom message not sent to " + numbers.toString() +" because telecom.notifications.enabled property is set to N or empty", module);
            return ServiceUtil.returnSuccess("Telecom message not sent to " + numbers.toString() +" because sms.notifications.enabled property is set to N or empty");
        }

        String redirectNumber = EntityUtilProperties.getPropertyValue("general", "telecom.notifications.redirectTo", delegator);
        if (UtilValidate.isNotEmpty(redirectNumber)) {
            numbers.clear();
            numbers.add(redirectNumber);
        }


        try {
            Map<String, Object> createCommEventCtx = new HashMap<String, Object>();
            createCommEventCtx = ctx.makeValidContext("createCommunicationEvent", ModelService.IN_PARAM, context);
            createCommEventCtx.put("content", message);
            createCommEventCtx.put("communicationEventTypeId", "PHONE_COMMUNICATION");
            createCommEventCtx.put("fromString", EntityUtilProperties.getPropertyValue("general", "defaultFromTelecomAddress", delegator));
            createCommEventCtx.put("subject", telecomMsgTypeEnumId);
            createCommEventCtx.put("toString", numbers.toString());
            Map<String, Object> createCommEventResult = dispatcher.runSync("createCommunicationEvent", createCommEventCtx);
            if (!ServiceUtil.isSuccess(createCommEventResult)) {
                Debug.logError(ServiceUtil.getErrorMessage(createCommEventResult), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createCommEventResult));
            }
            String communicationEventId = (String) createCommEventResult.get("communicationEventId");

            Map<String, Object> conditions = new HashMap<String, Object>();
            conditions.put("productStoreId", productStoreId);
            conditions.put("telecomMsgTypeEnumId", telecomMsgTypeEnumId);
            conditions.put("telecomMethodTypeId", telecomMethodTypeId);
            GenericValue productStoreTelecomSetting = EntityQuery.use(delegator).from("ProductStoreTelecomSetting").where(conditions).queryOne();
            if (productStoreTelecomSetting != null) {
                GenericValue customMethod = productStoreTelecomSetting.getRelatedOne("CustomMethod", false);
                if (UtilValidate.isNotEmpty(customMethod.getString("customMethodName"))) {
                    Map<String, Object> serviceCtx = new HashMap<String, Object>();
                    serviceCtx.put("numbers", numbers);
                    serviceCtx.put("message", message);
                    if (telecomGatewayConfigId != null) {
                        serviceCtx.put("configId", telecomGatewayConfigId);
                    }
                    serviceCtx.put("userLogin", userLogin);
                    Map<String, Object> customMethodResult = dispatcher.runSync(customMethod.getString("customMethodName"), serviceCtx);
                    if (ServiceUtil.isError(customMethodResult) || ServiceUtil.isFailure(customMethodResult)) {
                        String errorMessage = ServiceUtil.getErrorMessage(customMethodResult);
                        Debug.logError(errorMessage, module);
                        return ServiceUtil.returnError(errorMessage);
                    }

                    createCommEventCtx.clear();
                    createCommEventCtx.put("communicationEventId", communicationEventId);
                    if (UtilValidate.isNotEmpty(createCommEventResult.get("response")))
                        createCommEventCtx.put("note", customMethodResult.get("response"));
                    createCommEventCtx.put("statusId", "COM_COMPLETE");
                    createCommEventCtx.put("userLogin", userLogin);
                    dispatcher.runSync("updateCommunicationEvent", createCommEventCtx);
                }
            } else {
                return ServiceUtil.returnError("Not sending SMS as no ProductStoreEmailSetting found for the passed inputs.");
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }
}
