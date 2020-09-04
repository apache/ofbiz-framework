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
package org.apache.ofbiz.order.task;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.event.EventHandler;
import org.apache.ofbiz.webapp.event.EventHandlerException;

/**
 * Order Processing Task Events
 */
public class TaskEvents {

    private static final String MODULE = TaskEvents.class.getName();
    private static final String RES_ERROR = "OrderErrorUiLabels";

    /** Complete assignment event */
    public static String completeAssignment(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        Map<String, Object> parameterMap = UtilHttp.getParameterMap(request);
        String workEffortId = (String) parameterMap.remove("workEffortId");
        String partyId = (String) parameterMap.remove("partyId");
        String roleTypeId = (String) parameterMap.remove("roleTypeId");
        String fromDateStr = (String) parameterMap.remove("fromDate");
        java.sql.Timestamp fromDate = null;
        Locale locale = UtilHttp.getLocale(request);

        try {
            fromDate = (java.sql.Timestamp) ObjectType.simpleTypeOrObjectConvert(fromDateStr, "java.sql.Timestamp", null, null);
        } catch (GeneralException e) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR, "OrderInvalidDateFormatForFromDate", locale));
            return "error";
        }

        Map<String, Object> result = null;
        try {
            Map<String, ? extends Object> context = UtilMisc.toMap("workEffortId", workEffortId, "partyId", partyId, "roleTypeId", roleTypeId,
                    "fromDate", fromDate, "result", parameterMap, "userLogin", userLogin);
            result = dispatcher.runSync("wfCompleteAssignment", context);
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }
        } catch (GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR,
                    "OrderProblemsInvokingTheCompleteAssignmentService", locale));
            return "error";
        }

        return "success";
    }

    /** Accept role assignment event */
    public static String acceptRoleAssignment(HttpServletRequest request, HttpServletResponse response) {
        RequestHandler rh = RequestHandler.from(request);
        Locale locale = UtilHttp.getLocale(request);

        if (addToOrderRole(request)) {
            try {
                EventHandler eh = rh.getEventFactory().getEventHandler("service");
                eh.invoke(new Event("service", "", "wfAcceptRoleAssignment", true), null, request, response);
            } catch (EventHandlerException e) {
                Debug.logError(e, "Invocation error", MODULE);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR,
                        "OrderFailedToInvokeTheWfAcceptRoleAssignmentService", locale));
                return "error";
            }
            return "success";
        }
        return "error";
    }

    /** Delegate and accept assignment event */
    public static String delegateAndAcceptAssignment(HttpServletRequest request, HttpServletResponse response) {
        RequestHandler rh = RequestHandler.from(request);
        Locale locale = UtilHttp.getLocale(request);

        if (addToOrderRole(request)) {
            try {
                EventHandler eh = rh.getEventFactory().getEventHandler("service");
                eh.invoke(new Event("service", "", "wfAcceptRoleAssignment", true), null, request, response);
            } catch (EventHandlerException e) {
                Debug.logError(e, "Invocation error", MODULE);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(RES_ERROR,
                        "OrderFailedToInvokeTheWfDelegateAndAcceptAssignmentService", locale));
                return "error";
            }
            return "success";
        }
        return "error";
    }

    private static boolean addToOrderRole(HttpServletRequest request) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String partyId = request.getParameter("partyId");
        String roleTypeId = request.getParameter("roleTypeId");
        String orderId = request.getParameter("orderId");
        Map<String, ? extends Object> context = UtilMisc.toMap("orderId", orderId, "partyId", partyId, "roleTypeId", roleTypeId);
        Map<String, Object> result = null;
        try {
            result = dispatcher.runSync("addOrderRole", context);
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return false;
            }
            Debug.logInfo("Added user to order role " + result, MODULE);
        } catch (GenericServiceException gse) {
            request.setAttribute("_ERROR_MESSAGE_", gse.getMessage());
            return false;
        }
        return true;
    }

}
