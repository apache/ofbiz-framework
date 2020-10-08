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
package org.apache.ofbiz.manufacturing.jobshopmgt;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class ProductionRunEvents {

    private static final String MODULE = ProductionRunEvents.class.getName();

    public static String productionRunDeclareAndProduce(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        Map<String, Object> parameters = UtilHttp.getParameterMap(request);

        BigDecimal quantity = null;
        try {
            quantity = new BigDecimal((String) parameters.get("quantity"));
        } catch (NumberFormatException nfe) {
            String errMsg = "Invalid format for quantity field: " + nfe.toString();
            Debug.logError(nfe, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        Collection<Map<String, Object>> componentRows = UtilHttp.parseMultiFormData(parameters);
        Map<GenericPK, Object> componentsLocationMap = new HashMap<>();
        for (Map<String, Object> componentRow : componentRows) {
            Timestamp fromDate = null;
            try {
                fromDate = Timestamp.valueOf((String) componentRow.get("fromDate"));
            } catch (IllegalArgumentException iae) {
                String errMsg = "Invalid format for date field: " + iae.toString();
                Debug.logError(iae, errMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            GenericPK key = delegator.makePK("WorkEffortGoodStandard",
                    UtilMisc.<String, Object>toMap("workEffortId", (String) componentRow.get("productionRunTaskId"),
                            "productId", (String) componentRow.get("productId"),
                            "fromDate", fromDate,
                            "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"));
            componentsLocationMap.put(key,
                    UtilMisc.<String, Object>toMap("locationSeqId", (String) componentRow.get("locationSeqId"),
                            "secondaryLocationSeqId", (String) componentRow.get("secondaryLocationSeqId"),
                            "failIfItemsAreNotAvailable", (String) componentRow.get("failIfItemsAreNotAvailable")));
        }

        try {
            Map<String, Object> inputMap = UtilMisc.<String, Object>toMap("workEffortId", parameters.get("workEffortId"),
                    "inventoryItemTypeId", parameters.get("inventoryItemTypeId"));
            inputMap.put("componentsLocationMap", componentsLocationMap);
            inputMap.put("quantity", quantity);
            inputMap.put("lotId", parameters.get("lotId"));
            inputMap.put("userLogin", userLogin);
            Map<String, Object> result = dispatcher.runSync("productionRunDeclareAndProduce", inputMap);
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error issuing materials: " + e.toString();
            Debug.logError(e, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        return "success";
    }
}
