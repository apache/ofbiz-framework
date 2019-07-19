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
package org.apache.ofbiz.webapp.control;

import freemarker.template.Template;
import java.io.IOException;
import java.util.Enumeration;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilTimer;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.stats.ServerHitBin;
import org.apache.ofbiz.webapp.stats.VisitHandler;
import org.apache.ofbiz.widget.renderer.VisualTheme;

import freemarker.ext.servlet.ServletContextHashModel;

/**
 * ControlServlet.java - Master servlet for the web application.
 */
@SuppressWarnings("serial")
public class ControlServlet extends HttpServlet {

    public static final String module = ControlServlet.class.getName();

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init() throws ServletException {
        ServletContext ctx = getServletContext();
        if (Debug.infoOn()) {
            String path = ctx.getContextPath();
            String webappName = path.isEmpty() ? path : path.substring(1);
            Debug.logInfo("Loading webapp [" + webappName + "], located at " + ctx.getRealPath("/"), module);
        }

        // Initialize the request handler.
        RequestHandler.getRequestHandler(ctx);
    }
    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long requestStartTime = System.currentTimeMillis();
        HttpSession session = request.getSession();

        // setup DEFAULT character encoding and content type, this will be overridden in the RequestHandler for view rendering
        String charset = request.getCharacterEncoding();

        // setup content type
        String contentType = "text/html";
        if (UtilValidate.isNotEmpty(charset) && !"none".equals(charset)) {
            response.setContentType(contentType + "; charset=" + charset);
            response.setCharacterEncoding(charset);
        } else {
            response.setContentType(contentType);
        }

        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        //Debug.logInfo("Cert Chain: " + request.getAttribute("javax.servlet.request.X509Certificate"), module);

        // set the Entity Engine user info if we have a userLogin
        if (userLogin != null) {
            GenericDelegator.pushUserIdentifier(userLogin.getString("userLoginId"));
        }

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
            timer.timerString("[" + rname + "(Domain:" + request.getScheme() + "://" + request.getServerName() + ")] Request Begun, encoding=[" + charset + "]", module);
        }

        // Setup the CONTROL_PATH for JSP dispatching.
        String contextPath = request.getContextPath();
        if (contextPath == null || "/".equals(contextPath)) {
            contextPath = "";
        }
        request.setAttribute("_CONTROL_PATH_", contextPath + request.getServletPath());
        if (Debug.verboseOn()) {
             Debug.logVerbose("Control Path: " + request.getAttribute("_CONTROL_PATH_"), module);
        }

        // for convenience, and necessity with event handlers, make security and delegator available in the request:
        // try to get it from the session first so that we can have a delegator/dispatcher/security for a certain user if desired
        Delegator delegator = null;
        String delegatorName = (String) session.getAttribute("delegatorName");
        if (UtilValidate.isNotEmpty(delegatorName)) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        if (delegator == null) {
            delegator = (Delegator) getServletContext().getAttribute("delegator");
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

        VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
        if (visualTheme != null) {
            UtilHttp.setVisualTheme(request, visualTheme);
        }

        RequestHandler handler = RequestHandler.getRequestHandler(getServletContext());
        request.setAttribute("_REQUEST_HANDLER_", handler);
        
        ServletContextHashModel ftlServletContext = new ServletContextHashModel(this, FreeMarkerWorker.getDefaultOfbizWrapper());
        request.setAttribute("ftlServletContext", ftlServletContext);

        // setup some things that should always be there
        UtilHttp.setInitialRequestInfo(request);
        VisitHandler.getVisitor(request, response);

        // set the Entity Engine user info if we have a userLogin
        String visitId = VisitHandler.getVisitId(session);
        if (UtilValidate.isNotEmpty(visitId)) {
            GenericDelegator.pushSessionIdentifier(visitId);
        }

        // display details on the servlet objects
        if (Debug.verboseOn()) {
            logRequestInfo(request);
        }

        // some containers call filters on EVERY request, even forwarded ones, so let it know that it came from the control servlet
        request.setAttribute(ControlFilter.FORWARDED_FROM_SERVLET, Boolean.TRUE);

        String errorPage = null;
        try {
            // the ServerHitBin call for the event is done inside the doRequest method
            handler.doRequest(request, response, null, userLogin, delegator);
        } catch (MethodNotAllowedException e) {
            response.setContentType("text/plain");
            response.setCharacterEncoding(request.getCharacterEncoding());
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.getWriter().print(e.getMessage());
            Debug.logError(e.getMessage(), module);
        } catch (RequestHandlerException e) {
            Throwable throwable = e.getNested() != null ? e.getNested() : e;
            if (throwable instanceof IOException) {
                // when an IOException occurs (most of the times caused by the browser window being closed before the request is completed)
                // the connection with the browser is lost and so there is no need to serve the error page; a message is logged to record the event
                if (Debug.warningOn()) Debug.logWarning(e, "Communication error with the client while processing the request: " + request.getAttribute("_CONTROL_PATH_") + request.getPathInfo(), module);
                if (Debug.verboseOn()) Debug.logVerbose(throwable, module);
            } else {
                Debug.logError(throwable, "Error in request handler: ", module);
                request.setAttribute("_ERROR_MESSAGE_", UtilCodec.getEncoder("html").encode(throwable.toString()));
                errorPage = handler.getDefaultErrorPage(request);
            }
        } catch (RequestHandlerExceptionAllowExternalRequests e) {
            errorPage = handler.getDefaultErrorPage(request);
            Debug.logInfo("Going to external page: " + request.getPathInfo(), module);
        } catch (Exception e) {
            Debug.logError(e, "Error in request handler: ", module);
            request.setAttribute("_ERROR_MESSAGE_", UtilCodec.getEncoder("html").encode(e.toString()));
            errorPage = handler.getDefaultErrorPage(request);
        }

        // Forward to the JSP
        // if (Debug.infoOn()) Debug.logInfo("[" + rname + "] Event done, rendering page: " + nextPage, module);
        // if (Debug.timingOn()) timer.timerString("[" + rname + "] Event done, rendering page: " + nextPage, module);

        if (errorPage != null) {
            Debug.logError("An error occurred, going to the errorPage: " + errorPage, module);

            Map<String, Object> context = new HashMap<>();
            context.put("request", request);
            context.put("response", response);
            context.put("session", session);
            context.put("dispatcher", dispatcher);
            context.put("delegator", delegator);
            context.put("security", security);
            context.put("locale", UtilHttp.getLocale(request));
            context.put("timeZone", UtilHttp.getTimeZone(request));
            context.put("userLogin", session.getAttribute("userLogin"));
            context.put("visualTheme", UtilHttp.getVisualTheme(request));

            boolean errorPageFailed = false;
            if (errorPage.endsWith(".jsp")) {
                RequestDispatcher rd = request.getRequestDispatcher(errorPage);

                // use this request parameter to avoid infinite looping on errors in the error page...
                if (request.getAttribute("_ERROR_OCCURRED_") == null && rd != null) {
                    request.setAttribute("_ERROR_OCCURRED_", Boolean.TRUE);
                    Debug.logError("Including errorPage: " + errorPage, module);

                    try {
                        rd.include(request, response);
                    } catch (Throwable t) {
                        errorPageFailed = true;
                    }
                } else {
                    if (rd == null) {
                        Debug.logError("Could not get RequestDispatcher for errorPage: " + errorPage, module);
                    }
                    errorPageFailed = true;
                }
            } else {
                try {
                    Template template = FreeMarkerWorker.getTemplate(errorPage);
                    FreeMarkerWorker.renderTemplate(template, context, response.getWriter());
                } catch (Exception e) {
                    errorPageFailed = true;
                }
            }
            if (errorPageFailed) {
                StringBuilder errorMessage = new StringBuilder("<html><body>")
                        .append("<h1>ERROR MESSAGE</h1>")
                        .append("<hr>").append("<p>")
                        .append("ERROR in error page, (infinite loop or error page not found with name ")
                        .append("[").append(errorPage).append("]").append("</p><p>")
                        .append("Original error detected, maybe it would be helps you : ")
                        .append(StringUtil.replaceString((String) request.getAttribute("_ERROR_MESSAGE_"), "\n", "<br>"))
                        .append("</p></body></html>");
                try {
                    response.getWriter().print(errorMessage.toString());
                } catch (Throwable t) {
                    try {
                        int errorToSend = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                        Debug.logWarning("Error while trying to write error message using response.getOutputStream or response.getWriter, sending error code [" + errorToSend + "], and message [" + errorMessage + "]", module);
                        response.sendError(errorToSend, errorMessage.toString());
                    } catch (Throwable t2) {
                        // wow, still bad... just throw an IllegalStateException with the message and let the servlet container handle it
                        throw new IllegalStateException(errorMessage.toString());
                    }
                }
            }
        }

        // sanity check: make sure we don't have any transactions in place
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

        // run these two again before the ServerHitBin.countRequest call because on a logout this will end up creating a new visit
        if (response.isCommitted() && request.getSession(false) == null) {
            // response committed and no session, and we can't get a new session, what to do!
            // without a session we can't log the hit, etc; so just do nothing; this should NOT happen much!
            Debug.logError("Error in ControlServlet output where response isCommitted and there is no session (probably because of a logout); not saving ServerHit/Bin information because there is no session and as the response isCommitted we can't get a new one. The output was successful, but we just can't save ServerHit/Bin info.", module);
        } else {
            try {
                UtilHttp.setInitialRequestInfo(request);
                VisitHandler.getVisitor(request, response);
                if (handler.trackStats(request)) {
                    ServerHitBin.countRequest(webappName + "." + rname, request, requestStartTime, System.currentTimeMillis() - requestStartTime, userLogin);
                }
            } catch (Throwable t) {
                Debug.logError(t, "Error in ControlServlet saving ServerHit/Bin information; the output was successful, but can't save this tracking information. The error was: " + t.toString(), module);
            }
        }
        if (Debug.timingOn()) timer.timerString("[" + rname + "(Domain:" + request.getScheme() + "://" + request.getServerName() + ")] Request Done", module);

        // sanity check 2: make sure there are no user or session infos in the delegator, ie clear the thread
        GenericDelegator.clearUserIdentifierStack();
        GenericDelegator.clearSessionIdentifierStack();
    }

    protected void logRequestInfo(HttpServletRequest request) {
        ServletContext servletContext = this.getServletContext();
        HttpSession session = request.getSession();

        if (Debug.verboseOn()) Debug.logVerbose("--- Start Request Headers: ---", module);
        Enumeration<String> headerNames = UtilGenerics.cast(request.getHeaderNames());
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Debug.logVerbose(headerName + ":" + request.getHeader(headerName), module);
        }
        if (Debug.verboseOn()) Debug.logVerbose("--- End Request Headers: ---", module);

        if (Debug.verboseOn()) Debug.logVerbose("--- Start Request Parameters: ---", module);
        request.getParameterMap().forEach((name, values) -> {
            Debug.logVerbose(name + ":" + values, module);
        });
        if (Debug.verboseOn()) Debug.logVerbose("--- End Request Parameters: ---", module);

        if (Debug.verboseOn()) Debug.logVerbose("--- Start Request Attributes: ---", module);
        Enumeration<String> reqNames = UtilGenerics.cast(request.getAttributeNames());
        while (reqNames != null && reqNames.hasMoreElements()) {
            String attName = reqNames.nextElement();
            Debug.logVerbose(attName + ":" + request.getAttribute(attName), module);
        }
        if (Debug.verboseOn()) Debug.logVerbose("--- End Request Attributes ---", module);

        if (Debug.verboseOn()) Debug.logVerbose("--- Start Session Attributes: ---", module);
        Enumeration<String> sesNames = null;
        try {
            sesNames = UtilGenerics.cast(session.getAttributeNames());
        } catch (IllegalStateException e) {
            if (Debug.verboseOn()) Debug.logVerbose("Cannot get session attributes : " + e.getMessage(), module);
        }
        while (sesNames != null && sesNames.hasMoreElements()) {
            String attName = sesNames.nextElement();
            Debug.logVerbose(attName + ":" + session.getAttribute(attName), module);
        }
        if (Debug.verboseOn()) Debug.logVerbose("--- End Session Attributes ---", module);

        Enumeration<String> appNames = UtilGenerics.cast(servletContext.getAttributeNames());
        if (Debug.verboseOn()) Debug.logVerbose("--- Start ServletContext Attributes: ---", module);
        while (appNames != null && appNames.hasMoreElements()) {
            String attName = appNames.nextElement();
            Debug.logVerbose(attName + ":" + servletContext.getAttribute(attName), module);
        }
        if (Debug.verboseOn()) Debug.logVerbose("--- End ServletContext Attributes ---", module);
    }
}
