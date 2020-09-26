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
package org.apache.ofbiz.webapp.event;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelParam;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceValidationException;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.apache.ofbiz.widget.renderer.VisualTheme;

/**
 * ServiceEventHandler - Service Event Handler
 */
public class ServiceEventHandler implements EventHandler {

    private static final String MODULE = ServiceEventHandler.class.getName();

    public static final String SYNC = "sync";
    public static final String ASYNC = "async";

    @Override
    public void init(ServletContext context) throws EventHandlerException {
    }

    @Override
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response)
            throws EventHandlerException {
        // make sure we have a valid reference to the Service Engine
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        if (dispatcher == null) {
            throw new EventHandlerException("The local service dispatcher is null");
        }
        DispatchContext dctx = dispatcher.getDispatchContext();
        if (dctx == null) {
            throw new EventHandlerException("Dispatch context cannot be found");
        }

        // get the details for the service(s) to call
        String mode = SYNC;
        String serviceName = null;

        if (UtilValidate.isEmpty(event.getPath())) {
            mode = SYNC;
        } else {
            mode = event.getPath();
        }

        // make sure we have a defined service to call
        serviceName = event.getInvoke();
        if (serviceName == null) {
            throw new EventHandlerException("Service name (eventMethod) cannot be null");
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("[Set mode/service]: " + mode + "/" + serviceName, MODULE);
        }

        // some needed info for when running the service
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        // get the service model to generate context
        ModelService model = null;

        try {
            model = dctx.getModelService(serviceName);
        } catch (GenericServiceException e) {
            throw new EventHandlerException("Problems getting the service model", e);
        }

        if (model == null) {
            throw new EventHandlerException("Problems getting the service model");
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("[Processing]: SERVICE Event", MODULE);
            Debug.logVerbose("[Using delegator]: " + dispatcher.getDelegator().getDelegatorName(), MODULE);
        }

        Map<String, Object> rawParametersMap = UtilHttp.getCombinedMap(request);
        Map<String, Object> multiPartMap = UtilGenerics.cast(request.getAttribute("multiPartMap"));

        // we have a service and the model; build the context
        Map<String, Object> serviceContext = new HashMap<>();
        for (ModelParam modelParam: model.getInModelParamList()) {
            String name = modelParam.getName();

            // don't include userLogin, that's taken care of below
            if ("userLogin".equals(name)) {
                continue;
            }
            // don't include locale, that is also taken care of below
            if ("locale".equals(name)) {
                continue;
            }
            // don't include timeZone, that is also taken care of below
            if ("timeZone".equals(name)) {
                continue;
            }
            // don't include theme, that is also taken care of below
            if ("visualTheme".equals(name)) {
                continue;
            }

            Object value = null;
            if (UtilValidate.isNotEmpty(modelParam.getStringMapPrefix())) {
                Map<String, Object> paramMap = UtilHttp.makeParamMapWithPrefix(request, multiPartMap,
                        modelParam.getStringMapPrefix(), null);
                value = paramMap;
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Set [" + modelParam.getName() + "]: " + paramMap, MODULE);
                }
            } else if (UtilValidate.isNotEmpty(modelParam.getStringListSuffix())) {
                List<Object> paramList = UtilHttp.makeParamListWithSuffix(request, multiPartMap,
                        modelParam.getStringListSuffix(), null);
                value = paramList;
            } else {
                // first check the multi-part map
                value = multiPartMap.get(name);

                // next check attributes; do this before parameters so that attribute which can
                // be changed by code can override parameters which can't
                if (UtilValidate.isEmpty(value)) {
                    Object tempVal = request.getAttribute(UtilValidate.isEmpty(modelParam.getRequestAttributeName()) ? name
                            : modelParam.getRequestAttributeName());
                    if (tempVal != null) {
                        value = tempVal;
                    }
                }

                // check the request parameters
                if (UtilValidate.isEmpty(value)) {
                    // if the service modelParam has allow-html="any" then get this direct from the
                    // request instead of in the parameters Map so there will be no canonicalization
                    // possibly messing things up
                    if ("any".equals(modelParam.getAllowHtml())) {
                        value = request.getParameter(name);
                    } else {
                        // use the rawParametersMap from UtilHttp in order to also get pathInfo
                        // parameters, do canonicalization, etc
                        value = rawParametersMap.get(name);
                    }

                    // make any composite parameter data (e.g., from a set of parameters
                    // {name_c_date, name_c_hour, name_c_minutes})
                    if (value == null) {
                        value = UtilHttp.makeParamValueFromComposite(request, name);
                    }
                }

                // then session
                if (UtilValidate.isEmpty(value)) {
                    Object tempVal = request.getSession()
                            .getAttribute(UtilValidate.isEmpty(modelParam.getSessionAttributeName()) ? name
                                    : modelParam.getSessionAttributeName());
                    if (tempVal != null) {
                        value = tempVal;
                    }
                }

                // no field found
                if (value == null) {
                    //still null, give up for this one
                    continue;
                }

                if (value instanceof String && ((String) value).isEmpty()) {
                    // interpreting empty fields as null values for each in back end handling...
                    value = null;
                }
            }
            // set even if null so that values will get nulled in the db later on
            serviceContext.put(name, value);
        }

        // get only the parameters for this service - converted to proper type
        // TODO: pass in a list for error messages, like could not convert type or not a
        // proper X, return immediately with messages if there are any
        List<Object> errorMessages = new LinkedList<>();
        serviceContext = model.makeValid(serviceContext, ModelService.IN_PARAM, true, errorMessages, timeZone, locale);
        if (!errorMessages.isEmpty()) {
            // uh-oh, had some problems...
            request.setAttribute("_ERROR_MESSAGE_LIST_", errorMessages);
            return "error";
        }

        // include the UserLogin value object
        if (userLogin != null) {
            serviceContext.put("userLogin", userLogin);
        }

        // include the Locale object
        if (locale != null) {
            serviceContext.put("locale", locale);
        }

        // include the TimeZone object
        if (timeZone != null) {
            serviceContext.put("timeZone", timeZone);
        }

        // include the Theme object
        if (visualTheme != null) {
            serviceContext.put("visualTheme", visualTheme);
        }

        // invoke the service
        Map<String, Object> result = null;
        try {
            if (ASYNC.equalsIgnoreCase(mode)) {
                dispatcher.runAsync(serviceName, serviceContext);
            } else {
                result = dispatcher.runSync(serviceName, serviceContext);
            }
        } catch (ServiceAuthException e) {
            // not logging since the service engine already did
            request.setAttribute("_ERROR_MESSAGE_", e.getNonNestedMessage());
            return "error";
        } catch (ServiceValidationException e) {
            // not logging since the service engine already did
            request.setAttribute("serviceValidationException", e);
            if (e.getMessageList() != null) {
                request.setAttribute("_ERROR_MESSAGE_LIST_", e.getMessageList());
            } else {
                request.setAttribute("_ERROR_MESSAGE_", e.getNonNestedMessage());
            }
            return "error";
        } catch (GenericServiceException e) {
            Debug.logError(e, "Service invocation error", MODULE);
            throw new EventHandlerException("Service invocation error", e.getNested());
        }

        String responseString = null;

        if (result == null) {
            responseString = ModelService.RESPOND_SUCCESS;
        } else {

            if (!result.containsKey(ModelService.RESPONSE_MESSAGE)) {
                responseString = ModelService.RESPOND_SUCCESS;
            } else {
                responseString = (String) result.get(ModelService.RESPONSE_MESSAGE);
            }

            // set the messages in the request; this will be picked up by messages.ftl and displayed
            request.setAttribute("_ERROR_MESSAGE_LIST_", result.get(ModelService.ERROR_MESSAGE_LIST));
            request.setAttribute("_ERROR_MESSAGE_MAP_", result.get(ModelService.ERROR_MESSAGE_MAP));
            request.setAttribute("_ERROR_MESSAGE_", result.get(ModelService.ERROR_MESSAGE));

            request.setAttribute("_EVENT_MESSAGE_LIST_", result.get(ModelService.SUCCESS_MESSAGE_LIST));
            request.setAttribute("_EVENT_MESSAGE_", result.get(ModelService.SUCCESS_MESSAGE));

            // set the results in the request
            for (Map.Entry<String, Object> rme: result.entrySet()) {
                String resultKey = rme.getKey();
                Object resultValue = rme.getValue();

                if (resultKey != null && !ModelService.RESPONSE_MESSAGE.equals(resultKey)
                        && !ModelService.ERROR_MESSAGE.equals(resultKey)
                        && !ModelService.ERROR_MESSAGE_LIST.equals(resultKey)
                        && !ModelService.ERROR_MESSAGE_MAP.equals(resultKey)
                        && !ModelService.SUCCESS_MESSAGE.equals(resultKey)
                        && !ModelService.SUCCESS_MESSAGE_LIST.equals(resultKey)) {
                    request.setAttribute(resultKey, resultValue);
                }
            }
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("[Event Return]: " + responseString, MODULE);
        }
        return responseString;
    }

}
