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

import static org.ofbiz.base.util.UtilGenerics.checkList;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceAuthException;
import org.ofbiz.service.ServiceValidationException;
import org.ofbiz.webapp.control.ConfigXMLReader;
import org.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.ofbiz.webapp.control.ControlActivationEventListener;

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
     * @see org.ofbiz.webapp.event.EventHandler#invoke(ConfigXMLReader.Event, ConfigXMLReader.RequestMap, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
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

        if (UtilValidate.isEmpty(event.path)) {
            mode = SYNC;
        } else {
            mode = event.path;
        }

        // make sure we have a defined service to call
        serviceName = event.invoke;
        if (serviceName == null) {
            throw new EventHandlerException("Service name (eventMethod) cannot be null");
        }
        if (Debug.verboseOn()) Debug.logVerbose("[Set mode/service]: " + mode + "/" + serviceName, module);

        // some needed info for when running the service
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
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
            Debug.logVerbose("[Processing]: SERVICE Event", module);
            Debug.logVerbose("[Using delegator]: " + dispatcher.getDelegator().getDelegatorName(), module);
        }

        boolean isMultiPart = ServletFileUpload.isMultipartContent(request);
        Map<String, Object> multiPartMap = new HashMap<String, Object>();
        if (isMultiPart) {
            // get the http upload configuration
            String maxSizeStr = EntityUtilProperties.getPropertyValue("general", "http.upload.max.size", "-1", dctx.getDelegator());
            long maxUploadSize = -1;
            try {
                maxUploadSize = Long.parseLong(maxSizeStr);
            } catch (NumberFormatException e) {
                Debug.logError(e, "Unable to obtain the max upload size from general.properties; using default -1", module);
                maxUploadSize = -1;
            }
            // get the http size threshold configuration - files bigger than this will be
            // temporarly stored on disk during upload
            String sizeThresholdStr = EntityUtilProperties.getPropertyValue("general", "http.upload.max.sizethreshold", "10240", dctx.getDelegator());
            int sizeThreshold = 10240; // 10K
            try {
                sizeThreshold = Integer.parseInt(sizeThresholdStr);
            } catch (NumberFormatException e) {
                Debug.logError(e, "Unable to obtain the threshold size from general.properties; using default 10K", module);
                sizeThreshold = -1;
            }
            // directory used to temporarily store files that are larger than the configured size threshold
            String tmpUploadRepository = EntityUtilProperties.getPropertyValue("general", "http.upload.tmprepository", "runtime/tmp", dctx.getDelegator());
            String encoding = request.getCharacterEncoding();
            // check for multipart content types which may have uploaded items

            ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(sizeThreshold, new File(tmpUploadRepository)));

            // create the progress listener and add it to the session
            FileUploadProgressListener listener = new FileUploadProgressListener();
            upload.setProgressListener(listener);
            session.setAttribute("uploadProgressListener", listener);

            if (encoding != null) {
                upload.setHeaderEncoding(encoding);
            }
            upload.setSizeMax(maxUploadSize);

            List<FileItem> uploadedItems = null;
            try {
                uploadedItems = UtilGenerics.<FileItem>checkList(upload.parseRequest(request));
            } catch (FileUploadException e) {
                throw new EventHandlerException("Problems reading uploaded data", e);
            }
            if (uploadedItems != null) {
                for (FileItem item: uploadedItems) {
                    String fieldName = item.getFieldName();
                    //byte[] itemBytes = item.get();
                    /*
                    Debug.logInfo("Item Info [" + fieldName + "] : " + item.getName() + " / " + item.getSize() + " / " +
                            item.getContentType() + " FF: " + item.isFormField(), module);
                    */
                    if (item.isFormField() || item.getName() == null) {
                        if (multiPartMap.containsKey(fieldName)) {
                            Object mapValue = multiPartMap.get(fieldName);
                            if (mapValue instanceof List<?>) {
                                checkList(mapValue, Object.class).add(item.getString());
                            } else if (mapValue instanceof String) {
                                List<String> newList = new LinkedList<String>();
                                newList.add((String) mapValue);
                                newList.add(item.getString());
                                multiPartMap.put(fieldName, newList);
                            } else {
                                Debug.logWarning("Form field found [" + fieldName + "] which was not handled!", module);
                            }
                        } else {
                            if (encoding != null) {
                                try {
                                    multiPartMap.put(fieldName, item.getString(encoding));
                                } catch (java.io.UnsupportedEncodingException uee) {
                                    Debug.logError(uee, "Unsupported Encoding, using deafault", module);
                                    multiPartMap.put(fieldName, item.getString());
                                }
                            } else {
                                multiPartMap.put(fieldName, item.getString());
                            }
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
                        multiPartMap.put(fieldName, ByteBuffer.wrap(item.get()));
                        multiPartMap.put("_" + fieldName + "_size", Long.valueOf(item.getSize()));
                        multiPartMap.put("_" + fieldName + "_fileName", fileName);
                        multiPartMap.put("_" + fieldName + "_contentType", item.getContentType());
                    }
                }
            }
        }

        // store the multi-part map as an attribute so we can access the parameters
        request.setAttribute("multiPartMap", multiPartMap);

        Map<String, Object> rawParametersMap = UtilHttp.getCombinedMap(request);
        Set<String> urlOnlyParameterNames = UtilHttp.getUrlOnlyParameterMap(request).keySet();

        // we have a service and the model; build the context
        Map<String, Object> serviceContext = new HashMap<String, Object>();
        for (ModelParam modelParam: model.getInModelParamList()) {
            String name = modelParam.name;

            // don't include userLogin, that's taken care of below
            if ("userLogin".equals(name)) continue;
            // don't include locale, that is also taken care of below
            if ("locale".equals(name)) continue;
            // don't include timeZone, that is also taken care of below
            if ("timeZone".equals(name)) continue;

            Object value = null;
            if (UtilValidate.isNotEmpty(modelParam.stringMapPrefix)) {
                Map<String, Object> paramMap = UtilHttp.makeParamMapWithPrefix(request, multiPartMap, modelParam.stringMapPrefix, null);
                value = paramMap;
                if (Debug.verboseOn()) Debug.logVerbose("Set [" + modelParam.name + "]: " + paramMap, module);
            } else if (UtilValidate.isNotEmpty(modelParam.stringListSuffix)) {
                List<Object> paramList = UtilHttp.makeParamListWithSuffix(request, multiPartMap, modelParam.stringListSuffix, null);
                value = paramList;
            } else {
                // first check the multi-part map
                value = multiPartMap.get(name);

                // next check attributes; do this before parameters so that attribute which can be changed by code can override parameters which can't
                if (UtilValidate.isEmpty(value)) {
                    Object tempVal = request.getAttribute(UtilValidate.isEmpty(modelParam.requestAttributeName) ? name : modelParam.requestAttributeName);
                    if (tempVal != null) {
                        value = tempVal;
                    }
                }

                // check the request parameters
                if (UtilValidate.isEmpty(value)) {
                    ServiceEventHandler.checkSecureParameter(requestMap, urlOnlyParameterNames, name, session, serviceName, dctx.getDelegator());

                    // if the service modelParam has allow-html="any" then get this direct from the request instead of in the parameters Map so there will be no canonicalization possibly messing things up
                    if ("any".equals(modelParam.allowHtml)) {
                        value = request.getParameter(name);
                    } else {
                        // use the rawParametersMap from UtilHttp in order to also get pathInfo parameters, do canonicalization, etc
                        value = rawParametersMap.get(name);
                    }

                    // make any composite parameter data (e.g., from a set of parameters {name_c_date, name_c_hour, name_c_minutes})
                    if (value == null) {
                        value = UtilHttp.makeParamValueFromComposite(request, name, locale);
                    }
                }

                // then session
                if (UtilValidate.isEmpty(value)) {
                    Object tempVal = request.getSession().getAttribute(UtilValidate.isEmpty(modelParam.sessionAttributeName) ? name : modelParam.sessionAttributeName);
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
        List<Object> errorMessages = new LinkedList<Object>();
        serviceContext = model.makeValid(serviceContext, ModelService.IN_PARAM, true, errorMessages, timeZone, locale);
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

        // include the TimeZone object
        if (timeZone != null) {
            serviceContext.put("timeZone", timeZone);
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
            for (Map.Entry<String, Object> rme: result.entrySet()) {
                String resultKey = rme.getKey();
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

    public static void checkSecureParameter(RequestMap requestMap, Set<String> urlOnlyParameterNames, String name, HttpSession session, String serviceName, Delegator delegator) throws EventHandlerException {
        // special case for security: if this is a request-map defined as secure in controller.xml then only accept body parameters coming in, ie don't allow the insecure URL parameters
        // NOTE: the RequestHandler will check the HttpSerletRequest security to make sure it is secure if the request-map -> security -> https=true, but we can't just look at the request.isSecure() method here because it is allowed to send secure requests for request-map with https=false
        if (requestMap != null && requestMap.securityHttps) {
            if (urlOnlyParameterNames.contains(name)) {
                String errMsg = "Found URL parameter [" + name + "] passed to secure (https) request-map with uri ["
                    + requestMap.uri + "] with an event that calls service ["
                    + serviceName + "]; this is not allowed for security reasons! The data should be encrypted by making it part of the request body "
                    + "(a form field) instead of the request URL."
                    + " Moreover it would be kind if you could create a Jira sub-task of https://issues.apache.org/jira/browse/OFBIZ-2330 "
                    + "(check before if a sub-task for this error does not exist)."
                    + " If you are not sure how to create a Jira issue please have a look before at http://cwiki.apache.org/confluence/x/JIB2"
                    + " Thank you in advance for your help.";
                Debug.logError("=============== " + errMsg + "; In session [" + ControlActivationEventListener.showSessionId(session) + "]; Note that this can be changed using the service.http.parameters.require.encrypted property in the url.properties file", module);

                // the default here is true, so anything but N/n is true
                boolean requireEncryptedServiceWebParameters = !EntityUtilProperties.propertyValueEqualsIgnoreCase("url", "service.http.parameters.require.encrypted", "N", delegator);

                // NOTE: this forces service call event parameters to be in the body and not in the URL! can be issues with existing links, like Delete links or whatever, and those need to be changed to forms!
                if (requireEncryptedServiceWebParameters) {
                    throw new EventHandlerException(errMsg);
                }
            }
            // NOTTODO: may want to allow parameters that map to entity PK fields to be in the URL, but that might be a big security hole since there are certain security sensitive entities that are made of only PK fields, or that only need PK fields to function (like UserLoginSecurityGroup)
            // NOTTODO: we could allow URL parameters when it is not a POST (ie when !request.getMethod().equalsIgnoreCase("POST")), but that would open a security hole where sensitive parameters can be passed on the URL in a GET/etc and bypass this security constraint
        }
    }
}
