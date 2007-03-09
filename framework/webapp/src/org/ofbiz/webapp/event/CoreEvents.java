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
package org.ofbiz.webapp.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.calendar.RecurrenceRule;
import org.ofbiz.webapp.control.RequestHandler;

/**
 * CoreEvents - WebApp Events Related To Framework pieces
 */
public class CoreEvents {
    
    public static final String module = CoreEvents.class.getName();
    public static final String err_resource = "WebappUiLabels";

    /**
     * Return success event. Used as a place holder for events.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return Response code string
     */
    public static String returnSuccess(HttpServletRequest request, HttpServletResponse response) {
        return "success";
    }

    /**
     * Return error event. Used as a place holder for events.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return Response code string
     */
    public static String returnError(HttpServletRequest request, HttpServletResponse response) {
        return "error";
    }

    /**
     * Return null event. Used as a place holder for events.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return Response code string
     */
    public static String returnNull(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * Change delegator event. Changes the delegator for the current session
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return Response code string
     */
    public static String changeDelegator(HttpServletRequest request, HttpServletResponse response) {
        String delegatorName = request.getParameter("delegator");
        Security security = (Security) request.getAttribute("security");
        Locale locale = UtilHttp.getLocale(request);

        if (!security.hasPermission("ENTITY_MAINT", request.getSession())) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.not_authorized_use_fct", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }
        if (delegatorName == null) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.delegator_not_passed", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }

        GenericDelegator delegator = GenericDelegator.getGenericDelegator(delegatorName);

        if (delegator == null) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.no_delegator_name_defined", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }

        // now change the dispatcher to use this delegator
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        DispatchContext dctx = dispatcher.getDispatchContext();
        String dispatcherName = dispatcher.getName();

        if (dispatcherName == null) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.dispatcher_name_null", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }
        if (dctx == null) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.dispatcher_context_null", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }

        dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, delegator);

        request.getSession().setAttribute("delegator", delegator);
        request.getSession().setAttribute("dispatcher", dispatcher);

        return "success";
    }

    /**
     * Change dispatcher event. Changes the dispatch for the current session
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return Response code string
     */
    public static String changeDispatcher(HttpServletRequest request, HttpServletResponse response) {
        String dispatcherName = request.getParameter("dispatcher");
        Security security = (Security) request.getAttribute("security");
        Locale locale = UtilHttp.getLocale(request);

        if (!security.hasPermission("ENTITY_MAINT", request.getSession())) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.not_authorized_use_fct", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }
        if (dispatcherName == null) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.dispatcher_not_passed", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }

        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        ServiceDispatcher sd = ServiceDispatcher.getInstance(dispatcherName, delegator);

        if (sd == null) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.no_dispachter_name_registered", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }
        LocalDispatcher dispatcher = sd.getLocalContext(dispatcherName).getDispatcher();

        request.getSession().setAttribute("dispatcher", dispatcher);
        return "success";
    }

    /**
     * Schedule a service for a specific time or recurrence
     *  Request Parameters which are used for this service:
     *
     *  SERVICE_NAME      - Name of the service to invoke
     *  SERVICE_TIME      - First time the service will occur
     *  SERVICE_FREQUENCY - The type of recurrence (SECONDLY,MINUTELY,DAILY,etc)
     *  SERVICE_INTERVAL  - The interval of the frequency (every 5 minutes, etc)
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return Response code string
     */
    public static String scheduleService(HttpServletRequest request, HttpServletResponse response) {
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        Security security = (Security) request.getAttribute("security");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        //GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        
        Map params = UtilHttp.getParameterMap(request);
        // get the schedule parameters
        String jobName = (String) params.remove("JOB_NAME");
        String serviceName = (String) params.remove("SERVICE_NAME");
        String poolName = (String) params.remove("POOL_NAME");
        String serviceTime = (String) params.remove("SERVICE_TIME");
        String serviceEndTime = (String) params.remove("SERVICE_END_TIME");
        String serviceFreq = (String) params.remove("SERVICE_FREQUENCY");
        String serviceIntr = (String) params.remove("SERVICE_INTERVAL");
        String serviceCnt = (String) params.remove("SERVICE_COUNT");
        String retryCnt = (String) params.remove("SERVICE_MAXRETRY");

        // the frequency map
        Map freqMap = new HashMap();

        freqMap.put("SECONDLY", new Integer(1));
        freqMap.put("MINUTELY", new Integer(2));
        freqMap.put("HOURLY", new Integer(3));
        freqMap.put("DAILY", new Integer(4));
        freqMap.put("WEEKLY", new Integer(5));
        freqMap.put("MONTHLY", new Integer(6));
        freqMap.put("YEARLY", new Integer(7));

        // some defaults
        long startTime = (new Date()).getTime();
        long endTime = 0;
        int maxRetry = -1;
        int count = 1;
        int interval = 1;
        int frequency = RecurrenceRule.DAILY;

        StringBuffer errorBuf = new StringBuffer();

        // make sure we passed a service
        if (serviceName == null) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.must_specify_service", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }

        // lookup the service definition to see if this service is externally available, if not require the SERVICE_INVOKE_ANY permission
        ModelService modelService = null;
        try {
            modelService = dispatcher.getDispatchContext().getModelService(serviceName);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error looking up ModelService for serviceName [" + serviceName + "]", module);
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.error_modelservice_for_srv_name", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + " [" + serviceName + "]: " + e.toString());
            return "error";
        }
        if (modelService == null) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.service_name_not_find", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + " [" + serviceName + "]");
            return "error";
        }
        
        // make the context valid; using the makeValid method from ModelService
        Map serviceContext = new HashMap();
        Iterator ci = modelService.getInParamNames().iterator();
        while (ci.hasNext()) {
            String name = (String) ci.next();

            // don't include userLogin, that's taken care of below
            if ("userLogin".equals(name)) continue;
            // don't include locale, that is also taken care of below
            if ("locale".equals(name)) continue;
            
            Object value = request.getParameter(name);

            // if the parameter wasn't passed and no other value found, don't pass on the null
            if (value == null) {
                value = request.getAttribute(name);
            } 
            if (value == null) {
                value = request.getSession().getAttribute(name);
            }
            if (value == null) {
                // still null, give up for this one
                continue;
            }
            
            if (value instanceof String && ((String) value).length() == 0) {
                // interpreting empty fields as null values for each in back end handling...
                value = null;
            }

            // set even if null so that values will get nulled in the db later on
            serviceContext.put(name, value);
        }     
        serviceContext = modelService.makeValid(serviceContext, ModelService.IN_PARAM, true, null, locale);
        
        if (userLogin != null) {
            serviceContext.put("userLogin", userLogin);                             
        }
        
        if (locale != null) {
            serviceContext.put("locale", locale);
        }
                
        if (!modelService.export && !security.hasPermission("SERVICE_INVOKE_ANY", request.getSession())) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.not_authorized_to_call", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }
        
        // some conversions
        if (serviceTime != null && serviceTime.length() > 0) {            
            try {        
                Timestamp ts1 = Timestamp.valueOf(serviceTime);
                startTime = ts1.getTime();
            } catch (IllegalArgumentException e) {
                try {                        
                    startTime = Long.parseLong(serviceTime);                    
                } catch (NumberFormatException nfe) {
                    String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.invalid_format_time", locale);
                    errorBuf.append("<li>" + errMsg);
                }
            }
            if (startTime < (new Date()).getTime()) {
                String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.service_time_already_passed", locale);
                errorBuf.append("<li>" + errMsg);
            }
        }
        if (serviceEndTime != null && serviceEndTime.length() > 0) {            
            try {        
                Timestamp ts1 = Timestamp.valueOf(serviceEndTime);
                endTime = ts1.getTime();
            } catch (IllegalArgumentException e) {
                try {                        
                    endTime = Long.parseLong(serviceTime);
                } catch (NumberFormatException nfe) {
                    String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.invalid_format_time", locale);
                    errorBuf.append("<li>" + errMsg);
                }
            }
            if (endTime < (new Date()).getTime()) {
                String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.service_time_already_passed", locale);
                errorBuf.append("<li>" + errMsg);
            }
        }
        if (serviceIntr != null && serviceIntr.length() > 0) {
            try {
                interval = Integer.parseInt(serviceIntr);
            } catch (NumberFormatException nfe) {
                String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.invalid_format_interval", locale);
                errorBuf.append("<li>" + errMsg);
            }
        }
        if (serviceCnt != null && serviceCnt.length() > 0) {
            try {
                count = Integer.parseInt(serviceCnt);
            } catch (NumberFormatException nfe) {
                String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.invalid_format_count", locale);
                errorBuf.append("<li>" + errMsg);
            }
        }
        if (serviceFreq != null && serviceFreq.length() > 0) {
            int parsedValue = 0;

            try {
                parsedValue = Integer.parseInt(serviceFreq);
                if (parsedValue > 0 && parsedValue < 8)
                    frequency = parsedValue;
            } catch (NumberFormatException nfe) {
                parsedValue = 0;
            }
            if (parsedValue == 0) {
                if (!freqMap.containsKey(serviceFreq.toUpperCase())) {
                    String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.invalid_format_frequency", locale);
                    errorBuf.append("<li>" + errMsg);
                } else {
                    frequency = ((Integer) freqMap.get(serviceFreq.toUpperCase())).intValue();
                }
            }
        }
        if (retryCnt != null && retryCnt.length() > 0) {
            int parsedValue = -2;

            try {
                parsedValue = Integer.parseInt(retryCnt);
            } catch (NumberFormatException e) {
                parsedValue = -2;
            }
            if (parsedValue > -2) {
                maxRetry = parsedValue;
            } else {
                maxRetry = modelService.maxRetry;
            }
        } else {
            maxRetry = modelService.maxRetry;
        }

        // return the errors
        if (errorBuf.length() > 0) {
            request.setAttribute("_ERROR_MESSAGE_", errorBuf.toString());
            return "error";
        }
                
        Map syncServiceResult = null;
        // schedule service
        try {
            if(null!=request.getParameter("_RUN_SYNC_") && request.getParameter("_RUN_SYNC_").equals("Y")){
                syncServiceResult = dispatcher.runSync(serviceName, serviceContext);
            }else{
                dispatcher.schedule(jobName, poolName, serviceName, serviceContext, startTime, frequency, interval, count, endTime, maxRetry);
            }
        } catch (GenericServiceException e) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.service_dispatcher_exception", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + e.getMessage());
            return "error";
        }
        
        String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.service_scheduled", locale);
        request.setAttribute("_EVENT_MESSAGE_", errMsg);
        if(null!=syncServiceResult){
            request.getSession().setAttribute("_RUN_SYNC_RESULT_", syncServiceResult);
            return "sync_success";
        }
        return "success";
    }
    
    public static String saveServiceResultsToSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Locale locale = UtilHttp.getLocale(request);
        Map syncServiceResult = (Map)session.getAttribute("_RUN_SYNC_RESULT_");
        if(null==syncServiceResult){
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.no_fields_in_session", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }
        
        if(null!=request.getParameter("_CLEAR_PREVIOUS_PARAMS_") && request.getParameter("_CLEAR_PREVIOUS_PARAMS_").equalsIgnoreCase("on"))
            session.removeAttribute("_SAVED_SYNC_RESULT_");
        
        Map serviceFieldsToSave = request.getParameterMap();
        Map savedFields = FastMap.newInstance();
        Set keys = serviceFieldsToSave.keySet();
        Iterator keysItr = keys.iterator();
        
        while(keysItr.hasNext()){
            String key = (String)keysItr.next();
            if(null!=serviceFieldsToSave.get(key) && request.getParameter(key).equalsIgnoreCase("on") && !key.equals("_CLEAR_PREVIOUS_PARAMS_")){
                String[] servicePath = key.split("\\|\\|");
                String partialKey = servicePath[servicePath.length-1];
                savedFields.put(partialKey, getObjectFromServicePath(key ,syncServiceResult));
            }
        }
        if(null!=session.getAttribute("_SAVED_SYNC_RESULT_")){
            Map savedSyncResult = (Map)session.getAttribute("_SAVED_SYNC_RESULT_");
            savedSyncResult.putAll(savedFields);
            savedFields = savedSyncResult; 
        }
        session.setAttribute("_SAVED_SYNC_RESULT_", savedFields);
        return "success";
    }
    
    //Tries to return a map, if Object is one of HashMap, GenericEntity, List
    public static Object getObjectFromServicePath(String servicePath, Map serviceResult){
        String[] sp = servicePath.split("\\|\\|");
        Object servicePathObject = null;
        Map servicePathMap = null;
        for(int i=0;i<sp.length;i++){
            String servicePathEntry = sp[i];
            if(null==servicePathMap){
                servicePathObject = serviceResult.get(servicePathEntry);
            }else{
                servicePathObject = servicePathMap.get(servicePathEntry);
            }
            servicePathMap = null;
            
            if(servicePathObject instanceof HashMap){
                servicePathMap = (HashMap)servicePathObject;
            }else if(servicePathObject instanceof GenericEntity){
                GenericEntity servicePathEntity = (GenericEntity)servicePathObject;
                Set servicePathEntitySet = servicePathEntity.keySet();
                Iterator spesItr = servicePathEntitySet.iterator();
                servicePathMap = FastMap.newInstance();
                while(spesItr.hasNext()){
                    String spesKey = (String)spesItr.next();
                    servicePathMap.put(spesKey, servicePathEntity.get(spesKey));
                }
            }else if(servicePathObject instanceof Collection){
                Collection servicePathColl = (Collection)servicePathObject;
                Iterator splItr = servicePathColl.iterator();
                int count=0;
                servicePathMap = FastMap.newInstance();
                while(splItr.hasNext()){
                    servicePathMap.put("_"+count+"_", splItr.next());
                    count++;
                }
            }
        }
        if(null==servicePathMap){
            return servicePathObject;
        }else{
            return servicePathMap;
        }
    }

    public static ServiceEventHandler seh = new ServiceEventHandler();

    /**
     * Run a service.
     *  Request Parameters which are used for this event:
     *  SERVICE_NAME      - Name of the service to invoke
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return Response code string
     */
    public static String runService(HttpServletRequest request, HttpServletResponse response) {
        // get the mode and service name 
        String serviceName = request.getParameter("serviceName");
        String mode = request.getParameter("mode");
        Locale locale = UtilHttp.getLocale(request);
        
        if (UtilValidate.isEmpty(serviceName)) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.must_specify_service_name", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg);
            return "error";
        }

        if (UtilValidate.isEmpty(mode)) {
            mode = "sync";
        }
        
        // now do a security check
        
        Security security = (Security) request.getAttribute("security");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        
        //lookup the service definition to see if this service is externally available, if not require the SERVICE_INVOKE_ANY permission
        ModelService modelService = null;
        try {
            modelService = dispatcher.getDispatchContext().getModelService(serviceName);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error looking up ModelService for serviceName [" + serviceName + "]", module);
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.error_modelservice_for_srv_name", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + "[" + serviceName + "]: " + e.toString());
            return "error";
        }
        if (modelService == null) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.service_name_not_find", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + "[" + serviceName + "]");
            return "error";
        }

        if (!modelService.export && !security.hasPermission("SERVICE_INVOKE_ANY", request.getSession())) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.not_authorized_to_call", locale);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + ".");
            return "error";
        }
        
        Debug.logInfo("Running service named [" + serviceName + "] from event with mode [" + mode + "]", module);
        
        // call the service via the ServiceEventHandler which 
        // adapts an event to a service.
        try {
            return seh.invoke(mode, serviceName, request, response);
        } catch (EventHandlerException e) {
            String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.service_eventhandler_exception", locale);                                  
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + ": " + e.getMessage());
            return "error";
        }
    }
    
    public static String streamFile(HttpServletRequest request, HttpServletResponse response) {
        //RequestHandler rh = (RequestHandler) request.getAttribute("_REQUEST_HANDLER_");
        String filePath = RequestHandler.getNextPageUri(request.getPathInfo());
        //String fileName = filePath.substring(filePath.lastIndexOf("/")+1);
        
        // load the file
        File file = new File(filePath);
        if (file.exists()) {
            Long longLen = new Long(file.length());
            int length = longLen.intValue();
            try {
                FileInputStream fis = new FileInputStream(file);                                
                UtilHttp.streamContentToBrowser(response, fis, length, null);
                fis.close();                
            } catch (FileNotFoundException e) {
                Debug.logError(e, module);
                return "error";
            } catch (IOException e) {
                Debug.logError(e, module);
                return "error";
            }
        }                        
        return null;
    }
}
