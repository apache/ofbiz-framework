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
package org.ofbiz.webapp.control;

import static org.ofbiz.base.util.UtilGenerics.checkMap;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.start.Start;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.webapp.OfbizUrlBuilder;
import org.ofbiz.webapp.event.EventFactory;
import org.ofbiz.webapp.event.EventHandler;
import org.ofbiz.webapp.event.EventHandlerException;
import org.ofbiz.webapp.stats.ServerHitBin;
import org.ofbiz.webapp.view.ViewFactory;
import org.ofbiz.webapp.view.ViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.ofbiz.webapp.website.WebSiteProperties;
import org.ofbiz.webapp.website.WebSiteWorker;

/**
 * RequestHandler - Request Processor Object
 */
public class RequestHandler {

    public static final String module = RequestHandler.class.getName();
    private final String defaultStatusCodeString = UtilProperties.getPropertyValue("requestHandler", "status-code", "302");
    private final ViewFactory viewFactory;
    private final EventFactory eventFactory;
    private final URL controllerConfigURL;
    private final boolean trackServerHit;
    private final boolean trackVisit;
    private final String charset;

    public static RequestHandler getRequestHandler(ServletContext servletContext) {
        RequestHandler rh = (RequestHandler) servletContext.getAttribute("_REQUEST_HANDLER_");
        if (rh == null) {
            rh = new RequestHandler(servletContext);
            servletContext.setAttribute("_REQUEST_HANDLER_", rh);
        }
        return rh;
    }

    private RequestHandler(ServletContext context) {
        // init the ControllerConfig, but don't save it anywhere, just load it into the cache
        this.controllerConfigURL = ConfigXMLReader.getControllerConfigURL(context);
        try {
            ConfigXMLReader.getControllerConfig(this.controllerConfigURL);
        } catch (WebAppConfigurationException e) {
            // FIXME: controller.xml errors should throw an exception.
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
        }
        this.viewFactory = new ViewFactory(context, this.controllerConfigURL);
        this.eventFactory = new EventFactory(context, this.controllerConfigURL);

        this.trackServerHit = !"false".equalsIgnoreCase(context.getInitParameter("track-serverhit"));
        this.trackVisit = !"false".equalsIgnoreCase(context.getInitParameter("track-visit"));
        this.charset = context.getInitParameter("charset");
    }

    public ConfigXMLReader.ControllerConfig getControllerConfig() {
        try {
            return ConfigXMLReader.getControllerConfig(this.controllerConfigURL);
        } catch (WebAppConfigurationException e) {
            // FIXME: controller.xml errors should throw an exception.
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
        }
        return null;
    }

    public void doRequest(HttpServletRequest request, HttpServletResponse response, String requestUri) throws RequestHandlerException, RequestHandlerExceptionAllowExternalRequests {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        doRequest(request, response, requestUri, userLogin, delegator);
    }

    public void doRequest(HttpServletRequest request, HttpServletResponse response, String chain,
            GenericValue userLogin, Delegator delegator) throws RequestHandlerException, RequestHandlerExceptionAllowExternalRequests {

        final boolean throwRequestHandlerExceptionOnMissingLocalRequest = EntityUtilProperties.propertyValueEqualsIgnoreCase(
                "requestHandler", "throwRequestHandlerExceptionOnMissingLocalRequest", "Y", delegator);
        long startTime = System.currentTimeMillis();
        HttpSession session = request.getSession();

        // get the controllerConfig once for this method so we don't have to get it over and over inside the method
        ConfigXMLReader.ControllerConfig controllerConfig = this.getControllerConfig();
        Map<String, ConfigXMLReader.RequestMap> requestMapMap = null;
        String statusCodeString = null;
        try {
            requestMapMap = controllerConfig.getRequestMapMap();
            statusCodeString = controllerConfig.getStatusCode();
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
            throw new RequestHandlerException(e);
        }
        if (UtilValidate.isEmpty(statusCodeString)) {
            statusCodeString = defaultStatusCodeString;
        }

        // workaround if we are in the root webapp
        String cname = UtilHttp.getApplicationName(request);

        // Grab data from request object to process
        String defaultRequestUri = RequestHandler.getRequestUri(request.getPathInfo());
        if (request.getAttribute("targetRequestUri") == null) {
            if (request.getSession().getAttribute("_PREVIOUS_REQUEST_") != null) {
                request.setAttribute("targetRequestUri", request.getSession().getAttribute("_PREVIOUS_REQUEST_"));
            } else {
                request.setAttribute("targetRequestUri", "/" + defaultRequestUri);
            }
        }

        String overrideViewUri = RequestHandler.getOverrideViewUri(request.getPathInfo());

        String requestMissingErrorMessage = "Unknown request [" + defaultRequestUri + "]; this request does not exist or cannot be called directly.";
        ConfigXMLReader.RequestMap requestMap = null;
        if (defaultRequestUri != null) {
            requestMap = requestMapMap.get(defaultRequestUri);
        }
        // check for default request
        if (requestMap == null) {
            String defaultRequest;
            try {
                defaultRequest = controllerConfig.getDefaultRequest();
            } catch (WebAppConfigurationException e) {
                Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                throw new RequestHandlerException(e);
            }
            if (defaultRequest != null) { // required! to avoid a null pointer exception and generate a requesthandler exception if default request not found.
                requestMap = requestMapMap.get(defaultRequest);
            }
        }

        // check for override view
        if (overrideViewUri != null) {
            ConfigXMLReader.ViewMap viewMap;
            try {
                viewMap = getControllerConfig().getViewMapMap().get(overrideViewUri);
                if (viewMap == null) {
                    String defaultRequest = controllerConfig.getDefaultRequest();
                    if (defaultRequest != null) { // required! to avoid a null pointer exception and generate a requesthandler exception if default request not found.
                        requestMap = requestMapMap.get(defaultRequest);
                    }
                }
            } catch (WebAppConfigurationException e) {
                Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                throw new RequestHandlerException(e);
            }
        }

        // if no matching request is found in the controller, depending on throwRequestHandlerExceptionOnMissingLocalRequest
        //  we throw a RequestHandlerException or RequestHandlerExceptionAllowExternalRequests
        if (requestMap == null) {
            if (throwRequestHandlerExceptionOnMissingLocalRequest) throw new RequestHandlerException(requestMissingErrorMessage);
            else throw new RequestHandlerExceptionAllowExternalRequests();
         }

        String eventReturn = null;
        if (requestMap.metrics != null && requestMap.metrics.getThreshold() != 0.0 && requestMap.metrics.getTotalEvents() > 3 && requestMap.metrics.getThreshold() < requestMap.metrics.getServiceRate()) {
            eventReturn = "threshold-exceeded";
        }
        ConfigXMLReader.RequestMap originalRequestMap = requestMap; // Save this so we can update the correct performance metrics.


        boolean interruptRequest = false;

        // Check for chained request.
        if (chain != null) {
            String chainRequestUri = RequestHandler.getRequestUri(chain);
            requestMap = requestMapMap.get(chainRequestUri);
            if (requestMap == null) {
                throw new RequestHandlerException("Unknown chained request [" + chainRequestUri + "]; this request does not exist");
            }
            if (request.getAttribute("_POST_CHAIN_VIEW_") != null) {
                overrideViewUri = (String) request.getAttribute("_POST_CHAIN_VIEW_");
            } else {
                overrideViewUri = RequestHandler.getOverrideViewUri(chain);
            }
            if (overrideViewUri != null) {
                // put this in a request attribute early in case an event needs to access it
                // not using _POST_CHAIN_VIEW_ because it shouldn't be set unless the event execution is successful
                request.setAttribute("_CURRENT_CHAIN_VIEW_", overrideViewUri);
            }
            if (Debug.infoOn()) Debug.logInfo("[RequestHandler]: Chain in place: requestUri=" + chainRequestUri + " overrideViewUri=" + overrideViewUri + showSessionId(request), module);
        } else {
            // Check if X509 is required and we are not secure; throw exception
            if (!request.isSecure() && requestMap.securityCert) {
                throw new RequestHandlerException(requestMissingErrorMessage);
            }

            // Check to make sure we are allowed to access this request directly. (Also checks if this request is defined.)
            // If the request cannot be called, or is not defined, check and see if there is a default-request we can process
            if (!requestMap.securityDirectRequest) {
                String defaultRequest;
                try {
                    defaultRequest = controllerConfig.getDefaultRequest();
                } catch (WebAppConfigurationException e) {
                    Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                    throw new RequestHandlerException(e);
                }
                if (defaultRequest == null || !requestMapMap.get(defaultRequest).securityDirectRequest) {
                    // use the same message as if it was missing for security reasons, ie so can't tell if it is missing or direct request is not allowed
                    throw new RequestHandlerException(requestMissingErrorMessage);
                } else {
                    requestMap = requestMapMap.get(defaultRequest);
                }
            }
            // Check if we SHOULD be secure and are not.
            String forwardedProto = request.getHeader("X-Forwarded-Proto");
            boolean isForwardedSecure = UtilValidate.isNotEmpty(forwardedProto) && "HTTPS".equals(forwardedProto.toUpperCase());
            if ((!request.isSecure() && !isForwardedSecure) && requestMap.securityHttps) {
                // If the request method was POST then return an error to avoid problems with XSRF where the request may have come from another machine/program and had the same session ID but was not encrypted as it should have been (we used to let it pass to not lose data since it was too late to protect that data anyway)
                if (request.getMethod().equalsIgnoreCase("POST")) {
                    // we can't redirect with the body parameters, and for better security from XSRF, just return an error message
                    Locale locale = UtilHttp.getLocale(request);
                    String errMsg = UtilProperties.getMessage("WebappUiLabels", "requestHandler.InsecureFormPostToSecureRequest", locale);
                    Debug.logError("Got a insecure (non-https) form POST to a secure (http) request [" + requestMap.uri + "], returning error", module);

                    // see if HTTPS is enabled, if not then log a warning instead of throwing an exception
                    Boolean enableHttps = null;
                    String webSiteId = WebSiteWorker.getWebSiteId(request);
                    if (webSiteId != null) {
                        try {
                            GenericValue webSite = EntityQuery.use(delegator).from("WebSite").where("webSiteId", webSiteId).cache().queryOne();
                            if (webSite != null) enableHttps = webSite.getBoolean("enableHttps");
                        } catch (GenericEntityException e) {
                            Debug.logWarning(e, "Problems with WebSite entity; using global defaults", module);
                        }
                    }
                    if (enableHttps == null) {
                        enableHttps = EntityUtilProperties.propertyValueEqualsIgnoreCase("url", "port.https.enabled", "Y", delegator);
                    }

                    if (Boolean.FALSE.equals(enableHttps)) {
                        Debug.logWarning("HTTPS is disabled for this site, so we can't tell if this was encrypted or not which means if a form was POSTed and it was not over HTTPS we don't know, but it would be vulnerable to an XSRF and other attacks: " + errMsg, module);
                    } else {
                        throw new RequestHandlerException(errMsg);
                    }
                } else {
                    StringBuilder urlBuf = new StringBuilder();
                    urlBuf.append(request.getPathInfo());
                    if (request.getQueryString() != null) {
                        urlBuf.append("?").append(request.getQueryString());
                    }
                    String newUrl = RequestHandler.makeUrl(request, response, urlBuf.toString());
                    if (newUrl.toUpperCase().startsWith("HTTPS")) {
                        // if we are supposed to be secure, redirect secure.
                        callRedirect(newUrl, response, request, statusCodeString);
                        return;
                    }
                }
            }

            // Check for HTTPS client (x.509) security
            if (request.isSecure() && requestMap.securityCert) {
                X509Certificate[] clientCerts = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate"); // 2.2 spec
                if (clientCerts == null) {
                    clientCerts = (X509Certificate[]) request.getAttribute("javax.net.ssl.peer_certificates"); // 2.1 spec
                }
                if (clientCerts == null) {
                    Debug.logWarning("Received no client certificates from browser", module);
                }

                // check if the client has a valid certificate (in our db store)
                boolean foundTrustedCert = false;

                if (clientCerts == null) {
                    throw new RequestHandlerException(requestMissingErrorMessage);
                } else {
                    if (Debug.infoOn()) {
                        for (int i = 0; i < clientCerts.length; i++) {
                            Debug.logInfo(clientCerts[i].getSubjectX500Principal().getName(), module);
                        }
                    }

                    // check if this is a trusted cert
                    if (SSLUtil.isClientTrusted(clientCerts, null)) {
                        foundTrustedCert = true;
                    }
                }

                if (!foundTrustedCert) {
                    Debug.logWarning(requestMissingErrorMessage, module);
                    throw new RequestHandlerException(requestMissingErrorMessage);
                }
            }

            // If its the first visit run the first visit events.
            if (this.trackVisit(request) && session.getAttribute("_FIRST_VISIT_EVENTS_") == null) {
                if (Debug.infoOn())
                    Debug.logInfo("This is the first request in this visit." + showSessionId(request), module);
                session.setAttribute("_FIRST_VISIT_EVENTS_", "complete");
                try {
                    for (ConfigXMLReader.Event event: controllerConfig.getFirstVisitEventList().values()) {
                        try {
                            String returnString = this.runEvent(request, response, event, null, "firstvisit");
                            if (returnString == null || "none".equalsIgnoreCase(returnString)) {
                                interruptRequest = true;
                            } else if (!returnString.equalsIgnoreCase("success")) {
                                throw new EventHandlerException("First-Visit event did not return 'success'.");
                            }
                        } catch (EventHandlerException e) {
                            Debug.logError(e, module);
                        }
                    }
                } catch (WebAppConfigurationException e) {
                    Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                    throw new RequestHandlerException(e);
                }
            }

            // Invoke the pre-processor (but NOT in a chain)
            try {
                for (ConfigXMLReader.Event event: controllerConfig.getPreprocessorEventList().values()) {
                    try {
                        String returnString = this.runEvent(request, response, event, null, "preprocessor");
                        if (returnString == null || "none".equalsIgnoreCase(returnString)) {
                            interruptRequest = true;
                        } else if (!returnString.equalsIgnoreCase("success")) {
                            if (!returnString.contains(":_protect_:")) {
                                throw new EventHandlerException("Pre-Processor event [" + event.invoke + "] did not return 'success'.");
                            } else { // protect the view normally rendered and redirect to error response view
                                returnString = returnString.replace(":_protect_:", "");
                                if (returnString.length() > 0) {
                                    request.setAttribute("_ERROR_MESSAGE_", returnString);
                                }
                                eventReturn = null;
                                // check to see if there is a "protect" response, if so it's ok else show the default_error_response_view
                                if (!requestMap.requestResponseMap.containsKey("protect")) {
                                    String protectView = controllerConfig.getProtectView();
                                    if (protectView != null) {
                                        overrideViewUri = protectView;
                                    } else {
                                        overrideViewUri = EntityUtilProperties.getPropertyValue("security", "default.error.response.view", delegator);
                                        overrideViewUri = overrideViewUri.replace("view:", "");
                                        if ("none:".equals(overrideViewUri)) {
                                            interruptRequest = true;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (EventHandlerException e) {
                        Debug.logError(e, module);
                    }
                }
            } catch (WebAppConfigurationException e) {
                Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                throw new RequestHandlerException(e);
            }
        }

        // Pre-Processor/First-Visit event(s) can interrupt the flow by returning null.
        // Warning: this could cause problems if more then one event attempts to return a response.
        if (interruptRequest) {
            if (Debug.infoOn()) Debug.logInfo("[Pre-Processor Interrupted Request, not running: [" + requestMap.uri + "]. " + showSessionId(request), module);
            return;
        }

        if (Debug.verboseOn()) Debug.logVerbose("[Processing Request]: " + requestMap.uri + showSessionId(request), module);
        request.setAttribute("thisRequestUri", requestMap.uri); // store the actual request URI


        // Perform security check.
        if (requestMap.securityAuth) {
            // Invoke the security handler
            // catch exceptions and throw RequestHandlerException if failed.
            if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler]: AuthRequired. Running security check. " + showSessionId(request), module);
            ConfigXMLReader.Event checkLoginEvent = requestMapMap.get("checkLogin").event;
            String checkLoginReturnString = null;

            try {
                checkLoginReturnString = this.runEvent(request, response, checkLoginEvent, null, "security-auth");
            } catch (EventHandlerException e) {
                throw new RequestHandlerException(e.getMessage(), e);
            }
            if (!"success".equalsIgnoreCase(checkLoginReturnString)) {
                // previous URL already saved by event, so just do as the return says...
                eventReturn = checkLoginReturnString;
                // if the request is an ajax request we don't want to return the default login check
                if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                    requestMap = requestMapMap.get("checkLogin");
                } else {
                    requestMap = requestMapMap.get("ajaxCheckLogin");
                }
            }
        }

        // after security check but before running the event, see if a post-login redirect has completed and we have data from the pre-login request form to use now
        // we know this is the case if the _PREVIOUS_PARAM_MAP_ attribute is there, but the _PREVIOUS_REQUEST_ attribute has already been removed
        if (request.getSession().getAttribute("_PREVIOUS_PARAM_MAP_FORM_") != null && request.getSession().getAttribute("_PREVIOUS_REQUEST_") == null) {
            Map<String, Object> previousParamMap = UtilGenerics.checkMap(request.getSession().getAttribute("_PREVIOUS_PARAM_MAP_FORM_"), String.class, Object.class);
            for (Map.Entry<String, Object> previousParamEntry: previousParamMap.entrySet()) {
                request.setAttribute(previousParamEntry.getKey(), previousParamEntry.getValue());
            }

            // to avoid this data being included again, now remove the _PREVIOUS_PARAM_MAP_ attribute
            request.getSession().removeAttribute("_PREVIOUS_PARAM_MAP_FORM_");
        }

        // now we can start looking for the next request response to use
        ConfigXMLReader.RequestResponse nextRequestResponse = null;

        // Invoke the defined event (unless login failed)
        if (eventReturn == null && requestMap.event != null) {
            if (requestMap.event.type != null && requestMap.event.path != null && requestMap.event.invoke != null) {
                try {
                    long eventStartTime = System.currentTimeMillis();

                    // run the request event
                    eventReturn = this.runEvent(request, response, requestMap.event, requestMap, "request");
                                        
                    if (requestMap.event.metrics != null) {
                        requestMap.event.metrics.recordServiceRate(1, System.currentTimeMillis() - startTime);
                    }                    

                    // save the server hit for the request event
                    if (this.trackStats(request)) {
                        ServerHitBin.countEvent(cname + "." + requestMap.event.invoke, request, eventStartTime,
                                System.currentTimeMillis() - eventStartTime, userLogin);
                    }

                    // set the default event return
                    if (eventReturn == null) {
                        nextRequestResponse = ConfigXMLReader.emptyNoneRequestResponse;
                    }
                } catch (EventHandlerException e) {
                    // check to see if there is an "error" response, if so go there and make an request error message
                    if (requestMap.requestResponseMap.containsKey("error")) {
                        eventReturn = "error";
                        Locale locale = UtilHttp.getLocale(request);
                        String errMsg = UtilProperties.getMessage("WebappUiLabels", "requestHandler.error_call_event", locale);
                        request.setAttribute("_ERROR_MESSAGE_", errMsg + ": " + e.toString());
                    } else {
                        throw new RequestHandlerException("Error calling event and no error response was specified", e);
                    }
                }
            }
        }

        // Process the eventReturn
        // at this point eventReturnString is finalized, so get the RequestResponse
        ConfigXMLReader.RequestResponse eventReturnBasedRequestResponse;
        if (eventReturn == null) {
            eventReturnBasedRequestResponse = null;
        } else {
            eventReturnBasedRequestResponse = requestMap.requestResponseMap.get(eventReturn);
            if (eventReturnBasedRequestResponse == null && eventReturn.equals("none")) {
                eventReturnBasedRequestResponse = ConfigXMLReader.emptyNoneRequestResponse;
            }
        }
        if (eventReturnBasedRequestResponse != null) {
            //String eventReturnBasedResponse = requestResponse.value;
            if (Debug.verboseOn()) Debug.logVerbose("[Response Qualified]: " + eventReturnBasedRequestResponse.name + ", " + eventReturnBasedRequestResponse.type + ":" + eventReturnBasedRequestResponse.value + showSessionId(request), module);

            // If error, then display more error messages:
            if ("error".equals(eventReturnBasedRequestResponse.name)) {
                if (Debug.errorOn()) {
                    String errorMessageHeader = "Request " + requestMap.uri + " caused an error with the following message: ";
                    if (request.getAttribute("_ERROR_MESSAGE_") != null) {
                        Debug.logError(errorMessageHeader + request.getAttribute("_ERROR_MESSAGE_"), module);
                    }
                    if (request.getAttribute("_ERROR_MESSAGE_LIST_") != null) {
                        Debug.logError(errorMessageHeader + request.getAttribute("_ERROR_MESSAGE_LIST_"), module);
                    }
                }
            }
        } else if (eventReturn != null) {
            // only log this warning if there is an eventReturn (ie skip if no event, etc)
            Debug.logWarning("Could not find response in request [" + requestMap.uri + "] for event return [" + eventReturn + "]", module);
        }

        // Set the next view (don't use event return if success, default to nextView (which is set to eventReturn later if null); also even if success if it is a type "none" response ignore the nextView, ie use the eventReturn)
        if (eventReturnBasedRequestResponse != null && (!"success".equals(eventReturnBasedRequestResponse.name) || "none".equals(eventReturnBasedRequestResponse.type))) nextRequestResponse = eventReturnBasedRequestResponse;

        // get the previous request info
        String previousRequest = (String) request.getSession().getAttribute("_PREVIOUS_REQUEST_");
        String loginPass = (String) request.getAttribute("_LOGIN_PASSED_");

        // restore previous redirected request's attribute, so redirected page can display previous request's error msg etc.
        String preReqAttStr = (String) request.getSession().getAttribute("_REQ_ATTR_MAP_");
        Map<String, Object> previousRequestAttrMap = null;
        if (preReqAttStr != null) {
            previousRequestAttrMap = new HashMap<String, Object>();
            request.getSession().removeAttribute("_REQ_ATTR_MAP_");
            byte[] reqAttrMapBytes = StringUtil.fromHexString(preReqAttStr);
            Map<String, Object> preRequestMap = checkMap(UtilObject.getObject(reqAttrMapBytes), String.class, Object.class);
            if (UtilValidate.isNotEmpty(preRequestMap)) {
                for (Map.Entry<String, Object> entry: preRequestMap.entrySet()) {
                    String key = entry.getKey();
                    if ("_ERROR_MESSAGE_LIST_".equals(key) || "_ERROR_MESSAGE_MAP_".equals(key) || "_ERROR_MESSAGE_".equals(key) ||
                            "_EVENT_MESSAGE_LIST_".equals(key) || "_EVENT_MESSAGE_".equals(key)) {
                        request.setAttribute(key, entry.getValue());
                        previousRequestAttrMap.put(key, entry.getValue());
                   }
                }
            }
        }

        if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler]: previousRequest - " + previousRequest + " (" + loginPass + ")" + showSessionId(request), module);

        // if previous request exists, and a login just succeeded, do that now.
        if (previousRequest != null && loginPass != null && loginPass.equalsIgnoreCase("TRUE")) {
            request.getSession().removeAttribute("_PREVIOUS_REQUEST_");
            // special case to avoid login/logout looping: if request was "logout" before the login, change to null for default success view; do the same for "login" to avoid going back to the same page
            if ("logout".equals(previousRequest) || "/logout".equals(previousRequest) || "login".equals(previousRequest) || "/login".equals(previousRequest) || "checkLogin".equals(previousRequest) || "/checkLogin".equals(previousRequest) || "/checkLogin/login".equals(previousRequest)) {
                Debug.logWarning("Found special _PREVIOUS_REQUEST_ of [" + previousRequest + "], setting to null to avoid problems, not running request again", module);
            } else {
                if (Debug.infoOn()) Debug.logInfo("[Doing Previous Request]: " + previousRequest + showSessionId(request), module);

                // note that the previous form parameters are not setup (only the URL ones here), they will be found in the session later and handled when the old request redirect comes back
                Map<String, Object> previousParamMap = UtilGenerics.checkMap(request.getSession().getAttribute("_PREVIOUS_PARAM_MAP_URL_"), String.class, Object.class);
                String queryString = UtilHttp.urlEncodeArgs(previousParamMap, false);
                String redirectTarget = previousRequest;
                if (UtilValidate.isNotEmpty(queryString)) {
                    redirectTarget += "?" + queryString;
                }
                
                callRedirect(makeLink(request, response, redirectTarget), response, request, statusCodeString);
                return;
            }
        }

        ConfigXMLReader.RequestResponse successResponse = requestMap.requestResponseMap.get("success");
        if ((eventReturn == null || "success".equals(eventReturn)) && successResponse != null && "request".equals(successResponse.type)) {
            // chains will override any url defined views; but we will save the view for the very end
            if (UtilValidate.isNotEmpty(overrideViewUri)) {
                request.setAttribute("_POST_CHAIN_VIEW_", overrideViewUri);
            }
            nextRequestResponse = successResponse;
        }

        // Make sure we have some sort of response to go to
        if (nextRequestResponse == null) nextRequestResponse = successResponse;

        if (nextRequestResponse == null) {
            throw new RequestHandlerException("Illegal response; handler could not process request [" + requestMap.uri + "] and event return [" + eventReturn + "].");
        }

        if (Debug.verboseOn()) Debug.logVerbose("[Event Response Selected]  type=" + nextRequestResponse.type + ", value=" + nextRequestResponse.value + ". " + showSessionId(request), module);

        // ========== Handle the responses - chains/views ==========

        // if the request has the save-last-view attribute set, save it now before the view can be rendered or other chain done so that the _LAST* session attributes will represent the previous request
        if (nextRequestResponse.saveLastView) {
            // Debug.logInfo("======save last view: " + session.getAttribute("_LAST_VIEW_NAME_"));
            String lastViewName = (String) session.getAttribute("_LAST_VIEW_NAME_");
            // Do not save the view if the last view is the same as the current view and saveCurrentView is false
            if (!(!nextRequestResponse.saveCurrentView && "view".equals(nextRequestResponse.type) && nextRequestResponse.value.equals(lastViewName))) {
                session.setAttribute("_SAVED_VIEW_NAME_", session.getAttribute("_LAST_VIEW_NAME_"));
                session.setAttribute("_SAVED_VIEW_PARAMS_", session.getAttribute("_LAST_VIEW_PARAMS_"));
            }
        }
        String saveName = null;
        if (nextRequestResponse.saveCurrentView) { saveName = "SAVED"; }
        if (nextRequestResponse.saveHomeView) { saveName = "HOME"; }

        if ("request".equals(nextRequestResponse.type)) {
            // chained request
            Debug.logInfo("[RequestHandler.doRequest]: Response is a chained request." + showSessionId(request), module);
            doRequest(request, response, nextRequestResponse.value, userLogin, delegator);
        } else {
            // ======== handle views ========

            // first invoke the post-processor events.
            try {
                for (ConfigXMLReader.Event event: controllerConfig.getPostprocessorEventList().values()) {
                    try {
                        String returnString = this.runEvent(request, response, event, requestMap, "postprocessor");
                        if (returnString != null && !returnString.equalsIgnoreCase("success")) {
                            throw new EventHandlerException("Post-Processor event did not return 'success'.");
                        }
                    } catch (EventHandlerException e) {
                        Debug.logError(e, module);
                    }
                }
            } catch (WebAppConfigurationException e) {
                Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                throw new RequestHandlerException(e);
            }

            String responseStatusCode  = nextRequestResponse.statusCode;
            if(UtilValidate.isNotEmpty(responseStatusCode))
                statusCodeString = responseStatusCode;            
            
            
            if ("url".equals(nextRequestResponse.type)) {
                if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler.doRequest]: Response is a URL redirect." + showSessionId(request), module);
                callRedirect(nextRequestResponse.value, response, request, statusCodeString);
            } else if ("cross-redirect".equals(nextRequestResponse.type)) {
                // check for a cross-application redirect
                if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler.doRequest]: Response is a Cross-Application redirect." + showSessionId(request), module);
                String url = nextRequestResponse.value.startsWith("/") ? nextRequestResponse.value : "/" + nextRequestResponse.value;
                callRedirect(url + this.makeQueryString(request, nextRequestResponse), response, request, statusCodeString);
            } else if ("request-redirect".equals(nextRequestResponse.type)) {
                if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler.doRequest]: Response is a Request redirect." + showSessionId(request), module);
                callRedirect(makeLinkWithQueryString(request, response, "/" + nextRequestResponse.value, nextRequestResponse), response, request, statusCodeString);
            } else if ("request-redirect-noparam".equals(nextRequestResponse.type)) {
                if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler.doRequest]: Response is a Request redirect with no parameters." + showSessionId(request), module);
                callRedirect(makeLink(request, response, nextRequestResponse.value), response, request, statusCodeString);
            } else if ("view".equals(nextRequestResponse.type)) {
                if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler.doRequest]: Response is a view." + showSessionId(request), module);

                // check for an override view, only used if "success" = eventReturn
                String viewName = (UtilValidate.isNotEmpty(overrideViewUri) && (eventReturn == null || "success".equals(eventReturn))) ? overrideViewUri : nextRequestResponse.value;
                renderView(viewName, requestMap.securityExternalView, request, response, saveName);
            } else if ("view-last".equals(nextRequestResponse.type)) {
                if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler.doRequest]: Response is a view." + showSessionId(request), module);

                // check for an override view, only used if "success" = eventReturn
                String viewName = (UtilValidate.isNotEmpty(overrideViewUri) && (eventReturn == null || "success".equals(eventReturn))) ? overrideViewUri : nextRequestResponse.value;

                // as a further override, look for the _SAVED and then _HOME and then _LAST session attributes
                Map<String, Object> urlParams = null;
                if (session.getAttribute("_SAVED_VIEW_NAME_") != null) {
                    viewName = (String) session.getAttribute("_SAVED_VIEW_NAME_");
                    urlParams = UtilGenerics.<String, Object>checkMap(session.getAttribute("_SAVED_VIEW_PARAMS_"));
                } else if (session.getAttribute("_HOME_VIEW_NAME_") != null) {
                    viewName = (String) session.getAttribute("_HOME_VIEW_NAME_");
                    urlParams = UtilGenerics.<String, Object>checkMap(session.getAttribute("_HOME_VIEW_PARAMS_"));
                } else if (session.getAttribute("_LAST_VIEW_NAME_") != null) {
                    viewName = (String) session.getAttribute("_LAST_VIEW_NAME_");
                    urlParams = UtilGenerics.<String, Object>checkMap(session.getAttribute("_LAST_VIEW_PARAMS_"));
                } else if (UtilValidate.isNotEmpty(nextRequestResponse.value)) {
                    viewName = nextRequestResponse.value;
                }
                if (UtilValidate.isEmpty(viewName) && UtilValidate.isNotEmpty(nextRequestResponse.value)) {
                    viewName = nextRequestResponse.value;
                }
                if (urlParams != null) {
                    for (Map.Entry<String, Object> urlParamEntry: urlParams.entrySet()) {
                        String key = urlParamEntry.getKey();
                        // Don't overwrite messages coming from the current event
                        if (!("_EVENT_MESSAGE_".equals(key) || "_ERROR_MESSAGE_".equals(key)
                                || "_EVENT_MESSAGE_LIST_".equals(key) || "_ERROR_MESSAGE_LIST_".equals(key))) {
                            request.setAttribute(key, urlParamEntry.getValue());
                        }
                    }
                }
                renderView(viewName, requestMap.securityExternalView, request, response, null);
            } else if ("view-last-noparam".equals(nextRequestResponse.type)) {
                 if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler.doRequest]: Response is a view." + showSessionId(request), module);

                 // check for an override view, only used if "success" = eventReturn
                 String viewName = (UtilValidate.isNotEmpty(overrideViewUri) && (eventReturn == null || "success".equals(eventReturn))) ? overrideViewUri : nextRequestResponse.value;

                 // as a further override, look for the _SAVED and then _HOME and then _LAST session attributes
                 if (session.getAttribute("_SAVED_VIEW_NAME_") != null) {
                     viewName = (String) session.getAttribute("_SAVED_VIEW_NAME_");
                 } else if (session.getAttribute("_HOME_VIEW_NAME_") != null) {
                     viewName = (String) session.getAttribute("_HOME_VIEW_NAME_");
                 } else if (session.getAttribute("_LAST_VIEW_NAME_") != null) {
                     viewName = (String) session.getAttribute("_LAST_VIEW_NAME_");
                 } else if (UtilValidate.isNotEmpty(nextRequestResponse.value)) {
                     viewName = nextRequestResponse.value;
                 }
                 renderView(viewName, requestMap.securityExternalView, request, response, null);
            } else if ("view-home".equals(nextRequestResponse.type)) {
                if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler.doRequest]: Response is a view." + showSessionId(request), module);

                // check for an override view, only used if "success" = eventReturn
                String viewName = (UtilValidate.isNotEmpty(overrideViewUri) && (eventReturn == null || "success".equals(eventReturn))) ? overrideViewUri : nextRequestResponse.value;

                // as a further override, look for the _HOME session attributes
                Map<String, Object> urlParams = null;
                if (session.getAttribute("_HOME_VIEW_NAME_") != null) {
                    viewName = (String) session.getAttribute("_HOME_VIEW_NAME_");
                    urlParams = UtilGenerics.<String, Object>checkMap(session.getAttribute("_HOME_VIEW_PARAMS_"));
                }
                if (urlParams != null) {
                    for (Map.Entry<String, Object> urlParamEntry: urlParams.entrySet()) {
                        request.setAttribute(urlParamEntry.getKey(), urlParamEntry.getValue());
                    }
                }
                renderView(viewName, requestMap.securityExternalView, request, response, null);
            } else if ("none".equals(nextRequestResponse.type)) {
                // no view to render (meaning the return was processed by the event)
                if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler.doRequest]: Response is handled by the event." + showSessionId(request), module);
            }
        }
        if (originalRequestMap.metrics != null) {
            originalRequestMap.metrics.recordServiceRate(1, System.currentTimeMillis() - startTime);
        }
    }

    /** Find the event handler and invoke an event. */
    public String runEvent(HttpServletRequest request, HttpServletResponse response,
            ConfigXMLReader.Event event, ConfigXMLReader.RequestMap requestMap, String trigger) throws EventHandlerException {
        EventHandler eventHandler = eventFactory.getEventHandler(event.type);
        String eventReturn = eventHandler.invoke(event, requestMap, request, response);
        if (Debug.verboseOn() || (Debug.infoOn() && "request".equals(trigger))) Debug.logInfo("Ran Event [" + event.type + ":" + event.path + "#" + event.invoke + "] from [" + trigger + "], result is [" + eventReturn + "]", module);
        return eventReturn;
    }

    /** Returns the default error page for this request. */
    public String getDefaultErrorPage(HttpServletRequest request) {
        String errorpage = null;
        try {
            errorpage = getControllerConfig().getErrorpage();
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
        }
        if (UtilValidate.isNotEmpty(errorpage)) return errorpage;
        return "/error/error.jsp";
    }

    /** Returns the default status-code for this request. */
    public String getStatusCode(HttpServletRequest request) {
        String statusCode = null;
        try {
            statusCode = getControllerConfig().getStatusCode();
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
        }
        if (UtilValidate.isNotEmpty(statusCode)) return statusCode;
        return null;
    }

    /** Returns the ViewFactory Object. */
    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    /** Returns the EventFactory Object. */
    public EventFactory getEventFactory() {
        return eventFactory;
    }

    public static String getRequestUri(String path) {
        List<String> pathInfo = StringUtil.split(path, "/");
        if (UtilValidate.isEmpty(pathInfo)) {
            Debug.logWarning("Got nothing when splitting URI: " + path, module);
            return null;
        }
        if (pathInfo.get(0).indexOf('?') > -1) {
            return pathInfo.get(0).substring(0, pathInfo.get(0).indexOf('?'));
        } else {
            return pathInfo.get(0);
        }
    }

    public static String getOverrideViewUri(String path) {
        List<String> pathItemList = StringUtil.split(path, "/");
        if (pathItemList == null) {
            return null;
        }
        pathItemList = pathItemList.subList(1, pathItemList.size());

        String nextPage = null;
        for (String pathItem: pathItemList) {
            if (pathItem.indexOf('~') != 0) {
                if (pathItem.indexOf('?') > -1) {
                    pathItem = pathItem.substring(0, pathItem.indexOf('?'));
                }
                nextPage = (nextPage == null ? pathItem : nextPage + "/" + pathItem);
            }
        }
        return nextPage;
    }

    private void callRedirect(String url, HttpServletResponse resp, HttpServletRequest req, String statusCodeString) throws RequestHandlerException {
        if (Debug.infoOn()) Debug.logInfo("Sending redirect to: [" + url + "]. " + showSessionId(req), module);
        // set the attributes in the session so we can access it.
        Enumeration<String> attributeNameEnum = UtilGenerics.cast(req.getAttributeNames());
        Map<String, Object> reqAttrMap = new HashMap<String, Object>();
        Integer statusCode;
        try {
            statusCode = Integer.valueOf(statusCodeString);
        } catch (NumberFormatException e) {
            statusCode = 303;
        } 
        while (attributeNameEnum.hasMoreElements()) {
            String name = attributeNameEnum.nextElement();
            Object obj = req.getAttribute(name);
            if (obj instanceof Serializable) {
                reqAttrMap.put(name, obj);
            }
        }
        if (reqAttrMap.size() > 0) {
            reqAttrMap.remove("_REQUEST_HANDLER_");  // RequestHandler is not serializable and must be removed first.  See http://issues.apache.org/jira/browse/OFBIZ-750
            byte[] reqAttrMapBytes = UtilObject.getBytes(reqAttrMap);
            if (reqAttrMapBytes != null) {
                req.getSession().setAttribute("_REQ_ATTR_MAP_", StringUtil.toHexString(reqAttrMapBytes));
            }
        }

        // send the redirect
        try {            
            resp.setStatus(statusCode);
            resp.setHeader("Location", url);
            resp.setHeader("Connection", "close");
        } catch (IllegalStateException ise) {
            throw new RequestHandlerException(ise.getMessage(), ise);
        }
    }
    private void renderView(String view, boolean allowExtView, HttpServletRequest req, HttpServletResponse resp, String saveName) throws RequestHandlerException {
        GenericValue userLogin = (GenericValue) req.getSession().getAttribute("userLogin");
        // workaround if we are in the root webapp
        String cname = UtilHttp.getApplicationName(req);
        String oldView = view;

        if (UtilValidate.isNotEmpty(view) && view.charAt(0) == '/') {
            view = view.substring(1);
        }

        // if the view name starts with the control servlet name and a /, then it was an
        // attempt to override the default view with a call back into the control servlet,
        // so just get the target view name and use that
        String servletName = req.getServletPath();
        if (UtilValidate.isNotEmpty(servletName) && servletName.length() > 1 || servletName.startsWith("/")) {
            servletName = servletName.substring(1);
        }
        
        if (Debug.infoOn()) Debug.logInfo("Rendering View [" + view + "]. " + showSessionId(req), module);
        if (view.startsWith(servletName + "/")) {
            view = view.substring(servletName.length() + 1);
            if (Debug.infoOn()) Debug.logInfo("a manual control servlet request was received, removing control servlet path resulting in: view=" + view, module);
        }

        if (Debug.verboseOn()) Debug.logVerbose("[Getting View Map]: " + view + showSessionId(req), module);

        // before mapping the view, set a request attribute so we know where we are
        req.setAttribute("_CURRENT_VIEW_", view);

        // save the view in the session for the last view, plus the parameters Map (can use all parameters as they will never go into a URL, will only stay in the session and extra data will be ignored as we won't go to the original request just the view); note that this is saved after the request/view processing has finished so when those run they will get the value from the previous request
        Map<String, Object> paramMap = UtilHttp.getParameterMap(req);
        // add in the attributes as well so everything needed for the rendering context will be in place if/when we get back to this view
        paramMap.putAll(UtilHttp.getAttributeMap(req));
        UtilMisc.makeMapSerializable(paramMap);
        if (paramMap.containsKey("_LAST_VIEW_NAME_")) { // Used by lookups to keep the real view (request)
            req.getSession().setAttribute("_LAST_VIEW_NAME_", paramMap.get("_LAST_VIEW_NAME_"));
        } else {
            req.getSession().setAttribute("_LAST_VIEW_NAME_", view);
        }
        req.getSession().setAttribute("_LAST_VIEW_PARAMS_", paramMap);

        if ("SAVED".equals(saveName)) {
            //Debug.logInfo("======save current view: " + view);
            req.getSession().setAttribute("_SAVED_VIEW_NAME_", view);
            req.getSession().setAttribute("_SAVED_VIEW_PARAMS_", paramMap);
        }

        if ("HOME".equals(saveName)) {
            //Debug.logInfo("======save home view: " + view);
            req.getSession().setAttribute("_HOME_VIEW_NAME_", view);
            req.getSession().setAttribute("_HOME_VIEW_PARAMS_", paramMap);
            // clear other saved views
            req.getSession().removeAttribute("_SAVED_VIEW_NAME_");
            req.getSession().removeAttribute("_SAVED_VIEW_PARAMS_");
        }

        ConfigXMLReader.ViewMap viewMap = null;
        try {
            viewMap = (view == null ? null : getControllerConfig().getViewMapMap().get(view));
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
            throw new RequestHandlerException(e);
        }
        if (viewMap == null) {
            throw new RequestHandlerException("No definition found for view with name [" + view + "]");
        }

        String nextPage;

        if (viewMap.page == null) {
            if (!allowExtView) {
                throw new RequestHandlerException("No view to render.");
            } else {
                nextPage = "/" + oldView;
            }
        } else {
            nextPage = viewMap.page;
        }

        if (Debug.verboseOn()) Debug.logVerbose("[Mapped To]: " + nextPage + showSessionId(req), module);

        long viewStartTime = System.currentTimeMillis();

        // setup character encoding and content type
        String charset = UtilFormatOut.checkEmpty(this.charset, req.getCharacterEncoding(), "UTF-8");

        String viewCharset = viewMap.encoding;
        //NOTE: if the viewCharset is "none" then no charset will be used
        if (UtilValidate.isNotEmpty(viewCharset)) {
            charset = viewCharset;
        }

        if (!"none".equals(charset)) {
            try {
                req.setCharacterEncoding(charset);
            } catch (UnsupportedEncodingException e) {
                throw new RequestHandlerException("Could not set character encoding to " + charset, e);
            } catch (IllegalStateException e) {
                Debug.logInfo(e, "Could not set character encoding to " + charset + ", something has probably already committed the stream", module);
            }
        }

        // setup content type
        String contentType = "text/html";
        String viewContentType = viewMap.contentType;
        if (UtilValidate.isNotEmpty(viewContentType)) {
            contentType = viewContentType;
        }

        if (charset.length() > 0 && !"none".equals(charset)) {
            resp.setContentType(contentType + "; charset=" + charset);
        } else {
            resp.setContentType(contentType);
        }

        if (Debug.verboseOn()) Debug.logVerbose("The ContentType for the " + view + " view is: " + contentType, module);

        boolean viewNoCache = viewMap.noCache;
        if (viewNoCache) {
           UtilHttp.setResponseBrowserProxyNoCache(resp);
           if (Debug.verboseOn()) Debug.logVerbose("Sending no-cache headers for view [" + nextPage + "]", module);
        }
        
        String xFrameOption = viewMap.xFrameOption;
        // default to sameorigin
        if (UtilValidate.isNotEmpty(xFrameOption)) {
            resp.addHeader("x-frame-options", xFrameOption);
        } else {
            resp.addHeader("x-frame-options", "sameorigin");
        }

        String strictTransportSecurity = viewMap.strictTransportSecurity;
        // default to "max-age=31536000; includeSubDomains" 31536000 secs = 1 year
        if (UtilValidate.isNotEmpty(strictTransportSecurity)) {
            if (!"none".equals(strictTransportSecurity)) {
                resp.addHeader("strict-transport-security", strictTransportSecurity);
            }
        } else {
            if (EntityUtilProperties.getPropertyAsBoolean("requestHandler", "strict-transport-security", true)) { // FIXME later pass req.getAttribute("delegator") as last argument
                resp.addHeader("strict-transport-security", "max-age=31536000; includeSubDomains");
            }
        }
        
        //The only x-content-type-options defined value, "nosniff", prevents Internet Explorer from MIME-sniffing a response away from the declared content-type. 
        // This also applies to Google Chrome, when downloading extensions.
        resp.addHeader("x-content-type-options", "nosniff");
        
        // This header enables the Cross-site scripting (XSS) filter built into most recent web browsers. 
        // It's usually enabled by default anyway, so the role of this header is to re-enable the filter for this particular website if it was disabled by the user. 
        // This header is supported in IE 8+, and in Chrome (not sure which versions). The anti-XSS filter was added in Chrome 4. Its unknown if that version honored this header.
        // FireFox has still an open bug entry and "offers" only the noscript plugin
        // https://wiki.mozilla.org/Security/Features/XSS_Filter 
        // https://bugzilla.mozilla.org/show_bug.cgi?id=528661
        resp.addHeader("X-XSS-Protection","1; mode=block"); 

        try {
            if (Debug.verboseOn()) Debug.logVerbose("Rendering view [" + nextPage + "] of type [" + viewMap.type + "]", module);
            ViewHandler vh = viewFactory.getViewHandler(viewMap.type);
            vh.render(view, nextPage, viewMap.info, contentType, charset, req, resp);
        } catch (ViewHandlerException e) {
            Throwable throwable = e.getNested() != null ? e.getNested() : e;

            throw new RequestHandlerException(e.getNonNestedMessage(), throwable);
        }

        // before getting the view generation time flush the response output to get more consistent results
        try {
            resp.flushBuffer();
        } catch (java.io.IOException e) {
            /* If any request gets aborted before completing, i.e if a user requests a page and cancels that request before the page is rendered and returned
               or if request is an ajax request and user calls abort() method for on ajax request then its showing broken pipe exception on console,
               skip throwing of RequestHandlerException. JIRA Ticket - OFBIZ-254
            */
            if (Debug.verboseOn()) Debug.logVerbose("Skip Request Handler Exception that is caused due to aborted requests. " + e.getMessage(), module);
        }

        String vname = (String) req.getAttribute("_CURRENT_VIEW_");

        if (this.trackStats(req) && vname != null) {
            ServerHitBin.countView(cname + "." + vname, req, viewStartTime,
                System.currentTimeMillis() - viewStartTime, userLogin);
        }
    }

    /**
     * Returns a URL String that contains only the scheme and host parts. This method
     * should not be used because it ignores settings in the WebSite entity.
     * 
     * @param request
     * @param secure
     * @deprecated Use OfbizUrlBuilder
     */
    @Deprecated
    public static String getDefaultServerRootUrl(HttpServletRequest request, boolean secure) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String httpsPort = EntityUtilProperties.getPropertyValue("url", "port.https", "443", delegator);
        String httpsServer = EntityUtilProperties.getPropertyValue("url", "force.https.host", delegator);
        String httpPort = EntityUtilProperties.getPropertyValue("url", "port.http", "80", delegator);
        String httpServer = EntityUtilProperties.getPropertyValue("url", "force.http.host", delegator);
        boolean useHttps = EntityUtilProperties.propertyValueEqualsIgnoreCase("url", "port.https.enabled", "Y", delegator);

        if (Start.getInstance().getConfig().portOffset != 0) {
            Integer httpPortValue = Integer.valueOf(httpPort);
            httpPortValue += Start.getInstance().getConfig().portOffset;
            httpPort = httpPortValue.toString();
            Integer httpsPortValue = Integer.valueOf(httpsPort);
            httpsPortValue += Start.getInstance().getConfig().portOffset;
            httpsPort = httpsPortValue.toString();
        }
        
        StringBuilder newURL = new StringBuilder();

        if (secure && useHttps) {
            String server = httpsServer;
            if (UtilValidate.isEmpty(server)) {
                server = request.getServerName();
            }

            newURL.append("https://");
            newURL.append(server);
            if (!httpsPort.equals("443")) {
                newURL.append(":").append(httpsPort);
            }

        } else {
            String server = httpServer;
            if (UtilValidate.isEmpty(server)) {
                server = request.getServerName();
            }

            newURL.append("http://");
            newURL.append(server);
            if (!httpPort.equals("80")) {
                newURL.append(":").append(httpPort);
            }
        }
        return newURL.toString();
    }


    /**
     * Creates a query string based on the redirect parameters for a request response, if specified, or for all request parameters if no redirect parameters are specified.
     *
     * @param request the Http request
     * @param requestResponse the RequestResponse Object
     * @return return the query string
     */
    public String makeQueryString(HttpServletRequest request, ConfigXMLReader.RequestResponse requestResponse) {
        if (requestResponse == null ||
                (requestResponse.redirectParameterMap.size() == 0 && requestResponse.redirectParameterValueMap.size() == 0)) {
            Map<String, Object> urlParams = UtilHttp.getUrlOnlyParameterMap(request);
            String queryString = UtilHttp.urlEncodeArgs(urlParams, false);
            if(UtilValidate.isEmpty(queryString)) {
                return queryString;
            }
            return "?" + queryString;
        } else {
            StringBuilder queryString = new StringBuilder();
            queryString.append("?");
            for (Map.Entry<String, String> entry: requestResponse.redirectParameterMap.entrySet()) {
                String name = entry.getKey();
                String from = entry.getValue();

                Object value = request.getAttribute(from);
                if (value == null) {
                    value = request.getParameter(from);
                }

                addNameValuePairToQueryString(queryString, name, (String) value);
            }

            for (Map.Entry<String, String> entry: requestResponse.redirectParameterValueMap.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();

                addNameValuePairToQueryString(queryString, name, value);
            }

            return queryString.toString();
        }
    }

    private void addNameValuePairToQueryString(StringBuilder queryString, String name, String value) {
        if (UtilValidate.isNotEmpty(value)) {
            if (queryString.length() > 1) {
                queryString.append("&");
            }
            String encodedName = UtilCodec.getEncoder("url").encode(name);
            if (encodedName != null) {
                queryString.append(encodedName);
                queryString.append("=");
                queryString.append(UtilCodec.getEncoder("url").encode(value));
            }
        }
    }

    public String makeLinkWithQueryString(HttpServletRequest request, HttpServletResponse response, String url, ConfigXMLReader.RequestResponse requestResponse) {
        String initialLink = this.makeLink(request, response, url);
        String queryString = this.makeQueryString(request, requestResponse);
        return initialLink + queryString;
    }

    public String makeLink(HttpServletRequest request, HttpServletResponse response, String url) {
        return makeLink(request, response, url, false, request.isSecure(), true);
    }

    public String makeLink(HttpServletRequest request, HttpServletResponse response, String url, boolean fullPath, boolean secure, boolean encode) {
        WebSiteProperties webSiteProps = null;
        try {
            webSiteProps = WebSiteProperties.from(request);
        } catch (GenericEntityException e) {
            // If the entity engine is throwing exceptions, then there is no point in continuing.
            Debug.logError(e, "Exception thrown while getting web site properties: ", module);
            return null;
        }
        String requestUri = RequestHandler.getRequestUri(url);
        ConfigXMLReader.RequestMap requestMap = null;
        if (requestUri != null) {
            try {
                requestMap = getControllerConfig().getRequestMapMap().get(requestUri);
            } catch (WebAppConfigurationException e) {
                // If we can't read the controller.xml file, then there is no point in continuing.
                Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                return null;
            }
        }
        boolean didFullSecure = false;
        boolean didFullStandard = false;
        if (requestMap != null && (webSiteProps.getEnableHttps() || fullPath || secure)) {
            if (Debug.verboseOn()) Debug.logVerbose("In makeLink requestUri=" + requestUri, module);
            if (secure || (webSiteProps.getEnableHttps() && requestMap.securityHttps && !request.isSecure())) {
                didFullSecure = true;
            } else if (fullPath || (webSiteProps.getEnableHttps() && !requestMap.securityHttps && request.isSecure())) {
                didFullStandard = true;
            }
        }
        StringBuilder newURL = new StringBuilder(250);
        if (didFullSecure || didFullStandard) {
            // Build the scheme and host part
            try {
                OfbizUrlBuilder builder = OfbizUrlBuilder.from(request);
                builder.buildHostPart(newURL, url, didFullSecure);
            } catch (GenericEntityException e) {
                // If the entity engine is throwing exceptions, then there is no point in continuing.
                Debug.logError(e, "Exception thrown while getting web site properties: ", module);
                return null;
            } catch (WebAppConfigurationException e) {
                // If we can't read the controller.xml file, then there is no point in continuing.
                Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                return null;
            } catch (IOException e) {
                // If we can't write to StringBuilder, then there is no point in continuing.
                Debug.logError(e, "Exception thrown while writing to StringBuilder: ", module);
                return null;
            }
        }
        // create the path to the control servlet
        String controlPath = (String) request.getAttribute("_CONTROL_PATH_");
        newURL.append(controlPath);

        // now add the actual passed url, but if it doesn't start with a / add one first
        if (!url.startsWith("/")) {
            newURL.append("/");
        }
        newURL.append(url);

        String encodedUrl;
        if (encode) {
            encodedUrl = response.encodeURL(newURL.toString());
        } else {
            encodedUrl = newURL.toString();
        }
        return encodedUrl;
    }

    public static String makeUrl(HttpServletRequest request, HttpServletResponse response, String url) {
        return makeUrl(request, response, url, false, false, false);
    }

    public static String makeUrl(HttpServletRequest request, HttpServletResponse response, String url, boolean fullPath, boolean secure, boolean encode) {
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        return rh.makeLink(request, response, url, fullPath, secure, encode);
    }

    public void runAfterLoginEvents(HttpServletRequest request, HttpServletResponse response) {
        try {
            for (ConfigXMLReader.Event event: getControllerConfig().getAfterLoginEventList().values()) {
                try {
                    String returnString = this.runEvent(request, response, event, null, "after-login");
                    if (returnString != null && !returnString.equalsIgnoreCase("success")) {
                        throw new EventHandlerException("Pre-Processor event did not return 'success'.");
                    }
                } catch (EventHandlerException e) {
                    Debug.logError(e, module);
                }
            }
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
        }
    }

    public void runBeforeLogoutEvents(HttpServletRequest request, HttpServletResponse response) {
        try {
            for (ConfigXMLReader.Event event: getControllerConfig().getBeforeLogoutEventList().values()) {
                try {
                    String returnString = this.runEvent(request, response, event, null, "before-logout");
                    if (returnString != null && !returnString.equalsIgnoreCase("success")) {
                        throw new EventHandlerException("Pre-Processor event did not return 'success'.");
                    }
                } catch (EventHandlerException e) {
                    Debug.logError(e, module);
                }
            }
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
        }
    }

    public boolean trackStats(HttpServletRequest request) {
        if (trackServerHit) {
            String uriString = RequestHandler.getRequestUri(request.getPathInfo());
            if (uriString == null) {
                uriString="";
            }
            ConfigXMLReader.RequestMap requestMap = null;
            try {
                requestMap = getControllerConfig().getRequestMapMap().get(uriString);
            } catch (WebAppConfigurationException e) {
                Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
            }
            if (requestMap == null) return false;
            return requestMap.trackServerHit;
        } else {
            return false;
        }
    }

    public boolean trackVisit(HttpServletRequest request) {
        if (trackVisit) {
            String uriString = RequestHandler.getRequestUri(request.getPathInfo());
            if (uriString == null) {
                uriString="";
            }
            ConfigXMLReader.RequestMap requestMap = null;
            try {
                requestMap = getControllerConfig().getRequestMapMap().get(uriString);
            } catch (WebAppConfigurationException e) {
                Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
            }
            if (requestMap == null) return false;
            return requestMap.trackVisit;
        } else {
            return false;
        }
    }
    
    private String showSessionId(HttpServletRequest request) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        boolean showSessionIdInLog = EntityUtilProperties.propertyValueEqualsIgnoreCase("requestHandler", "show-sessionId-in-log", "Y", delegator);
        if (showSessionIdInLog) {
            return " sessionId=" + UtilHttp.getSessionId(request); 
        }
        return " Hidden sessionId by default.";
    }
}
