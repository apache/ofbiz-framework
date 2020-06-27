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
package org.apache.ofbiz.product.inventory;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.control.ExternalLoginKeysManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.*;

public class InventoryEvents {
    public static final String MODULE = InventoryEvents.class.getName();
    public static final String resource = "ProductUiLabels";

    public static String createInventoryCountAndAddBulkLocations(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        String facilityId = request.getParameter("facilityId");
        if (UtilValidate.isEmpty(facilityId)) {
            request.setAttribute("_ERROR_MESSAGE_", "Facility should not be empty.");
            return "error";
        }
        String inventoryCountId = null;
        List<String> locationSeqIds = new ArrayList<String>();

        Map<String, Object> serviceResult = new HashMap<String, Object>();
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        int rowCount = UtilHttp.getMultiFormRowCount(paramMap);

        for(int i = 0; i < rowCount; ++i) {
            String curSuffix = UtilHttp.getMultiRowDelimiter() + i;
            boolean rowSelected = false;
            if (UtilValidate.isNotEmpty(request.getAttribute(UtilHttp.getRowSubmitPrefix() + i))) {
                rowSelected = request.getAttribute(UtilHttp.getRowSubmitPrefix() + i) == null ? false :
                        "Y".equalsIgnoreCase((String)request.getAttribute(UtilHttp.getRowSubmitPrefix() + i));
            } else {
                rowSelected = request.getParameter(UtilHttp.getRowSubmitPrefix() + i) == null ? false :
                        "Y".equalsIgnoreCase(request.getParameter(UtilHttp.getRowSubmitPrefix() + i));
            }

            if (!rowSelected) {
                continue;
            }
            String thisSuffix = UtilHttp.getMultiRowDelimiter() + i;        // current suffix after each field id
            if (paramMap.containsKey("locationSeqId" + thisSuffix)) {
                String locationSeqId = (String) paramMap.remove("locationSeqId" + thisSuffix);
                locationSeqIds.add(locationSeqId);
            }
        }

        try {
            Map<String, Object> createInventoryCountCtx = new HashMap<String, Object>();
            createInventoryCountCtx.put("userLogin", userLogin);
            createInventoryCountCtx.put("facilityId", facilityId);
            createInventoryCountCtx.put("statusId", "INV_COUNT_CREATED");
            createInventoryCountCtx.put("createdByUserLogin", userLogin.getString("userLoginId"));
            createInventoryCountCtx.put("createdDate", UtilDateTime.nowTimestamp());
            serviceResult = dispatcher.runSync("createInventoryCount", createInventoryCountCtx);
            if (!ServiceUtil.isSuccess(serviceResult)) {
                Debug.logError(ServiceUtil.getErrorMessage(serviceResult), MODULE);
                return "error";
            }
            inventoryCountId = (String) serviceResult.get("inventoryCountId");

            for (String locationSeqId : locationSeqIds) {
                GenericValue inventoryItemAndLocation = EntityQuery.use(delegator).from("FacilityLocation").where("facilityId", facilityId, "locationSeqId", locationSeqId, "locked", "Y").queryFirst();
                if (UtilValidate.isNotEmpty(inventoryItemAndLocation)) {
                    Debug.logError("Location #" + locationSeqId + " is currently locked under active counting session #" + inventoryItemAndLocation.getString("inventoryCountId") + " Please choose another location or wait till lock is released.", MODULE);
                    request.setAttribute("_ERROR_MESSAGE_", "Location #" + locationSeqId + " is currently locked under active counting session #" + inventoryItemAndLocation.getString("inventoryCountId") + ",  Please choose another location or wait till lock is released.");
                    return "error";
                }
                Map<String, Object> serviceInCtx = new HashMap<String, Object>();
                serviceInCtx.put("inventoryCountId", inventoryCountId);
                serviceInCtx.put("facilityId", facilityId);
                serviceInCtx.put("locationSeqId", locationSeqId);
                serviceInCtx.put("userLogin", userLogin);
                serviceResult.clear();
                serviceResult = dispatcher.runSync("addLocationItemsToCycleCount", serviceInCtx);
                if (!ServiceUtil.isSuccess(serviceResult)) {
                    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(serviceResult));
                    return "error";
                }
            }
            request.setAttribute("inventoryCountId", inventoryCountId);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        request.setAttribute("_EVENT_MESSAGE_", "Session # " + inventoryCountId + " Created successfully.");
        return "success";
    }
}