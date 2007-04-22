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

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.event.EventFactory;
import org.ofbiz.webapp.event.EventHandler;
import org.ofbiz.webapp.event.EventHandlerException;
import org.ofbiz.webapp.stats.ServerHitBin;
import org.ofbiz.webapp.stats.VisitHandler;
import org.ofbiz.webapp.view.ViewFactory;
import org.ofbiz.webapp.view.ViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.ofbiz.webapp.website.WebSiteWorker;

/**
 * RequestHandler - Request Processor Object
 */
public class RequestHandler implements Serializable {

    public static final String module = RequestHandler.class.getName();
    public static final String err_resource = "WebappUiLabels";

    public static RequestHandler getRequestHandler(ServletContext servletContext) {
        RequestHandler rh = (RequestHandler) servletContext.getAttribute("_REQUEST_HANDLER_");
        if (rh == null) {
            rh = new RequestHandler();
            servletContext.setAttribute("_REQUEST_HANDLER_", rh);
            rh.init(servletContext);            
        }
        return rh;
    }

    private ServletContext context = null;
    private RequestManager requestManager = null;
    private ViewFactory viewFactory = null;
    private EventFactory eventFactory = null;

    public void init(ServletContext context) {
        Debug.logInfo("[RequestHandler Loading...]", module);
        this.context = context;
        this.requestManager = new RequestManager(context);
        this.viewFactory = new ViewFactory(this);
        this.eventFactory = new EventFactory(this);
    }

    public void doRequest(HttpServletRequest request, HttpServletResponse response, String chain,
            GenericValue userLogin, GenericDelegator delegator) throws RequestHandlerException {

        HttpSession session = request.getSession();
        String eventType;
        String eventPath;
        String eventMethod;

        // workaraound if we are in the root webapp
        String cname = UtilHttp.getApplicationName(request);

        // Grab data from request object to process
        String requestUri = RequestHandler.getRequestUri(request.getPathInfo());
        String nextView = RequestHandler.getNextPageUri(request.getPathInfo());
        if (request.getAttribute("targetRequestUri") == null) {
            if (request.getSession().getAttribute("_PREVIOUS_REQUEST_") != null) {
                request.setAttribute("targetRequestUri", request.getSession().getAttribute("_PREVIOUS_REQUEST_"));
            } else {
                request.setAttribute("targetRequestUri", "/" + requestUri);
            }
        }

        // Check for chained request.
        if (chain != null) {
            requestUri = RequestHandler.getRequestUri(chain);
            if (request.getAttribute("_POST_CHAIN_VIEW_") != null) {
                nextView = (String) request.getAttribute("_POST_CHAIN_VIEW_");
            } else {
                nextView = RequestHandler.getNextPageUri(chain);
            }
            if (Debug.infoOn()) Debug.logInfo("[RequestHandler]: Chain in place: requestUri=" + requestUri + " nextView=" + nextView + " sessionId=" + UtilHttp.getSessionId(request), module);
        } else {
            // Check to make sure we are allowed to access this request directly. (Also checks if this request is defined.)
            // If the request cannot be called, or is not defined, check and see if there is a default-request we an process
            if (!requestManager.allowDirectRequest(requestUri)) {
                if (!requestManager.allowDirectRequest(requestManager.getDefaultRequest())) {
                    throw new RequestHandlerException("Unknown request [" + requestUri + "]; this request does not exist or cannot be called directly.");
                } else {
                    requestUri = requestManager.getDefaultRequest();
                }
            }

            // Check if we SHOULD be secure and are not. If we are posting let it pass to not lose data. (too late now anyway)
            if (!request.isSecure() && requestManager.requiresHttps(requestUri) && !request.getMethod().equalsIgnoreCase("POST")) {
                StringBuffer urlBuf = new StringBuffer();
                urlBuf.append(request.getPathInfo());
                if (request.getQueryString() != null) {
                    urlBuf.append("?").append(request.getQueryString());
                }
                String newUrl = RequestHandler.makeUrl(request, response, urlBuf.toString());
                if (newUrl.toUpperCase().startsWith("HTTPS")) {
                    // if we are supposed to be secure, redirect secure.
                    callRedirect(newUrl, response, request);
                }
            }

            // Check if X509 is required and we are not secure; throw exception
            if (!request.isSecure() && requestManager.requiresHttpsClientCert(requestUri)) {
                throw new RequestHandlerException("Unknown request [" + requestUri + "]; this request does not exist or cannot be called directly.");
            }            

            // Check for HTTPS client (x.509) security
            if (request.isSecure() && requestManager.requiresHttpsClientCert(requestUri)) {            
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
                    throw new RequestHandlerException("Unknown request [" + requestUri + "]; this request does not exist or cannot be called directly.");
                } else {
                    for (int i = 0; i < clientCerts.length; i++) {
                        Debug.log(clientCerts[i].getSubjectX500Principal().getName(), module);
                    }
                   
                    // check if this is a trusted cert
                    if (SSLUtil.isClientTrusted(clientCerts, null)) {
                        foundTrustedCert = true;
                    }
                }

                if (!foundTrustedCert) {
                    Debug.logWarning("No trusted certificate found for request [" + requestUri + "]", module);
                    throw new RequestHandlerException("Unknown request [" + requestUri + "]; this request does not exist or cannot be called directly.");
                }
            }

            // If its the first visit run the first visit events.
            if (session.getAttribute("visit") == null) {
                Debug.logInfo("This is the first request in this visit." + " sessionId=" + UtilHttp.getSessionId(request), module);
                // This isn't an event because it is required to run. We do not want to make it optional.
                VisitHandler.setInitialVisit(request, response);
                Collection events = requestManager.getFirstVisitEvents();

                if (events != null) {
                    Iterator i = events.iterator();

                    while (i.hasNext()) {
                        Map eventMap = (Map) i.next();
                        String eType = (String) eventMap.get(ConfigXMLReader.EVENT_TYPE);
                        String ePath = (String) eventMap.get(ConfigXMLReader.EVENT_PATH);
                        String eMeth = (String) eventMap.get(ConfigXMLReader.EVENT_METHOD);

                        try {
                            String returnString = this.runEvent(request, response, eType, ePath, eMeth);
                            if (returnString != null && !returnString.equalsIgnoreCase("success")) {
                                throw new EventHandlerException("First-Visit event did not return 'success'.");
                            } else if (returnString == null) {
                                nextView = "none:";
                            }
                        } catch (EventHandlerException e) {
                            Debug.logError(e, module);
                        }
                    }
                }
            }

            // Invoke the pre-processor (but NOT in a chain)
            Collection preProcEvents = requestManager.getPreProcessor();
            if (preProcEvents != null) {
                Iterator i = preProcEvents.iterator();

                while (i.hasNext()) {
                    Map eventMap = (Map) i.next();
                    String eType = (String) eventMap.get(ConfigXMLReader.EVENT_TYPE);
                    String ePath = (String) eventMap.get(ConfigXMLReader.EVENT_PATH);
                    String eMeth = (String) eventMap.get(ConfigXMLReader.EVENT_METHOD);
                    try {
                        String returnString = this.runEvent(request, response, eType, ePath, eMeth);
                        if (returnString != null && !returnString.equalsIgnoreCase("success")) {
                            throw new EventHandlerException("Pre-Processor event did not return 'success'.");
                        } else if (returnString == null) {
                            nextView = "none:";
                        }
                    } catch (EventHandlerException e) {
                        Debug.logError(e, module);
                    }
                }
            }
        }

        // Pre-Processor/First-Visit event(s) can interrupt the flow by returning null.
        // Warning: this could cause problems if more then one event attempts to return a response.
        if ("none:".equals(nextView)) {
            if (Debug.infoOn()) Debug.logInfo("[Pre-Processor Interrupted Request, not running: " + requestUri + " sessionId=" + UtilHttp.getSessionId(request), module);
            return;
        }

        if (Debug.infoOn()) Debug.logInfo("[Processing Request]: " + requestUri + " sessionId=" + UtilHttp.getSessionId(request), module);
        request.setAttribute("thisRequestUri", requestUri); // store the actual request URI
        
        String eventReturnString = null;

        // Perform security check.
        if (requestManager.requiresAuth(requestUri)) {
            // Invoke the security handler
            // catch exceptions and throw RequestHandlerException if failed.
            Debug.logVerbose("[RequestHandler]: AuthRequired. Running security check." + " sessionId=" + UtilHttp.getSessionId(request), module);
            String checkLoginType = requestManager.getEventType("checkLogin");
            String checkLoginPath = requestManager.getEventPath("checkLogin");
            String checkLoginMethod = requestManager.getEventMethod("checkLogin");
            String checkLoginReturnString;

            try {
                checkLoginReturnString = this.runEvent(request, response, checkLoginType,
                        checkLoginPath, checkLoginMethod);
            } catch (EventHandlerException e) {
                throw new RequestHandlerException(e.getMessage(), e);
            }
            if (!"success".equalsIgnoreCase(checkLoginReturnString)) {
                // previous URL already saved by event, so just do as the return says...
                eventReturnString = checkLoginReturnString;
                requestUri = "checkLogin";                            
            }
        }

        // Invoke the defined event (unless login failed)
        if (eventReturnString == null) {
            eventType = requestManager.getEventType(requestUri);
            eventPath = requestManager.getEventPath(requestUri);
            eventMethod = requestManager.getEventMethod(requestUri);
            if (eventType != null && eventPath != null && eventMethod != null) {
                try {
                    long eventStartTime = System.currentTimeMillis();

                    // run the event
                    eventReturnString = this.runEvent(request, response, eventType, eventPath, eventMethod);

                    // save the server hit
                    ServerHitBin.countEvent(cname + "." + eventMethod, request, eventStartTime,
                            System.currentTimeMillis() - eventStartTime, userLogin, delegator);

                    // set the default event return
                    if (eventReturnString == null) {
                        nextView = "none:";
                    }
                } catch (EventHandlerException e) {
                    // check to see if there is an "error" response, if so go there and make an request error message
                    String tryErrorMsg = requestManager.getRequestAttribute(requestUri, "error");

                    if (tryErrorMsg != null) {
                        eventReturnString = "error";
                        Locale locale = UtilHttp.getLocale(request);
                        String errMsg = UtilProperties.getMessage(RequestHandler.err_resource, "requestHandler.error_call_event", locale);
                        request.setAttribute("_ERROR_MESSAGE_", errMsg + ": " + e.toString());
                    } else {
                        throw new RequestHandlerException("Error calling event and no error repsonse was specified", e);
                    }
                }
            }
        }

         // If error, then display more error messages:
         if ("error".equals(eventReturnString)) {
             if (Debug.errorOn()) {
                 String errorMessageHeader = "Request " + requestUri + " caused an error with the following message: ";
                 if (request.getAttribute("_ERROR_MESSAGE_") != null) {
                     Debug.logError(errorMessageHeader + request.getAttribute("_ERROR_MESSAGE_"), module);
                 }
                 if (request.getAttribute("_ERROR_MESSAGE_LIST_") != null) {
                     Debug.logError(errorMessageHeader + request.getAttribute("_ERROR_MESSAGE_LIST_"), module);
                 }
             }
         }

        // Process the eventReturn.
        String eventReturn = requestManager.getRequestAttribute(requestUri, eventReturnString);
        if (Debug.verboseOn()) Debug.logVerbose("[Response Qualified]: " + eventReturn + " sessionId=" + UtilHttp.getSessionId(request), module);

        // Set the next view (don't use event return if success, default to nextView (which is set to eventReturn later if null); also even if success if it is a type "none" response ignore the nextView, ie use the eventReturn)
        if (eventReturn != null && (!"success".equals(eventReturnString) || eventReturn.startsWith("none:"))) nextView = eventReturn;
        if (Debug.verboseOn()) Debug.logVerbose("[Event Response Mapping]: " + nextView + " sessionId=" + UtilHttp.getSessionId(request), module);

        // get the previous request info
        String previousRequest = (String) request.getSession().getAttribute("_PREVIOUS_REQUEST_");
        String loginPass = (String) request.getAttribute("_LOGIN_PASSED_");

        // restore previous redirected request's attribute, so redirected page can display previous request's error msg etc.
        String preReqAttStr = (String) request.getSession().getAttribute("_REQ_ATTR_MAP_");
        Map preRequestMap;
        if (preReqAttStr != null) {
            request.getSession().removeAttribute("_REQ_ATTR_MAP_");
            byte [] reqAttrMapBytes = StringUtil.fromHexString(preReqAttStr);
            preRequestMap = (java.util.Map)org.ofbiz.base.util.UtilObject.getObject(reqAttrMapBytes);
            java.util.Iterator keys= preRequestMap.keySet().iterator();
            while(keys.hasNext()){
                String key = (String) keys.next();
                if("_ERROR_MESSAGE_LIST_".equals(key) ||
                        "_ERROR_MESSAGE_MAP_".equals(key) ||
                        "_ERROR_MESSAGE_".equals(key) ||
                        "_EVENT_MESSAGE_LIST_".equals(key) ||
                        "_EVENT_MESSAGE_".equals(key)){
                    Object value = preRequestMap.get(key);
                    request.setAttribute(key, value);
               }
            }
        }

        if (Debug.verboseOn()) Debug.logVerbose("[RequestHandler]: previousRequest - " + previousRequest + " (" + loginPass + ")" + " sessionId=" + UtilHttp.getSessionId(request), module);

        // if previous request exists, and a login just succeeded, do that now.
        if (previousRequest != null && loginPass != null && loginPass.equalsIgnoreCase("TRUE")) {
            request.getSession().removeAttribute("_PREVIOUS_REQUEST_");
            if (Debug.infoOn()) Debug.logInfo("[Doing Previous Request]: " + previousRequest + " sessionId=" + UtilHttp.getSessionId(request), module);
            doRequest(request, response, previousRequest, userLogin, delegator);
            return; // this is needed or else we will run the view twice
        }

        String successView = requestManager.getViewName(requestUri);
        if ("success".equals(eventReturnString) && successView.startsWith("request:")) {
            // chains will override any url defined views; but we will save the view for the very end
            if (nextView != null) {
                request.setAttribute("_POST_CHAIN_VIEW_", nextView);
            }
            nextView = successView;
        }

        // Make sure we have some sort of response to go to
        if (nextView == null) nextView = successView;
        if (Debug.verboseOn()) Debug.logVerbose("[Current View]: " + nextView + " sessionId=" + UtilHttp.getSessionId(request), module);

        // Handle the responses - chains/views
        if (nextView != null && nextView.startsWith("request:")) {
            // chained request
            Debug.logInfo("[RequestHandler.doRequest]: Response is a chained request." + " sessionId=" + UtilHttp.getSessionId(request), module);
            nextView = nextView.substring(8);
            doRequest(request, response, nextView, userLogin, delegator);
        } else { // handle views
            // first invoke the post-processor events.
            Collection postProcEvents = requestManager.getPostProcessor();
            if (postProcEvents != null) {
                Iterator i = postProcEvents.iterator();

                while (i.hasNext()) {
                    Map eventMap = (Map) i.next();
                    String eType = (String) eventMap.get(ConfigXMLReader.EVENT_TYPE);
                    String ePath = (String) eventMap.get(ConfigXMLReader.EVENT_PATH);
                    String eMeth = (String) eventMap.get(ConfigXMLReader.EVENT_METHOD);
                    try {
                        String returnString = this.runEvent(request, response, eType, ePath, eMeth);
                        if (returnString != null && !returnString.equalsIgnoreCase("success"))
                            throw new EventHandlerException("Post-Processor event did not return 'success'.");
                        else if (returnString == null)
                            nextView = "none:";
                    } catch (EventHandlerException e) {
                        Debug.logError(e, module);
                    }
                }
            }

            if (nextView != null && nextView.startsWith("url:")) {
                // check for a url for redirection
                Debug.logInfo("[RequestHandler.doRequest]: Response is a URL redirect." + " sessionId=" + UtilHttp.getSessionId(request), module);
                nextView = nextView.substring(4);
                callRedirect(nextView, response, request);
            } else if (nextView != null && nextView.startsWith("cross-redirect:")) {
                // check for a cross-application redirect
                Debug.logInfo("[RequestHandler.doRequest]: Response is a Cross-Application redirect." + " sessionId=" + UtilHttp.getSessionId(request), module);
                String url = nextView.startsWith("/") ? nextView : "/" + nextView;
                callRedirect(url + this.makeQueryString(request), response, request);
            } else if (nextView != null && nextView.startsWith("request-redirect:")) {
                // check for a Request redirect
                Debug.logInfo("[RequestHandler.doRequest]: Response is a Request redirect." + " sessionId=" + UtilHttp.getSessionId(request), module);
                nextView = nextView.substring(17);
                callRedirect(makeLinkWithQueryString(request, response, "/" + nextView), response, request);
            } else if (nextView != null && nextView.startsWith("request-redirect-noparam:")) {
                // check for a Request redirect
                Debug.logInfo("[RequestHandler.doRequest]: Response is a Request redirect with no parameters." + " sessionId=" + UtilHttp.getSessionId(request), module);
                nextView = nextView.substring(25);
                callRedirect(makeLink(request, response, nextView), response, request);
            } else if (nextView != null && nextView.startsWith("view:")) {
                // check for a View
                Debug.logInfo("[RequestHandler.doRequest]: Response is a view." + " sessionId=" + UtilHttp.getSessionId(request), module);
                nextView = nextView.substring(5);
                renderView(nextView, requestManager.allowExtView(requestUri), request, response);
            } else if (nextView != null && nextView.startsWith("none:")) {
                // check for a no dispatch return (meaning the return was processed by the event
                Debug.logInfo("[RequestHandler.doRequest]: Response is handled by the event." + " sessionId=" + UtilHttp.getSessionId(request), module);
            } else if (nextView != null) {
                // a page request
                Debug.logInfo("[RequestHandler.doRequest]: Response is a page [" + nextView + "]" + " sessionId=" + UtilHttp.getSessionId(request), module);
                renderView(nextView, requestManager.allowExtView(requestUri), request, response);
            } else {
                // unknown request
                throw new RequestHandlerException("Illegal response; handler could not process [" + eventReturnString + "].");
            }
        }
    }

    /** Find the event handler and invoke an event. */
    public String runEvent(HttpServletRequest request, HttpServletResponse response, String type,
            String path, String method) throws EventHandlerException {
        EventHandler eventHandler = eventFactory.getEventHandler(type);
        return eventHandler.invoke(path, method, request, response);
    }

    /** Returns the default error page for this request. */
    public String getDefaultErrorPage(HttpServletRequest request) {
        //String requestUri = RequestHandler.getRequestUri(request.getPathInfo());
        //return requestManager.getErrorPage(requestUri);
        return requestManager.getDefaultErrorPage();
    }

    public String makeQueryString(HttpServletRequest request) {
        Map paramMap = UtilHttp.getParameterMap(request);
        StringBuffer queryString = new StringBuffer();
        if (paramMap != null && paramMap.size() > 0) {
            queryString.append("?");
            Iterator i = paramMap.keySet().iterator();
            while (i.hasNext()) {
                String name = (String) i.next();
                Object value = paramMap.get(name);
                if (value instanceof String) {
                    if (queryString.length() > 1) {
                        queryString.append("&");
                    }
                    queryString.append(name);
                    queryString.append("=");
                    queryString.append(value);
                }
            }
        }
        return queryString.toString();
    }

    /** Returns the RequestManager Object. */
    public RequestManager getRequestManager() {
        return requestManager;
    }

    /** Returns the ServletContext Object. */
    public ServletContext getServletContext() {
        return context;
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
        List pathInfo = StringUtil.split(path, "/");
        if (pathInfo == null || pathInfo.size() == 0) {
            Debug.logWarning("Got nothing when splitting URI: " + path, module);
            return null;
        }
        if (((String)pathInfo.get(0)).indexOf('?') > -1) {
            return ((String) pathInfo.get(0)).substring(0, ((String)pathInfo.get(0)).indexOf('?'));
        } else {
            return (String) pathInfo.get(0);
        }
    }

    public static String getNextPageUri(String path) {
        List pathInfo = StringUtil.split(path, "/");
        String nextPage = null;
        if (pathInfo == null) {
            return nextPage;
        }

        for (int i = 1; i < pathInfo.size(); i++) {
            String element = (String) pathInfo.get(i);
            if (element.indexOf('~') != 0) {
                if (element.indexOf('?') > -1) {
                    element = element.substring(0, element.indexOf('?'));
                }
                if (i == 1) {
                    nextPage = element;
                } else {
                    nextPage = nextPage + "/" + element;
                }
            }
        }
        return nextPage;
    }

    private void callRedirect(String url, HttpServletResponse resp, HttpServletRequest req) throws RequestHandlerException {
        if (Debug.infoOn()) Debug.logInfo("[Sending redirect]: " + url + " sessionId=" + UtilHttp.getSessionId(req), module);
        // set the attributes in the session so we can access it.
        java.util.Enumeration attributeNameEnum = req.getAttributeNames();
        Map reqAttrMap = FastMap.newInstance();
        while (attributeNameEnum.hasMoreElements()) {
            String name = (String) attributeNameEnum.nextElement();
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
            resp.sendRedirect(url);
        } catch (IOException ioe) {
            throw new RequestHandlerException(ioe.getMessage(), ioe);
        } catch (IllegalStateException ise) {
            throw new RequestHandlerException(ise.getMessage(), ise);
        }
    }

    private void renderView(String view, boolean allowExtView, HttpServletRequest req, HttpServletResponse resp) throws RequestHandlerException {
        GenericValue userLogin = (GenericValue) req.getSession().getAttribute("userLogin");
        GenericDelegator delegator = (GenericDelegator) req.getAttribute("delegator");
        // workaraound if we are in the root webapp
        String cname = UtilHttp.getApplicationName(req);
        String oldView = view;

        if (view != null && view.length() > 0 && view.charAt(0) == '/') view = view.substring(1);

        // if the view name starts with the control servlet name and a /, then it was an
        // attempt to override the default view with a call back into the control servlet,
        // so just get the target view name and use that
        String servletName = req.getServletPath().substring(1);

        Debug.logInfo("servletName=" + servletName + ", view=" + view + " sessionId=" + UtilHttp.getSessionId(req), module);
        if (view.startsWith(servletName + "/")) {
            view = view.substring(servletName.length() + 1);
            Debug.logInfo("a manual control servlet request was received, removing control servlet path resulting in: view=" + view, module);
        }

        if (Debug.verboseOn()) Debug.logVerbose("[Getting View Map]: " + view + " sessionId=" + UtilHttp.getSessionId(req), module);

        // before mapping the view, set a session attribute so we know where we are
        req.setAttribute("_CURRENT_VIEW_", view);

        String viewType = requestManager.getViewType(view);
        String tempView = requestManager.getViewPage(view);
        String nextPage;

        if (tempView == null) {
            if (!allowExtView) {
                throw new RequestHandlerException("No view to render.");
            } else {
                nextPage = "/" + oldView;
            }
        } else {
            nextPage = tempView;
        }

        if (Debug.verboseOn()) Debug.logVerbose("[Mapped To]: " + nextPage + " sessionId=" + UtilHttp.getSessionId(req), module);

        long viewStartTime = System.currentTimeMillis();

        // setup chararcter encoding and content type
        String charset = getServletContext().getInitParameter("charset");

        if (charset == null || charset.length() == 0) charset = req.getCharacterEncoding();
        if (charset == null || charset.length() == 0) charset = "UTF-8";

        String viewCharset = requestManager.getViewEncoding(view);
        //NOTE: if the viewCharset is "none" then no charset will be used
        if (viewCharset != null && viewCharset.length() > 0) charset = viewCharset;

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
        String viewContentType = requestManager.getViewContentType(view);
        if (viewContentType != null && viewContentType.length() > 0) contentType = viewContentType;

        if (charset.length() > 0 && !"none".equals(charset)) {
            resp.setContentType(contentType + "; charset=" + charset);
        } else {
            resp.setContentType(contentType);
        }

        if (Debug.verboseOn()) Debug.logVerbose("The ContentType for the " + view + " view is: " + contentType, module);

        try {
            if (Debug.verboseOn()) Debug.logVerbose("Rendering view [" + nextPage + "] of type [" + viewType + "]", module);
            ViewHandler vh = viewFactory.getViewHandler(viewType);
            vh.render(view, nextPage, requestManager.getViewInfo(view), contentType, charset, req, resp);
        } catch (ViewHandlerException e) {
            Throwable throwable = e.getNested() != null ? e.getNested() : e;

            throw new RequestHandlerException(e.getNonNestedMessage(), throwable);
        }

        // before getting the view generation time flush the response output to get more consistent results
        try {
            resp.flushBuffer();
        } catch (java.io.IOException e) {
            throw new RequestHandlerException("Error flushing response buffer", e);
        }

        String vname = (String) req.getAttribute("_CURRENT_VIEW_");

        if (vname != null) {
            ServerHitBin.countView(cname + "." + vname, req, viewStartTime,
                System.currentTimeMillis() - viewStartTime, userLogin, delegator);
        }
    }

    public static String getDefaultServerRootUrl(HttpServletRequest request, boolean secure) {
        String httpsPort = UtilProperties.getPropertyValue("url.properties", "port.https", "443");
        String httpsServer = UtilProperties.getPropertyValue("url.properties", "force.https.host");
        String httpPort = UtilProperties.getPropertyValue("url.properties", "port.http", "80");
        String httpServer = UtilProperties.getPropertyValue("url.properties", "force.http.host");
        boolean useHttps = UtilProperties.propertyValueEqualsIgnoreCase("url.properties", "port.https.enabled", "Y");

        StringBuffer newURL = new StringBuffer();

        if (secure && useHttps) {
            String server = httpsServer;
            if (server == null || server.length() == 0) {
                server = request.getServerName();
            }

            newURL.append("https://");
            newURL.append(server);
            if (!httpsPort.equals("443")) {
                newURL.append(":").append(httpsPort);
            }

        } else {
            String server = httpServer;
            if (server == null || server.length() == 0) {
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


    public String makeLinkWithQueryString(HttpServletRequest request, HttpServletResponse response, String url) {
        String initialLink = this.makeLink(request, response, url);
        String queryString = this.makeQueryString(request);
        return initialLink + queryString;
    }

    public String makeLink(HttpServletRequest request, HttpServletResponse response, String url) {
        return makeLink(request, response, url, false, false, true);
    }

    public String makeLink(HttpServletRequest request, HttpServletResponse response, String url, boolean fullPath, boolean secure, boolean encode) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String webSiteId = WebSiteWorker.getWebSiteId(request);

        String httpsPort = null;
        String httpsServer = null;
        String httpPort = null;
        String httpServer = null;
        Boolean enableHttps = null;

        // load the properties from the website entity
        GenericValue webSite;
        if (webSiteId != null) {
            try {
                webSite = delegator.findByPrimaryKeyCache("WebSite", UtilMisc.toMap("webSiteId", webSiteId));
                if (webSite != null) {
                    httpsPort = webSite.getString("httpsPort");
                    httpsServer = webSite.getString("httpsHost");
                    httpPort = webSite.getString("httpPort");
                    httpServer = webSite.getString("httpHost");
                    enableHttps = webSite.getBoolean("enableHttps");
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Problems with WebSite entity; using global defaults", module);
            }
        }

        // fill in any missing properties with fields from the global file
        if (UtilValidate.isEmpty(httpsPort)) {
            httpsPort = UtilProperties.getPropertyValue("url.properties", "port.https", "443");
        }
        if (UtilValidate.isEmpty(httpServer)) {
            httpsServer = UtilProperties.getPropertyValue("url.properties", "force.https.host");
        }
        if (UtilValidate.isEmpty(httpPort)) {
            httpPort = UtilProperties.getPropertyValue("url.properties", "port.http", "80");
        }
        if (UtilValidate.isEmpty(httpServer)) {
            httpServer = UtilProperties.getPropertyValue("url.properties", "force.http.host");
        }
        if (enableHttps == null) {
            enableHttps = Boolean.valueOf(UtilProperties.propertyValueEqualsIgnoreCase("url.properties", "port.https.enabled", "Y"));
        }

        // create the path the the control servlet
        String controlPath = (String) request.getAttribute("_CONTROL_PATH_");

        String requestUri = RequestHandler.getRequestUri(url);
        StringBuffer newURL = new StringBuffer();

        boolean useHttps = enableHttps.booleanValue();
        boolean didFullSecure = false;
        boolean didFullStandard = false;
        if (useHttps || fullPath || secure) {
            if (secure || (useHttps && requestManager.requiresHttps(requestUri) && !request.isSecure())) {
                String server = httpsServer;
                if (server == null || server.length() == 0) {
                    server = request.getServerName();
                }

                newURL.append("https://");
                newURL.append(server);
                if (!httpsPort.equals("443")) {
                    newURL.append(":").append(httpsPort);
                }

                didFullSecure = true;
            } else if (fullPath || (useHttps && !requestManager.requiresHttps(requestUri) && request.isSecure())) {
                String server = httpServer;
                if (server == null || server.length() == 0) {
                    server = request.getServerName();
                }

                newURL.append("http://");
                newURL.append(server);
                if (!httpPort.equals("80")) {
                    newURL.append(":").append(httpPort);
                }

                didFullStandard = true;
            }
        }

        newURL.append(controlPath);

        // now add the actual passed url, but if it doesn't start with a / add one first
        if (!url.startsWith("/")) {
            newURL.append("/");
        }
        newURL.append(url);

        String encodedUrl;
        if (encode) {
            boolean forceManualJsessionid = false;

            // if this isn't a secure page, but we made a secure URL, make sure we manually add the jsessionid since the response.encodeURL won't do that
            if (!request.isSecure() && didFullSecure) {
                forceManualJsessionid = true;
            }

            // if this is a secure page, but we made a standard URL, make sure we manually add the jsessionid since the response.encodeURL won't do that
            if (request.isSecure() && didFullStandard) {
                forceManualJsessionid = true;
            }

            if (response != null && !forceManualJsessionid) {
                encodedUrl = response.encodeURL(newURL.toString());
            } else {
                String sessionId = ";jsessionid=" + request.getSession().getId();
                // this should be inserted just after the "?" for the parameters, if there is one, or at the end of the string
                int questionIndex = newURL.indexOf("?");
                if (questionIndex == -1) {
                    newURL.append(sessionId);
                } else {
                    newURL.insert(questionIndex, sessionId);
                }
                encodedUrl = newURL.toString();
            }
        } else {
            encodedUrl = newURL.toString();
        }
        //if (encodedUrl.indexOf("null") > 0) {
            //Debug.logError("in makeLink, controlPath:" + controlPath + " url:" + url, "");
            //throw new RuntimeException("in makeLink, controlPath:" + controlPath + " url:" + url);
        //}

        //Debug.logInfo("Making URL, encode=" + encode + " for URL: " + newURL + "\n encodedUrl: " + encodedUrl, module);

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
        List afterLoginEvents = requestManager.getAfterLoginEventList();
        if (afterLoginEvents != null) {
            Iterator i = afterLoginEvents.iterator();
            while (i.hasNext()) {
                Map eventMap = (Map) i.next();
                String eType = (String) eventMap.get(ConfigXMLReader.EVENT_TYPE);
                String ePath = (String) eventMap.get(ConfigXMLReader.EVENT_PATH);
                String eMeth = (String) eventMap.get(ConfigXMLReader.EVENT_METHOD);
                try {
                    String returnString = this.runEvent(request, response, eType, ePath, eMeth);
                    if (returnString != null && !returnString.equalsIgnoreCase("success")) {
                        throw new EventHandlerException("Pre-Processor event did not return 'success'.");
                    }
                } catch (EventHandlerException e) {
                    Debug.logError(e, module);
                }
            }
        }
    }

    public void runBeforeLogoutEvents(HttpServletRequest request, HttpServletResponse response) {
        List beforeLogoutEvents = requestManager.getBeforeLogoutEventList();
        if (beforeLogoutEvents != null) {
            Iterator i = beforeLogoutEvents.iterator();
            while (i.hasNext()) {
                Map eventMap = (Map) i.next();
                String eType = (String) eventMap.get(ConfigXMLReader.EVENT_TYPE);
                String ePath = (String) eventMap.get(ConfigXMLReader.EVENT_PATH);
                String eMeth = (String) eventMap.get(ConfigXMLReader.EVENT_METHOD);
                try {
                    String returnString = this.runEvent(request, response, eType, ePath, eMeth);
                    if (returnString != null && !returnString.equalsIgnoreCase("success")) {
                        throw new EventHandlerException("Pre-Processor event did not return 'success'.");
                    }
                } catch (EventHandlerException e) {
                    Debug.logError(e, module);
                }
            }
        }
    }
}
