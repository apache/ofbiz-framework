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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Enumeration;

import org.apache.bsf.BSFManager;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilJ2eeCompat;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.stats.ServerHitBin;

/**
 * ControlServlet.java - Master servlet for the web application.
 */
public class ControlServlet extends HttpServlet {

    public static final String module = ControlServlet.class.getName();

    public ControlServlet() {
        super();
    }

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (Debug.infoOn()) {
            Debug.logInfo("[ControlServlet.init] Loading Control Servlet mounted on path " + config.getServletContext().getRealPath("/"), module);
        }

        // configure custom BSF engines
        configureBsf();
        // initialize the request handler
        getRequestHandler();
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestHandler requestHandler = this.getRequestHandler(); 
        
        // setup DEFAULT chararcter encoding and content type, this will be overridden in the RequestHandler for view rendering
        String charset = getServletContext().getInitParameter("charset");
        if (charset == null || charset.length() == 0) charset = request.getCharacterEncoding();
        if (charset == null || charset.length() == 0) charset = "UTF-8";
        Debug.logInfo("The character encoding of the request is: [" + request.getCharacterEncoding() + "]. The character encoding we will use for the request and response is: [" + charset + "]", module);

        if (!"none".equals(charset)) {
            request.setCharacterEncoding(charset);
        }

        // setup content type
        String contentType = "text/html";
        if (charset.length() > 0 && !"none".equals(charset)) {
            response.setContentType(contentType + "; charset=" + charset);
            response.setCharacterEncoding(charset);
        } else {
            response.setContentType(contentType);
        }

        long requestStartTime = System.currentTimeMillis();
        HttpSession session = request.getSession();

        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        //Debug.log("Cert Chain: " + request.getAttribute("javax.servlet.request.X509Certificate"), module);

        // workaraound if we are in the root webapp
        String webappName = UtilHttp.getApplicationName(request);

        String rname = "";
        if (request.getPathInfo() != null) {
            rname = request.getPathInfo().substring(1);
        }
        if (rname.indexOf('/') > 0) {
            rname = rname.substring(0, rname.indexOf('/'));
        }

        UtilTimer timer = null;
        if (Debug.timingOn()) {
            timer = new UtilTimer();
            timer.setLog(true);
            timer.timerString("[" + rname + "] Servlet Starting, doing setup", module);
        }

        // Setup the CONTROL_PATH for JSP dispatching.
        String contextPath = request.getContextPath();
        if (contextPath == null || "/".equals(contextPath)) {
            contextPath = "";
        }
        request.setAttribute("_CONTROL_PATH_", contextPath + request.getServletPath());
        if (Debug.verboseOn())
            Debug.logVerbose("Control Path: " + request.getAttribute("_CONTROL_PATH_"), module);

        // for convenience, and necessity with event handlers, make security and delegator available in the request:
        // try to get it from the session first so that we can have a delegator/dispatcher/security for a certain user if desired
        GenericDelegator delegator = null;
        String delegatorName = (String) session.getAttribute("delegatorName");
        if (UtilValidate.isNotEmpty(delegatorName)) {
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }
        if (delegator == null) {
            delegator = (GenericDelegator) getServletContext().getAttribute("delegator");
        }
        if (delegator == null) {
            Debug.logError("[ControlServlet] ERROR: delegator not found in ServletContext", module);
        } else {
            request.setAttribute("delegator", delegator);
            // always put this in the session too so that session events can use the delegator
            session.setAttribute("delegatorName", delegator.getDelegatorName());
        }

        LocalDispatcher dispatcher = (LocalDispatcher) session.getAttribute("dispatcher");
        if (dispatcher == null) {
            dispatcher = (LocalDispatcher) getServletContext().getAttribute("dispatcher");
        }
        if (dispatcher == null) {
            Debug.logError("[ControlServlet] ERROR: dispatcher not found in ServletContext", module);
        }
        request.setAttribute("dispatcher", dispatcher);

        Security security = (Security) session.getAttribute("security");
        if (security == null) {
            security = (Security) getServletContext().getAttribute("security");
        }
        if (security == null) {
            Debug.logError("[ControlServlet] ERROR: security not found in ServletContext", module);
        }
        request.setAttribute("security", security);
        
        request.setAttribute("_REQUEST_HANDLER_", requestHandler);

        // display details on the servlet objects
        if (Debug.verboseOn()) {
            logRequestInfo(request);
        }

        if (Debug.timingOn()) timer.timerString("[" + rname + "] Setup done, doing Event(s) and View(s)", module);

        // some containers call filters on EVERY request, even forwarded ones, so let it know that it came from the control servlet
        request.setAttribute(ContextFilter.FORWARDED_FROM_SERVLET, Boolean.TRUE);

        String errorPage = null;
        try {
            // the ServerHitBin call for the event is done inside the doRequest method
            requestHandler.doRequest(request, response, null, userLogin, delegator);
        } catch (RequestHandlerException e) {
            Throwable throwable = e.getNested() != null ? e.getNested() : e;
            Debug.logError(throwable, "Error in request handler: ", module);
            request.setAttribute("_ERROR_MESSAGE_", throwable.toString());
            errorPage = requestHandler.getDefaultErrorPage(request);
        } catch (Exception e) {
            Debug.logError(e, "Error in request handler: ", module);
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            errorPage = requestHandler.getDefaultErrorPage(request);
        }

        // Forward to the JSP
        // if (Debug.infoOn()) Debug.logInfo("[" + rname + "] Event done, rendering page: " + nextPage, module);
        // if (Debug.timingOn()) timer.timerString("[" + rname + "] Event done, rendering page: " + nextPage, module);

        if (errorPage != null) {
            Debug.logError("An error occurred, going to the errorPage: " + errorPage, module);

            RequestDispatcher rd = request.getRequestDispatcher(errorPage);

            // use this request parameter to avoid infinite looping on errors in the error page...
            if (request.getAttribute("_ERROR_OCCURRED_") == null && rd != null) {
                request.setAttribute("_ERROR_OCCURRED_", Boolean.TRUE);
                Debug.logError("Including errorPage: " + errorPage, module);
                rd.include(request, response);

                /* For some reason (with Tomcat only?) this isn't making it to the browser, and neither is the rd.include...
                String errorMessage = "<html><body>ERROR in error page, (infinite loop or error page not found with name [" + errorPage + "]), but here is the text just in case it helps you: " + request.getAttribute("ERROR_MESSAGE_") + "</body></html>";
                if (UtilJ2eeCompat.useOutputStreamNotWriter(getServletContext())) {
                    response.getOutputStream().print(errorMessage);
                } else {
                    response.getWriter().print(errorMessage);
                }
                */
            } else {
                if (rd == null) {
                    Debug.logError("Could not get RequestDispatcher for errorPage: " + errorPage, module);
                }

                String errorMessage = "<html><body>ERROR in error page, (infinite loop or error page not found with name [" + errorPage + "]), but here is the text just in case it helps you: " + request.getAttribute("ERROR_MESSAGE_") + "</body></html>";
                if (UtilJ2eeCompat.useOutputStreamNotWriter(getServletContext())) {
                    response.getOutputStream().print(errorMessage);
                } else {
                    response.getWriter().print(errorMessage);
                }
            }
        }

        // sanity check; make sure we don't have any transactions in place
        try {
            // roll back current TX first
            if (TransactionUtil.isTransactionInPlace()) {
                Debug.logWarning("*** NOTICE: ControlServlet finished w/ a transaction in place! Rolling back.", module);
                TransactionUtil.rollback();
            }

            // now resume/rollback any suspended txs
            if (TransactionUtil.suspendedTransactionsHeld()) {
                int suspended = TransactionUtil.cleanSuspendedTransactions();
                Debug.logWarning("Resumed/Rolled Back [" + suspended + "] transactions.", module);
            }
        } catch (GenericTransactionException e) {
            Debug.logWarning(e, module);
        }

        ServerHitBin.countRequest(webappName + "." + rname, request, requestStartTime, System.currentTimeMillis() - requestStartTime, userLogin, delegator);
        if (Debug.timingOn()) timer.timerString("[" + rname + "] Done rendering page, Servlet Finished", module);
    }

    /**
     * @see javax.servlet.Servlet#destroy()
     */
    public void destroy() {
        super.destroy();
    }

    protected RequestHandler getRequestHandler() {
        return RequestHandler.getRequestHandler(getServletContext());
    }

    protected void configureBsf() {
        String[] bshExtensions = {"bsh"};
        BSFManager.registerScriptingEngine("beanshell", "org.ofbiz.base.util.OfbizBshBsfEngine", bshExtensions);

        String[] jsExtensions = {"js"};
        BSFManager.registerScriptingEngine("javascript", "org.ofbiz.base.util.OfbizJsBsfEngine", jsExtensions);

        String[] smExtensions = {"sm"};
        BSFManager.registerScriptingEngine("simplemethod", "org.ofbiz.minilang.SimpleMethodBsfEngine", smExtensions);
    }

    protected void logRequestInfo(HttpServletRequest request) {
        ServletContext servletContext = this.getServletContext();
        HttpSession session = request.getSession();

        Debug.logVerbose("--- Start Request Headers: ---", module);
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            Debug.logVerbose(headerName + ":" + request.getHeader(headerName), module);
        }
        Debug.logVerbose("--- End Request Headers: ---", module);

        Debug.logVerbose("--- Start Request Parameters: ---", module);
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            Debug.logVerbose(paramName + ":" + request.getParameter(paramName), module);
        }
        Debug.logVerbose("--- End Request Parameters: ---", module);

        Debug.logVerbose("--- Start Request Attributes: ---", module);
        Enumeration reqNames = request.getAttributeNames();
        while (reqNames != null && reqNames.hasMoreElements()) {
            String attName = (String) reqNames.nextElement();
            Debug.logVerbose(attName + ":" + request.getAttribute(attName), module);
        }
        Debug.logVerbose("--- End Request Attributes ---", module);

        Debug.logVerbose("--- Start Session Attributes: ---", module);
        Enumeration sesNames = null;
        try {
            sesNames = session.getAttributeNames();
        } catch (IllegalStateException e) {
            Debug.logVerbose("Cannot get session attributes : " + e.getMessage(), module);
        }
        while (sesNames != null && sesNames.hasMoreElements()) {
            String attName = (String) sesNames.nextElement();
            Debug.logVerbose(attName + ":" + session.getAttribute(attName), module);
        }
        Debug.logVerbose("--- End Session Attributes ---", module);

        Enumeration appNames = servletContext.getAttributeNames();
        Debug.logVerbose("--- Start ServletContext Attributes: ---", module);
        while (appNames != null && appNames.hasMoreElements()) {
            String attName = (String) appNames.nextElement();
            Debug.logVerbose(attName + ":" + servletContext.getAttribute(attName), module);
        }
        Debug.logVerbose("--- End ServletContext Attributes ---", module);
    }
}
