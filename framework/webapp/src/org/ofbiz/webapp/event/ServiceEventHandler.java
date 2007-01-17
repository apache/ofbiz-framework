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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceAuthException;
import org.ofbiz.service.ServiceValidationException;

/**
 * ServiceEventHandler - Service Event Handler
 */
public class ServiceEventHandler implements EventHandler {

    public static final String module = ServiceEventHandler.class.getName();

    public static final String SYNC = "sync";
    public static final String ASYNC = "async";

    /**
     * @see org.ofbiz.webapp.event.EventHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) throws EventHandlerException {
    }

    /**
     * @see org.ofbiz.webapp.event.EventHandler#invoke(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String invoke(String eventPath, String eventMethod, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
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

        if (eventPath == null || eventPath.length() == 0) {
            mode = SYNC;
        } else {
            mode = eventPath;
        }

        // nake sure we have a defined service to call
        serviceName = eventMethod;
        if (serviceName == null) {
            throw new EventHandlerException("Service name (eventMethod) cannot be null");
        }
        if (Debug.verboseOn()) Debug.logVerbose("[Set mode/service]: " + mode + "/" + serviceName, module);

        // some needed info for when running the service
        Locale locale = UtilHttp.getLocale(request);
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

        if (Debug.verboseOn()) Debug.logVerbose("[Processing]: SERVICE Event", module);
        if (Debug.verboseOn()) Debug.logVerbose("[Using delegator]: " + dispatcher.getDelegator().getDelegatorName(), module);

        // get the http upload configuration
        String maxSizeStr = UtilProperties.getPropertyValue("general.properties", "http.upload.max.size", "-1");
        long maxUploadSize = -1;
        try {
            maxUploadSize = Long.parseLong(maxSizeStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to obtain the max upload size from general.properties; using default -1", module);
            maxUploadSize = -1;
        }

        // check for multipart content types which may have uploaded items
        boolean isMultiPart = FileUpload.isMultipartContent(request);
        Map multiPartMap = new HashMap();
        if (isMultiPart) {
            DiskFileUpload upload = new DiskFileUpload();
            upload.setSizeMax(maxUploadSize);

            List uploadedItems = null;
            try {
                uploadedItems = upload.parseRequest(request);
            } catch (FileUploadException e) {
                throw new EventHandlerException("Problems reading uploaded data", e);
            }
            if (uploadedItems != null) {
                Iterator i = uploadedItems.iterator();
                while (i.hasNext()) {
                    FileItem item = (FileItem) i.next();
                    String fieldName = item.getFieldName();
                    //byte[] itemBytes = item.get();

                    //Debug.log("Item Info : " + item.getName() + " / " + item.getSize() + " / " + item.getContentType(), module);
                    if (item.isFormField() || item.getSize() == 0) {
                        if (multiPartMap.containsKey(fieldName)) {
                            Object mapValue = multiPartMap.get(fieldName);
                            if (mapValue instanceof List) {
                                ((List) mapValue).add(item.getString());
                            } else if (mapValue instanceof String) {
                                List newList = new ArrayList();
                                newList.add((String) mapValue);
                                newList.add(item.getString());
                                multiPartMap.put(fieldName, newList);
                            } else {
                                Debug.logWarning("Form field found [" + fieldName + "] which was not handled!", module);
                            }
                        } else {
                            multiPartMap.put(fieldName, item.getString());
                        }
                    } else {
                        String fileName = item.getName();
                        if (fileName.indexOf('\\') > -1 || fileName.indexOf('/') > -1) {
                            // get just the file name IE and other browsers also pass in the local path
                            int lastIndex = fileName.lastIndexOf('\\');
                            if (lastIndex == -1) {
                                lastIndex = fileName.lastIndexOf('/');
                            }
                            if (lastIndex > -1) {
                                fileName = fileName.substring(lastIndex + 1);
                            }
                        }
                        multiPartMap.put(fieldName, new ByteWrapper(item.get()));
                        multiPartMap.put("_" + fieldName + "_size", new Long(item.getSize()));
                        multiPartMap.put("_" + fieldName + "_fileName", fileName);
                        multiPartMap.put("_" + fieldName + "_contentType", item.getContentType());
                    }
                }
            }
        }

        // store the multi-part map as an attribute so we can access the parameters
        request.setAttribute("multiPartMap", multiPartMap);

        // we have a service and the model; build the context
        Map serviceContext = new HashMap();
        Iterator modelParmInIter = model.getInModelParamList().iterator();
        while (modelParmInIter.hasNext()) {
            ModelParam modelParam = (ModelParam) modelParmInIter.next();
            String name = modelParam.name;

            // don't include userLogin, that's taken care of below
            if ("userLogin".equals(name)) continue;
            // don't include locale, that is also taken care of below
            if ("locale".equals(name)) continue;

            Object value = null;
            if (modelParam.stringMapPrefix != null && modelParam.stringMapPrefix.length() > 0) {
                Map paramMap = UtilHttp.makeParamMapWithPrefix(request, multiPartMap, modelParam.stringMapPrefix, null);
                value = paramMap;
                if (Debug.verboseOn()) Debug.log("Set [" + modelParam.name + "]: " + paramMap, module);
            } else if (modelParam.stringListSuffix != null && modelParam.stringListSuffix.length() > 0) {
                List paramList = UtilHttp.makeParamListWithSuffix(request, multiPartMap, modelParam.stringListSuffix, null);
                value = paramList;
            } else {
                // first check the multi-part map
                value = multiPartMap.get(name);

                // next check attributes; do this before parameters so that attribute which can be changed by code can override parameters which can't
                if (UtilValidate.isEmpty(value)) {
                    Object tempVal = request.getAttribute(name);
                    if (tempVal != null) {
                        value = tempVal;
                    }
                }

                // check the request parameters
                if (UtilValidate.isEmpty(value)) {
                    // normal parameter data, which can either be a single value or an array of values
                    String[] paramArr = request.getParameterValues(name);
                    if (paramArr != null) {
                        if (paramArr.length > 1) {
                            value = Arrays.asList(paramArr);
                        } else {
                            value = paramArr[0];
                        }
                    }
                    // make any composite parameter data (e.g., from a set of parameters {name_c_date, name_c_hour, name_c_minutes})
                    if (value == null) {
                        value = UtilHttp.makeParamValueFromComposite(request, name, locale);
                    }
                }

                // then session
                if (UtilValidate.isEmpty(value)) {
                    Object tempVal = request.getSession().getAttribute(name);
                    if (tempVal != null) {
                        value = tempVal;
                    }
                }

                // no field found
                if (value == null) {
                    //still null, give up for this one
                    continue;
                }

                if (value instanceof String && ((String) value).length() == 0) {
                    // interpreting empty fields as null values for each in back end handling...
                    value = null;
                }
            }
            // set even if null so that values will get nulled in the db later on
            serviceContext.put(name, value);
        }

        // get only the parameters for this service - converted to proper type
        // TODO: pass in a list for error messages, like could not convert type or not a proper X, return immediately with messages if there are any
        List errorMessages = new LinkedList();
        serviceContext = model.makeValid(serviceContext, ModelService.IN_PARAM, true, errorMessages, locale);
        if (errorMessages.size() > 0) {
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

        // invoke the service
        Map result = null;
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
            Debug.logError(e, "Service invocation error", module);
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
            Iterator rmei = result.entrySet().iterator();
            while (rmei.hasNext()) {
                Map.Entry rme = (Map.Entry) rmei.next();
                String resultKey = (String) rme.getKey();
                Object resultValue = rme.getValue();

                if (resultKey != null && !ModelService.RESPONSE_MESSAGE.equals(resultKey) && !ModelService.ERROR_MESSAGE.equals(resultKey) &&
                        !ModelService.ERROR_MESSAGE_LIST.equals(resultKey) && !ModelService.ERROR_MESSAGE_MAP.equals(resultKey) &&
                        !ModelService.SUCCESS_MESSAGE.equals(resultKey) && !ModelService.SUCCESS_MESSAGE_LIST.equals(resultKey)) {
                    request.setAttribute(resultKey, resultValue);
                }
            }
        }

        if (Debug.verboseOn()) Debug.logVerbose("[Event Return]: " + responseString, module);
        return responseString;
    }
}
