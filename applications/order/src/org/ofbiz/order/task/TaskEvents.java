/*
 * $Id: TaskEvents.java 6642 2006-02-01 02:29:51Z sichen $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.order.task;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.event.EventHandler;
import org.ofbiz.webapp.event.EventHandlerException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;

/**
 * Order Processing Task Events
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class TaskEvents {
    
    public static final String module = TaskEvents.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";
    
    /** Complete assignment event */
    public static String completeAssignment(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        
        Map parameterMap = UtilHttp.getParameterMap(request);
        String workEffortId = (String) parameterMap.remove("workEffortId");
        String partyId = (String) parameterMap.remove("partyId");
        String roleTypeId = (String) parameterMap.remove("roleTypeId");
        String fromDateStr = (String) parameterMap.remove("fromDate");
        java.sql.Timestamp fromDate = null;
        Locale locale = UtilHttp.getLocale(request);
        
        try {       
            fromDate = (java.sql.Timestamp) ObjectType.simpleTypeConvert(fromDateStr, "java.sql.Timestamp", null, null);
        } catch (GeneralException e) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderInvalidDateFormatForFromDate", locale));
            return "error";        
        }
        
        Map result = null;
        try {
            Map context = UtilMisc.toMap("workEffortId", workEffortId, "partyId", partyId, "roleTypeId", roleTypeId, 
                    "fromDate", fromDate, "result", parameterMap, "userLogin", userLogin);
            result = dispatcher.runSync("wfCompleteAssignment", context);
            if (result.containsKey(ModelService.RESPOND_ERROR)) {
                request.setAttribute("_ERROR_MESSAGE_", (String) result.get(ModelService.ERROR_MESSAGE));
                return "error";
            }
        } catch (GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderProblemsInvokingTheCompleteAssignmentService", locale));
            return "error";
        }

        return "success";              
    }    
    
    /** Accept role assignment event */
    public static String acceptRoleAssignment(HttpServletRequest request, HttpServletResponse response) { 
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        Locale locale = UtilHttp.getLocale(request);
        
        if (addToOrderRole(request)) {
            try {                
                EventHandler eh = rh.getEventFactory().getEventHandler("service");
                eh.invoke("", "wfAcceptRoleAssignment", request, response); 
            } catch (EventHandlerException e) {
                Debug.logError(e, "Invocation error", module);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderFailedToInvokeTheWfAcceptRoleAssignmentService", locale));
                return "error";
            } 
            return "success";                         
        }                    
        return "error";
    }
    
    /** Delegate and accept assignment event */
    public static String delegateAndAcceptAssignment(HttpServletRequest request, HttpServletResponse response) {        
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        Locale locale = UtilHttp.getLocale(request);
        
        if (addToOrderRole(request)) {
            try {
                EventHandler eh = rh.getEventFactory().getEventHandler("service");
                eh.invoke("", "wfDelegateAndAcceptAssignmet", request, response); 
            } catch (EventHandlerException e) {
                Debug.logError(e, "Invocation error", module);
                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(resource_error,"OrderFailedToInvokeTheWfDelegateAndAcceptAssignmentService", locale));
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
        Map context = UtilMisc.toMap("orderId", orderId, "partyId", partyId, "roleTypeId", roleTypeId);
        Map result = null;
        try {
            result = dispatcher.runSync("addOrderRole", context);  
            Debug.logInfo("Added user to order role " + result, module);          
        } catch (GenericServiceException gse) {
            request.setAttribute("_ERROR_MESSAGE_", gse.getMessage());
            return false;
        }
        return true;
    }

}
