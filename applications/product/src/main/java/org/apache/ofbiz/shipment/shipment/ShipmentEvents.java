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
package org.apache.ofbiz.shipment.shipment;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * ShippingEvents - Events used for processing shipping fees
 */
public class ShipmentEvents {

    private static final String MODULE = ShipmentEvents.class.getName();

    public static String viewShipmentPackageRouteSegLabelImage(HttpServletRequest request, HttpServletResponse response) {

        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String shipmentId = request.getParameter("shipmentId");
        String shipmentRouteSegmentId = request.getParameter("shipmentRouteSegmentId");
        String shipmentPackageSeqId = request.getParameter("shipmentPackageSeqId");

        GenericValue shipmentPackageRouteSeg = null;
        try {
            shipmentPackageRouteSeg = EntityQuery.use(delegator).from("ShipmentPackageRouteSeg").where("shipmentId", shipmentId,
                    "shipmentRouteSegmentId", shipmentRouteSegmentId, "shipmentPackageSeqId", shipmentPackageSeqId).queryOne();
        } catch (GenericEntityException e) {
            String errorMsg = "Error looking up ShipmentPackageRouteSeg: " + e.toString();
            Debug.logError(e, errorMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            return "error";
        }

        if (shipmentPackageRouteSeg == null) {
            request.setAttribute("_ERROR_MESSAGE_", "Could not find ShipmentPackageRouteSeg where shipmentId=[" + shipmentId
                    + "], shipmentRouteSegmentId=[" + shipmentRouteSegmentId + "], shipmentPackageSeqId=[" + shipmentPackageSeqId + "]");
            return "error";
        }

        byte[] bytes = shipmentPackageRouteSeg.getBytes("labelImage");
        if (bytes == null || bytes.length == 0) {
            request.setAttribute("_ERROR_MESSAGE_", "The ShipmentPackageRouteSeg was found where shipmentId=[" + shipmentId
                    + "], shipmentRouteSegmentId=[" + shipmentRouteSegmentId + "], shipmentPackageSeqId=[" + shipmentPackageSeqId
                    + "], but there was no labelImage on the value.");
            return "error";
        }

        // TODO: record the image format somehow to make this block nicer.  Right now we're just trying GIF first as a default,
        //  then if it doesn't work, trying PNG.
        // It would be nice to store the actual type of the image alongside the image data.
        try {
            UtilHttp.streamContentToBrowser(response, bytes, "image/gif");
        } catch (IOException e1) {
            try {
                UtilHttp.streamContentToBrowser(response, bytes, "image/png");
            } catch (IOException e2) {
                String errorMsg = "Error writing labelImage to OutputStream: " + e2.toString();
                Debug.logError(e2, errorMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                return "error";
            }
        }

        return "success";
    }

    public static String checkForceShipmentReceived(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        String shipmentId = request.getParameter("shipmentIdReceived");
        String forceShipmentReceived = request.getParameter("forceShipmentReceived");
        if (UtilValidate.isNotEmpty(shipmentId) && "Y".equals(forceShipmentReceived)) {
            try {
                Map<String, Object> inputMap = UtilMisc.<String, Object>toMap("shipmentId", shipmentId, "statusId", "PURCH_SHIP_RECEIVED");
                inputMap.put("userLogin", userLogin);
                Map<String, Object> resultMap = dispatcher.runSync("updateShipment", inputMap);
                if (ServiceUtil.isError(resultMap)) {
                    String errorMessage = ServiceUtil.getErrorMessage(resultMap);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    return "error";
                }
            } catch (GenericServiceException gse) {
                String errMsg = "Error updating shipment [" + shipmentId + "]: " + gse.toString();
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        }
        return "success";
    }
}

