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

import static org.apache.ofbiz.base.util.UtilGenerics.checkMap;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.transaction.Transaction;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.KeyStoreUtil;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.StringUtil.StringWrapper;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.EntityCryptoException;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityCrypto;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityConfigurationException;
import org.apache.ofbiz.security.SecurityFactory;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.webapp.stats.VisitHandler;
import org.apache.ofbiz.widget.model.ThemeFactory;

/**
 * Common Workers
 */
public class LoginWorker {

    public final static String module = LoginWorker.class.getName();
    public static final String resourceWebapp = "SecurityextUiLabels";

    public static final String X509_CERT_ATTR = "SSLx509Cert";
    public static final String securityProperties = "security.properties";

    private static final String keyValue = UtilProperties.getPropertyValue(securityProperties, "login.secret_key_string");

    public static StringWrapper makeLoginUrl(PageContext pageContext) {
        return makeLoginUrl(pageContext, "checkLogin");
    }

    public static StringWrapper makeLoginUrl(HttpServletRequest request) {
        return makeLoginUrl(request, "checkLogin");
    }

    public static StringWrapper makeLoginUrl(PageContext pageContext, String requestName) {
        return makeLoginUrl((HttpServletRequest) pageContext.getRequest(), requestName);
    }
    public static StringWrapper makeLoginUrl(HttpServletRequest request, String requestName) {
        Map<String, Object> urlParams = UtilHttp.getUrlOnlyParameterMap(request);
        String queryString = UtilHttp.urlEncodeArgs(urlParams);
        String currentView = UtilFormatOut.checkNull((String) request.getAttribute("_CURRENT_VIEW_"));

        String loginUrl = "/" + requestName;
        if ("login".equals(currentView)) {
            return StringUtil.wrapString(loginUrl);
        }
        if (UtilValidate.isNotEmpty(currentView)) {
            loginUrl += "/" + currentView;
        }
        if (UtilValidate.isNotEmpty(queryString)) {
            loginUrl += "?" + queryString;
        }

        return StringUtil.wrapString(loginUrl);
    }

    public static void setLoggedOut(String userLoginId, Delegator delegator) {
        if (UtilValidate.isEmpty(userLoginId)) {
            if (Debug.warningOn()) {
                Debug.logWarning("Called setLogged out with empty userLoginId", module);
            }
        }

        Transaction parentTx = null;
        boolean beganTransaction = false;

        try {
            try {
                parentTx = TransactionUtil.suspend();
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Cannot suspend current transaction: " + e.getMessage(), module);
            }

            try {
                beganTransaction = TransactionUtil.begin();

                GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
                if (userLogin == null) {
                    Debug.logError("Could not find UserLogin record for setLoggedOut with userLoginId [" + userLoginId + "]", module);
                } else {
                    userLogin.set("hasLoggedOut", "Y");
                    userLogin.store();
                }
            } catch (GenericEntityException e) {
                String errMsg = "Unable to set logged out flag on UserLogin";
                Debug.logError(e, errMsg, module);
                try {
                    TransactionUtil.rollback(beganTransaction, errMsg, e);
                } catch (GenericTransactionException e2) {
                    Debug.logError(e2, "Could not rollback nested transaction: " + e.getMessage(), module);
                }
            } finally {
                try {
                    TransactionUtil.commit(beganTransaction);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, "Could not commit nested transaction: " + e.getMessage(), module);
                }
            }
        } finally {
            // resume/restore parent transaction
            if (parentTx != null) {
                try {
                    TransactionUtil.resume(parentTx);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Resumed the parent transaction.", module);
                    }
                } catch (GenericTransactionException ite) {
                    Debug.logError(ite, "Cannot resume transaction: " + ite.getMessage(), module);
                }
            }
        }
    }

    /**
     */
    public static GenericValue checkLogout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        // anonymous shoppers are not logged in
        if (userLogin != null && "anonymous".equals(userLogin.getString("userLoginId"))) {
            userLogin = null;
        }

        // user is logged in; check to see if they have globally logged out if not
        // check if they have permission for this login attempt; if not log them out
        if (userLogin != null) {
            List<Object> errorMessageList = UtilGenerics.checkList(request.getAttribute("_ERROR_MESSAGE_LIST"));
            if (!hasBasePermission(userLogin, request) || isFlaggedLoggedOut(userLogin, userLogin.getDelegator())) {
                if (errorMessageList == null) {
                    errorMessageList = new LinkedList<Object>();
                    request.setAttribute("_ERROR_MESSAGE_LIST", errorMessageList);
                }
                errorMessageList.add("User does not have permission or is flagged as logged out");
                if (Debug.infoOn()) {
                    Debug.logInfo("User does not have permission or is flagged as logged out", module);
                }
                doBasicLogout(userLogin, request, response);
                userLogin = null;
            }
        }
        return userLogin;
    }

    /** This WebEvent allows for java 'services' to hook into the login path.
     * This method loads all instances of {@link LoginCheck}, and calls the
     * {@link LoginCheck#associate} method.  The first implementation to return
     * a non-null value gets that value returned to the caller.  Returning
     * "none" will abort processing, while anything else gets looked up in
     * outer view dispatch.  This event is called when the current request
     * needs to have a validly logged in user; it is a wrapper around {@link
     * #checkLogin}.
     *
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return String
     */
    public static String extensionCheckLogin(HttpServletRequest request, HttpServletResponse response) {
        for (LoginCheck check: ServiceLoader.load(LoginCheck.class)) {
            if (!check.isEnabled()) {
                continue;
            }
            String result = check.associate(request, response);
            if (result != null) {
                return result;
            }
        }
        return checkLogin(request, response);
    }

    /** This WebEvent allows for java 'services' to hook into the login path.
     * This method loads all instances of {@link LoginCheck}, and calls the
     * {@link LoginCheck#check} method.  The first implementation to return
     * a non-null value gets that value returned to the caller.  Returning
     * "none" will abort processing, while anything else gets looked up in
     * outer view dispatch; for preprocessors, only "success" makes sense.
     *
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return String
     */
    public static String extensionConnectLogin(HttpServletRequest request, HttpServletResponse response) {
        for (LoginCheck check: ServiceLoader.load(LoginCheck.class)) {
            if (!check.isEnabled()) {
                continue;
            }
            String result = check.check(request, response);
            if (result != null) {
                return result;
            }
        }
        return "success";
    }

    /**
     * An HTTP WebEvent handler that checks to see is a userLogin is logged in.
     * If not, the user is forwarded to the login page.
     *
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return String
     */
    public static String checkLogin(HttpServletRequest request, HttpServletResponse response) {
        GenericValue userLogin = checkLogout(request, response);
        // have to reget this because the old session object will be invalid
        HttpSession session = request.getSession();

        String username = null;
        String password = null;

        if (userLogin == null) {
            // check parameters
            username = request.getParameter("USERNAME");
            password = request.getParameter("PASSWORD");
            // check session attributes
            if (username == null) username = (String) session.getAttribute("USERNAME");
            if (password == null) password = (String) session.getAttribute("PASSWORD");

            // in this condition log them in if not already; if not logged in or can't log in, save parameters and return error
            if ((username == null) || (password == null) || ("error".equals(login(request, response)))) {

                // make sure this attribute is not in the request; this avoids infinite recursion when a login by less stringent criteria (like not checkout the hasLoggedOut field) passes; this is not a normal circumstance but can happen with custom code or in funny error situations when the userLogin service gets the userLogin object but runs into another problem and fails to return an error
                request.removeAttribute("_LOGIN_PASSED_");

                // keep the previous request name in the session
                session.setAttribute("_PREVIOUS_REQUEST_", request.getPathInfo());

                // NOTE: not using the old _PREVIOUS_PARAMS_ attribute at all because it was a security hole as it was used to put data in the URL (never encrypted) that was originally in a form field that may have been encrypted
                // keep 2 maps: one for URL parameters and one for form parameters
                Map<String, Object> urlParams = UtilHttp.getUrlOnlyParameterMap(request);
                if (UtilValidate.isNotEmpty(urlParams)) {
                    session.setAttribute("_PREVIOUS_PARAM_MAP_URL_", urlParams);
                }
                Map<String, Object> formParams = UtilHttp.getParameterMap(request, urlParams.keySet(), false);
                if (UtilValidate.isNotEmpty(formParams)) {
                    session.setAttribute("_PREVIOUS_PARAM_MAP_FORM_", formParams);
                }

                //if (Debug.infoOn()) Debug.logInfo("checkLogin: PathInfo=" + request.getPathInfo(), module);

                return "error";
            }
        }

        return "success";
    }

    /**
     * An HTTP WebEvent handler that logs in a userLogin. This should run before the security check.
     *
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return Return a boolean which specifies whether or not the calling Servlet or
     *         JSP should generate its own content. This allows an event to override the default content.
     */
    public static String login(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();  
        
        // Prevent session fixation by making Tomcat generate a new jsessionId (ultimately put in cookie). 
        if (!session.isNew()) {  // Only do when really signing in. 
            request.changeSessionId();
        }
        
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String username = request.getParameter("USERNAME");
        String password = request.getParameter("PASSWORD");
        String forgotPwdFlag = request.getParameter("forgotPwdFlag");

        // password decryption
        EntityCrypto entityDeCrypto = null;
        try {
            entityDeCrypto = new EntityCrypto(delegator, null);
        } catch (EntityCryptoException e1) {
            Debug.logError(e1.getMessage(), module);
        }
        
        if(entityDeCrypto != null && "true".equals(forgotPwdFlag)) {
            try {
                Object decryptedPwd = entityDeCrypto.decrypt(keyValue, ModelField.EncryptMethod.TRUE, password);
                password = decryptedPwd.toString();
            } catch (GeneralException e) {
                Debug.logError(e, "Current Password Decryption failed", module);
            }
        }

        if (username == null) username = (String) session.getAttribute("USERNAME");
        if (password == null) password = (String) session.getAttribute("PASSWORD");

        // allow a username and/or password in a request attribute to override the request parameter or the session attribute; this way a preprocessor can play with these a bit...
        if (UtilValidate.isNotEmpty(request.getAttribute("USERNAME"))) {
            username = (String) request.getAttribute("USERNAME");
        }
        if (UtilValidate.isNotEmpty(request.getAttribute("PASSWORD"))) {
            password = (String) request.getAttribute("PASSWORD");
        }

        List<String> unpwErrMsgList = new LinkedList<String>();
        if (UtilValidate.isEmpty(username)) {
            unpwErrMsgList.add(UtilProperties.getMessage(resourceWebapp, "loginevents.username_was_empty_reenter", UtilHttp.getLocale(request)));
        }
        if (UtilValidate.isEmpty(password)) {
            unpwErrMsgList.add(UtilProperties.getMessage(resourceWebapp, "loginevents.password_was_empty_reenter", UtilHttp.getLocale(request)));
        }
        boolean requirePasswordChange = "Y".equals(request.getParameter("requirePasswordChange"));
        if (!unpwErrMsgList.isEmpty()) {
            request.setAttribute("_ERROR_MESSAGE_LIST_", unpwErrMsgList);
            return  requirePasswordChange ? "requirePasswordChange" : "error";
        }

        boolean setupNewDelegatorEtc = false;

        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ServletContext servletContext = session.getServletContext();

        // if a tenantId was passed in, see if the userLoginId is associated with that tenantId (can use any delegator for this, entity is not tenant-specific)
        String tenantId = request.getParameter("userTenantId");
        if (UtilValidate.isEmpty(tenantId)) {
            tenantId = (String) request.getAttribute("userTenantId");
        }
        if (UtilValidate.isNotEmpty(tenantId)) {
            // see if we need to activate a tenant delegator, only do if the current delegatorName has a hash symbol in it, and if the passed in tenantId doesn't match the one in the delegatorName
            String oldDelegatorName = delegator.getDelegatorName();
            int delegatorNameHashIndex = oldDelegatorName.indexOf('#');
            String currentDelegatorTenantId = null;
            if (delegatorNameHashIndex > 0) {
                currentDelegatorTenantId = oldDelegatorName.substring(delegatorNameHashIndex + 1);
                if (currentDelegatorTenantId != null) currentDelegatorTenantId = currentDelegatorTenantId.trim();
            }

            if (delegatorNameHashIndex == -1 || (currentDelegatorTenantId != null && !tenantId.equals(currentDelegatorTenantId))) {
                // make that tenant active, setup a new delegator and a new dispatcher
                String delegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;

                try {
                    // after this line the delegator is replaced with the new per-tenant delegator
                    delegator = DelegatorFactory.getDelegator(delegatorName);
                    dispatcher = WebAppUtil.makeWebappDispatcher(servletContext, delegator);
                } catch (NullPointerException e) {
                    Debug.logError(e, "Error getting tenant delegator", module);
                    Map<String, String> messageMap = UtilMisc.toMap("errorMessage", "Tenant [" + tenantId + "]  not found...");
                    String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.following_error_occurred_during_login", messageMap, UtilHttp.getLocale(request));
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }

                // NOTE: these will be local for now and set in the request and session later, after we've verified that the user
                setupNewDelegatorEtc = true;
            }
        } else {
            // Set default delegator
            if (Debug.infoOn()) {
                Debug.logInfo("Setting default delegator", module);
            }
            String delegatorName = delegator.getDelegatorBaseName();
            try {
                // after this line the delegator is replaced with default delegator
                delegator = DelegatorFactory.getDelegator(delegatorName);
                dispatcher = WebAppUtil.makeWebappDispatcher(servletContext, delegator);
            } catch (NullPointerException e) {
                Debug.logError(e, "Error getting default delegator", module);
                Map<String, String> messageMap = UtilMisc.toMap("errorMessage", "Error getting default delegator");
                String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.following_error_occurred_during_login", messageMap, UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            setupNewDelegatorEtc = true;
        }

        Map<String, Object> result = null;
        try {
            // get the visit id to pass to the userLogin for history
            String visitId = VisitHandler.getVisitId(session);
            result = dispatcher.runSync("userLogin", UtilMisc.toMap("login.username", username, "login.password", password, "visitId", visitId, "locale", UtilHttp.getLocale(request), "request", request));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling userLogin service", module);
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.following_error_occurred_during_login", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        if (ModelService.RESPOND_SUCCESS.equals(result.get(ModelService.RESPONSE_MESSAGE))) {
            GenericValue userLogin = (GenericValue) result.get("userLogin");

            if (requirePasswordChange) {
                Map<String, Object> inMap = UtilMisc.<String, Object>toMap("login.username", username, "login.password", password, "locale", UtilHttp.getLocale(request));
                inMap.put("userLoginId", username);
                inMap.put("currentPassword", password);
                inMap.put("newPassword", request.getParameter("newPassword"));
                inMap.put("newPasswordVerify", request.getParameter("newPasswordVerify"));
                Map<String, Object> resultPasswordChange = null;
                try {
                    resultPasswordChange = dispatcher.runSync("updatePassword", inMap);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error calling updatePassword service", module);
                    Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
                    String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.following_error_occurred_during_login", messageMap, UtilHttp.getLocale(request));
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "requirePasswordChange";
                }
                if (ServiceUtil.isError(resultPasswordChange)) {
                    String errorMessage = (String) resultPasswordChange.get(ModelService.ERROR_MESSAGE);
                    if (UtilValidate.isNotEmpty(errorMessage)) {
                        Map<String, String> messageMap = UtilMisc.toMap("errorMessage", errorMessage);
                        String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.following_error_occurred_during_login", messageMap, UtilHttp.getLocale(request));
                        request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    }
                    request.setAttribute("_ERROR_MESSAGE_LIST_", resultPasswordChange.get(ModelService.ERROR_MESSAGE_LIST));
                    return "requirePasswordChange";
                } else {
                    try {
                        userLogin.refresh();
                    }
                    catch (GenericEntityException e) {
                        Debug.logError(e, "Error refreshing userLogin value", module);
                        Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
                        String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.following_error_occurred_during_login", messageMap, UtilHttp.getLocale(request));
                        request.setAttribute("_ERROR_MESSAGE_", errMsg);
                        return "requirePasswordChange";
                    }
                }
            }
            if (setupNewDelegatorEtc) {
                // now set the delegator and dispatcher in a bunch of places just in case they were changed
                setWebContextObjects(request, response, delegator, dispatcher);
            }

            // check to see if a password change is required for the user
            Map<String, Object> userLoginSession = checkMap(result.get("userLoginSession"), String.class, Object.class);
            if (userLogin != null && "Y".equals(userLogin.getString("requirePasswordChange"))) {
                return "requirePasswordChange";
            }
            String autoChangePassword = EntityUtilProperties.getPropertyValue("security", "user.auto.change.password.enable", "false", delegator);
            if ("true".equalsIgnoreCase(autoChangePassword)) {
                if ("requirePasswordChange".equals(autoChangePassword(request, response))) {
                    return "requirePasswordChange";
                }
            }

            // check on JavaScriptEnabled
            String javaScriptEnabled = "N";
            if ("Y".equals(request.getParameter("JavaScriptEnabled"))) {
                javaScriptEnabled = "Y";
            }
            try {
                result = dispatcher.runSync("setUserPreference", UtilMisc.toMap("userPrefTypeId", "javaScriptEnabled", "userPrefGroupTypeId", "GLOBAL_PREFERENCES", "userPrefValue", javaScriptEnabled, "userLogin", userLogin));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Error setting user preference", module);
            }

            // finally do the main login routine to set everything else up in the session, etc
            return doMainLogin(request, response, userLogin, userLoginSession);
        } else {
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", (String) result.get(ModelService.ERROR_MESSAGE));
            String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.following_error_occurred_during_login", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return requirePasswordChange ? "requirePasswordChange" : "error";
        }
    }

    protected static void setWebContextObjects(HttpServletRequest request, HttpServletResponse response, Delegator delegator, LocalDispatcher dispatcher) {
        HttpSession session = request.getSession();
        // NOTE: we do NOT want to set this in the servletContext, only in the request and session
        // We also need to setup the security objects since they are dependent on the delegator
        Security security = null;
        try {
            security = SecurityFactory.getInstance(delegator);
        } catch (SecurityConfigurationException e) {
            Debug.logError(e, module);
        }
        request.setAttribute("delegator", delegator);
        request.setAttribute("dispatcher", dispatcher);
        request.setAttribute("security", security);

        session.setAttribute("delegatorName", delegator.getDelegatorName());
        session.setAttribute("delegator", delegator);
        session.setAttribute("dispatcher", dispatcher);
        session.setAttribute("security", security);

        // get rid of the visit info since it was pointing to the previous database, and get a new one
        session.removeAttribute("visitor");
        session.removeAttribute("visit");
        VisitHandler.getVisitor(request, response);
        VisitHandler.getVisit(session);
    }

    public static String doMainLogin(HttpServletRequest request, HttpServletResponse response, GenericValue userLogin, Map<String, Object> userLoginSession) {
        HttpSession session = request.getSession();
        if (userLogin != null && hasBasePermission(userLogin, request)) {
            doBasicLogin(userLogin, request);
        } else {
            String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.unable_to_login_this_application", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        if (userLoginSession != null) {
            session.setAttribute("userLoginSession", userLoginSession);
        }

        request.setAttribute("_LOGIN_PASSED_", "TRUE");

        // run the after-login events
        RequestHandler rh = RequestHandler.getRequestHandler(request.getSession().getServletContext());
        rh.runAfterLoginEvents(request, response);


        // make sure the autoUserLogin is set to the same and that the client cookie has the correct userLoginId
        autoLoginSet(request, response);

        return autoLoginCheck(request, response);
        
    }

    public static void doBasicLogin(GenericValue userLogin, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute("userLogin", userLogin);

        String javaScriptEnabled = null;
        try {
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            Map<String, Object> result = dispatcher.runSync("getUserPreference", UtilMisc.toMap("userPrefTypeId", "javaScriptEnabled", "userPrefGroupTypeId", "GLOBAL_PREFERENCES", "userLogin", userLogin));
            javaScriptEnabled = (String) result.get("userPrefValue");
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error getting user preference", module);
        }
        session.setAttribute("javaScriptEnabled", Boolean.valueOf("Y".equals(javaScriptEnabled)));

        //init theme from user preference, clean the current visualTheme value in session and restart the resolution
        UtilHttp.setVisualTheme(session, null);
        UtilHttp.setVisualTheme(session, ThemeFactory.resolveVisualTheme(request));

        ModelEntity modelUserLogin = userLogin.getModelEntity();
        if (modelUserLogin.isField("partyId")) {
            // if partyId is a field, then we should have these relations defined
            try {
                GenericValue person = userLogin.getRelatedOne("Person", false);
                GenericValue partyGroup = userLogin.getRelatedOne("PartyGroup", false);
                if (person != null) session.setAttribute("person", person);
                if (partyGroup != null) session.setAttribute("partyGroup", partyGroup);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error getting person/partyGroup info for session, ignoring...", module);
            }
        }

        // let the visit know who the user is
        VisitHandler.setUserLogin(session, userLogin, false);
    }

    /**
     * An HTTP WebEvent handler that logs out a userLogin by clearing the session.
     *
     * @param request The HTTP request object for the current request.
     * @param response The HTTP response object for the current request.
     * @return Return a boolean which specifies whether or not the calling request
     *        should generate its own content. This allows an event to override the default content.
     */
    public static String logout(HttpServletRequest request, HttpServletResponse response) {
        // run the before-logout events
        RequestHandler rh = RequestHandler.getRequestHandler(request.getSession().getServletContext());
        rh.runBeforeLogoutEvents(request, response);

        // invalidate the security group list cache
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        doBasicLogout(userLogin, request, response);
        
        if (request.getAttribute("_AUTO_LOGIN_LOGOUT_") == null) {
            return autoLoginCheck(request, response);
        }
        return "success";
    }

    public static void doBasicLogout(GenericValue userLogin, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");

        if (security != null && userLogin != null) {
            security.clearUserData(userLogin);
        }

        // set the logged out flag
        if (userLogin != null) {
            LoginWorker.setLoggedOut(userLogin.getString("userLoginId"), delegator);
        }

        // this is a setting we don't want to lose, although it would be good to have a more general solution here...
        String currCatalog = (String) session.getAttribute("CURRENT_CATALOG_ID");
        // also make sure the delegatorName is preserved, especially so that a new Visit can be created
        String delegatorName = (String) session.getAttribute("delegatorName");
        // also save the shopping cart if we have one
        // DON'T save the cart, causes too many problems: security issues with things done in cart to easy to miss, especially bad on public systems; was put in here because of the "not me" link for auto-login stuff, but that is a small problem compared to what it causes
        //ShoppingCart shoppingCart = (ShoppingCart) session.getAttribute("shoppingCart");

        // clean up some request attributes to which may no longer be valid now that user has logged out
        request.removeAttribute("delegator");
        request.removeAttribute("dispatcher");
        request.removeAttribute("security");

        // now empty out the session
        session.invalidate();
        session = request.getSession(true);

        if (EntityUtilProperties.propertyValueEquals("security", "security.login.tomcat.sso", "true")){
            try {
                // log out from Tomcat SSO
                request.logout();
            } catch (ServletException e) {
                Debug.logError(e, module);
            }
        }

        // setup some things that should always be there
        UtilHttp.setInitialRequestInfo(request);

        if (currCatalog != null) session.setAttribute("CURRENT_CATALOG_ID", currCatalog);
        if (delegatorName != null) {
            //Commented it as multi tenancy support is now available for front-store application as well.
            // if there is a tenantId in the delegatorName remove it now so that tenant selection doesn't last beyond logout
            /*if (delegatorName.indexOf('#') > 0) {
                delegatorName = delegatorName.substring(0, delegatorName.indexOf('#'));
            }*/
            session.setAttribute("delegatorName", delegatorName);

            delegator = DelegatorFactory.getDelegator(delegatorName);
            LocalDispatcher dispatcher = WebAppUtil.makeWebappDispatcher(session.getServletContext(), delegator);
            setWebContextObjects(request, response, delegator, dispatcher);
        }

        // DON'T save the cart, causes too many problems: if (shoppingCart != null) session.setAttribute("shoppingCart", new WebShoppingCart(shoppingCart, session));
    }

    // Set an autologin cookie for the webapp if it requests it
    public static String autoLoginSet(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        ServletContext context = request.getServletContext();
        String applicationName = UtilHttp.getApplicationName(request);
        WebappInfo webappInfo = ComponentConfig.getWebappInfo((String) context.getAttribute("_serverId"), applicationName);
                
        if (userLogin != null && 
                ((webappInfo != null && webappInfo.isAutologinCookieUsed())
                || webappInfo == null)) { // When using an empty mountpoint, ie using root as mountpoint. Beware: works only for 1 webapp!
            Cookie autoLoginCookie = new Cookie(getAutoLoginCookieName(request), userLogin.getString("userLoginId"));
            autoLoginCookie.setMaxAge(60 * 60 * 24 * 365);
            autoLoginCookie.setDomain(EntityUtilProperties.getPropertyValue("url", "cookie.domain", delegator));
            autoLoginCookie.setPath( applicationName.equals("root") ? "/" : request.getContextPath());
            autoLoginCookie.setSecure(true);
            autoLoginCookie.setHttpOnly(true);
            response.addCookie(autoLoginCookie);
            return autoLoginCheck(delegator, session, userLogin.getString("userLoginId"));
        } else {
            return "success";
        }
    }

    protected static String getAutoLoginCookieName(HttpServletRequest request) {
        return UtilHttp.getApplicationName(request) + ".autoUserLoginId";
    }

    public static String getAutoUserLoginId(HttpServletRequest request) {
        String autoUserLoginId = null;
        Cookie[] cookies = request.getCookies();
        if (Debug.verboseOn()) {
            Debug.logVerbose("Cookies: " + Arrays.toString(cookies), module);
        }
        if (cookies != null) {
            for (Cookie cookie: cookies) {
                if (cookie.getName().equals(getAutoLoginCookieName(request))) {
                    autoUserLoginId = cookie.getValue();
                    break;
                }
            }
        }
        return autoUserLoginId;
    }
    

    public static String autoLoginCheck(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();

        return autoLoginCheck(delegator, session, getAutoUserLoginId(request));
    }

    private static String autoLoginCheck(Delegator delegator, HttpSession session, String autoUserLoginId) {
        if (autoUserLoginId != null) {
            if (Debug.infoOn()) {
                Debug.logInfo("Running autoLogin check.", module);
            }
            try {
                GenericValue autoUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", autoUserLoginId).queryOne();
                GenericValue person = null;
                GenericValue group = null;
                if (autoUserLogin != null) {
                    session.setAttribute("autoUserLogin", autoUserLogin);

                    ModelEntity modelUserLogin = autoUserLogin.getModelEntity();
                    if (modelUserLogin.isField("partyId")) {
                        person = EntityQuery.use(delegator).from("Person").where("partyId", autoUserLogin.getString("partyId")).queryOne();
                        group = EntityQuery.use(delegator).from("PartyGroup").where("partyId", autoUserLogin.getString("partyId")).queryOne();
                    }
                }
                if (person != null) {
                    session.setAttribute("autoName", person.getString("firstName") + " " + person.getString("lastName"));
                } else if (group != null) {
                    session.setAttribute("autoName", group.getString("groupName"));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get autoUserLogin information: " + e.getMessage(), module);
            }
        }
        return "success";
    }

    public static String autoLoginRemove(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("autoUserLogin");

        // remove the cookie
        if (userLogin != null) {
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            String applicationName = UtilHttp.getApplicationName(request);
            Cookie autoLoginCookie = new Cookie(getAutoLoginCookieName(request), userLogin.getString("userLoginId"));
            autoLoginCookie.setMaxAge(0);
            autoLoginCookie.setDomain(EntityUtilProperties.getPropertyValue("url", "cookie.domain", delegator));
            autoLoginCookie.setPath( applicationName.equals("root") ? "/" : request.getContextPath());
            response.addCookie(autoLoginCookie);
        }
        // remove the session attributes
        session.removeAttribute("autoUserLogin");
        session.removeAttribute("autoName");
        // logout the user if logged in.
        if (session.getAttribute("userLogin") != null) {
            request.setAttribute("_AUTO_LOGIN_LOGOUT_", Boolean.TRUE);
            return logout(request, response);
        }
        return "success";
    }
    
    public static boolean isUserLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession();
        GenericValue currentUserLogin = (GenericValue) session.getAttribute("userLogin");
        if (currentUserLogin != null) {
            String hasLoggedOut = currentUserLogin.getString("hasLoggedOut");
            if (hasLoggedOut != null && "N".equals(hasLoggedOut)) {
                return true;
            }
            // User is not logged in so lets clear the attribute
            session.setAttribute("userLogin", null);
        }
        return false;
    }

    /**
     * This method will log in a user with only their username (userLoginId).
     * @param request
     * @param response
     * @param userLoginId
     * @return Returns "success" if user could be logged in or "error" if there was a problem.
     */
    public static String loginUserWithUserLoginId(HttpServletRequest request, HttpServletResponse response, String userLoginId) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
            if (userLogin != null) {
                String enabled = userLogin.getString("enabled");
                if (enabled == null || "Y".equals(enabled)) {
                    userLogin.set("hasLoggedOut", "N");
                    userLogin.store();

                    // login the user
                    Map<String, Object> ulSessionMap = LoginWorker.getUserLoginSession(userLogin);
                    return doMainLogin(request, response, userLogin, ulSessionMap); // doing the main login
                }
            }
        } catch (GeneralException e) {
            Debug.logError(e, module);
        }
        // Shouldn't be here if all went well
        return "error";
    }

    // preprocessor method to login a user from a HTTP request header (configured in security.properties)
    public static String checkRequestHeaderLogin(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String httpHeader = EntityUtilProperties.getPropertyValue("security", "security.login.http.header", null, delegator);

        // make sure the header field is set in security.properties; if not, then this is disabled and just return
        if (UtilValidate.isNotEmpty(httpHeader)) {

            // make sure the user isn't already logged in
            if (!LoginWorker.isUserLoggedIn(request)) {
                // user is not logged in; check the header field
                String headerValue = request.getHeader(httpHeader);
                if (UtilValidate.isNotEmpty(headerValue)) {
                    return LoginWorker.loginUserWithUserLoginId(request, response, headerValue);
                }
                else {
                    // empty headerValue is not good
                    return "error";
                }
            }
        }

        return "success";
    }

    // preprocessor method to login a user from HttpServletRequest.getRemoteUser() (configured in security.properties)
    public static String checkServletRequestRemoteUserLogin(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Boolean allowRemoteUserLogin = "true".equals(EntityUtilProperties.getPropertyValue("security", "security.login.http.servlet.remoteuserlogin.allow", "false", delegator));
        // make sure logging users via remote user is allowed in security.properties; if not just return
        if (allowRemoteUserLogin) {

            // make sure the user isn't already logged in
            if (!LoginWorker.isUserLoggedIn(request)) {
                // lets grab the remoteUserId
                String remoteUserId = request.getRemoteUser();
                if (UtilValidate.isNotEmpty(remoteUserId)) {
                    return LoginWorker.loginUserWithUserLoginId(request, response, remoteUserId);
                }
                else {
                    // empty remoteUserId is not good
                    return "error";
                }
            }
        }
        Boolean useTomcatSSO = EntityUtilProperties.propertyValueEquals("security", "security.login.tomcat.sso", "true");
        if (useTomcatSSO) {

            // make sure the user isn't already logged in
            if (!LoginWorker.isUserLoggedIn(request)) {
                String remoteUserId = request.getRemoteUser();
                if (UtilValidate.isNotEmpty(remoteUserId)) {
                    return LoginWorker.loginUserWithUserLoginId(request, response, remoteUserId);
                } else {
                    // user is/has logged out at this point
                    return "success";
                }
            }
        }

        return "success";
    }
    // preprocessor method to login a user w/ client certificate see security.properties to configure the pattern of CN
    public static String check509CertLogin(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        boolean doCheck = "true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "security.login.cert.allow", "true", delegator));
        if (doCheck) {
            HttpSession session = request.getSession();
            GenericValue currentUserLogin = (GenericValue) session.getAttribute("userLogin");
            if (currentUserLogin != null) {
                String hasLoggedOut = currentUserLogin.getString("hasLoggedOut");
                if (hasLoggedOut != null && "Y".equals(hasLoggedOut)) {
                    currentUserLogin = null;
                }
            }

            String cnPattern = EntityUtilProperties.getPropertyValue("security", "security.login.cert.pattern", "(.*)", delegator);
            Pattern pattern = Pattern.compile(cnPattern);
            //Debug.logInfo("CN Pattern: " + cnPattern, module);

            if (currentUserLogin == null) {
                X509Certificate[] clientCerts = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate"); // 2.2 spec
                if (clientCerts == null) {
                    clientCerts = (X509Certificate[]) request.getAttribute("javax.net.ssl.peer_certificates"); // 2.1 spec
                }

                if (clientCerts != null) {
                    String userLoginId = null;

                    for (int i = 0; i < clientCerts.length; i++) {
                        //X500Principal x500 = clientCerts[i].getSubjectX500Principal();
                        //Debug.logInfo("Checking client certification for authentication: " + x500.getName(), module);

                        Map<String, String> x500Map = KeyStoreUtil.getCertX500Map(clientCerts[i]);
                        if (i == 0) {
                            String cn = x500Map.get("CN");
                            cn = cn.replaceAll("\\\\", "");
                            Matcher m = pattern.matcher(cn);
                            if (m.matches()) {
                                userLoginId = m.group(1);
                            } else {
                                if (Debug.infoOn()) {
                                    Debug.logInfo("Client certificate CN does not match pattern: [" + cnPattern + "]", module);
                                }
                            }
                        }

                        try {
                            // check for a valid issuer (or generated cert data)
                            if (LoginWorker.checkValidIssuer(delegator, x500Map, clientCerts[i].getSerialNumber())) {
                                //Debug.logInfo("Looking up userLogin from CN: " + userLoginId, module);

                                // CN should match the userLoginId
                                GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
                                if (userLogin != null) {
                                    String enabled = userLogin.getString("enabled");
                                    if (enabled == null || "Y".equals(enabled)) {
                                        userLogin.set("hasLoggedOut", "N");
                                        userLogin.store();

                                        // login the user
                                        Map<String, Object> ulSessionMap = LoginWorker.getUserLoginSession(userLogin);
                                        return doMainLogin(request, response, userLogin, ulSessionMap); // doing the main login
                                    }
                                }
                            }
                        } catch (GeneralException e) {
                            Debug.logError(e, module);
                        }
                    }
                }
            }
        }

        return "success";
    }

    protected static boolean checkValidIssuer(Delegator delegator, Map<String, String> x500Map, BigInteger serialNumber) throws GeneralException {
        List<EntityCondition> conds = new LinkedList<EntityCondition>();
        conds.add(EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeConditionMap("commonName", x500Map.get("CN")),
                EntityCondition.makeConditionMap("commonName", null),
                EntityCondition.makeConditionMap("commonName", "")));

        conds.add(EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeConditionMap("organizationalUnit", x500Map.get("OU")),
                EntityCondition.makeConditionMap("organizationalUnit", null),
                EntityCondition.makeConditionMap("organizationalUnit", "")));

        conds.add(EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeConditionMap("organizationName", x500Map.get("O")),
                EntityCondition.makeConditionMap("organizationName", null),
                EntityCondition.makeConditionMap("organizationName", "")));

        conds.add(EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeConditionMap("cityLocality", x500Map.get("L")),
                EntityCondition.makeConditionMap("cityLocality", null),
                EntityCondition.makeConditionMap("cityLocality", "")));

        conds.add(EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeConditionMap("stateProvince", x500Map.get("ST")),
                EntityCondition.makeConditionMap("stateProvince", null),
                EntityCondition.makeConditionMap("stateProvince", "")));

        conds.add(EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeConditionMap("country", x500Map.get("C")),
                EntityCondition.makeConditionMap("country", null),
                EntityCondition.makeConditionMap("country", "")));

        conds.add(EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeConditionMap("serialNumber", serialNumber.toString(16)),
                EntityCondition.makeConditionMap("serialNumber", null),
                EntityCondition.makeConditionMap("serialNumber", "")));

        EntityConditionList<EntityCondition> condition = EntityCondition.makeCondition(conds);
        if (Debug.infoOn()) {
            Debug.logInfo("Doing issuer lookup: " + condition.toString(), module);
        }
        long count = EntityQuery.use(delegator).from("X509IssuerProvision").where(condition).queryCount();
        return count > 0;
    }

    public static boolean isFlaggedLoggedOut(GenericValue userLogin, Delegator delegator) {
        if ("true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "login.disable.global.logout", delegator))) {
            return false;
        }
        if (userLogin == null || userLogin.get("userLoginId") == null) {
            return true;
        }
        // refresh the login object -- maybe cache this?
        try {
            userLogin.refreshFromCache();
        } catch (GenericEntityException e) {
            if (Debug.warningOn()) {
                Debug.logWarning(e, "Unable to refresh UserLogin", module);
            }
        }
        return (userLogin.get("hasLoggedOut") != null ?
                "Y".equalsIgnoreCase(userLogin.getString("hasLoggedOut")) : false);
    }

    /**
     * Returns <code>true</code> if the specified user is authorized to access the specified web application.
     * @param info
     * @param security
     * @param userLogin
     * @return <code>true</code> if the specified user is authorized to access the specified web application
     */
    public static boolean hasApplicationPermission(ComponentConfig.WebappInfo info, Security security, GenericValue userLogin) {
        // New authorization attribute takes precedence.
        String accessPermission = info.getAccessPermission();
        if (!accessPermission.isEmpty()) {
            return security.hasPermission(accessPermission, userLogin);
        }
        for (String permission: info.getBasePermission()) {
            if (!"NONE".equals(permission) && !security.hasEntityPermission(permission, "_VIEW", userLogin)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasBasePermission(GenericValue userLogin, HttpServletRequest request) {
        Security security = (Security) request.getAttribute("security");
        if (security != null) {
            ServletContext context = request.getServletContext();
            String serverId = (String) context.getAttribute("_serverId");
            // get a context path from the request, if it is empty then assume it is the root mount point
            String contextPath = request.getContextPath();
            if (UtilValidate.isEmpty(contextPath)) {
                contextPath = "/";
            }
            ComponentConfig.WebappInfo info = ComponentConfig.getWebAppInfo(serverId, contextPath);
            if (info != null) {
                return hasApplicationPermission(info, security, userLogin);
            } else {
                if (Debug.infoOn()) {
                    Debug.logInfo("No webapp configuration found for : " + serverId + " / " + contextPath, module);
                }
            }
        } else {
            if (Debug.warningOn()) {
                Debug.logWarning("Received a null Security object from HttpServletRequest", module);
            }
        }
        return true;
    }

    /**
     * Returns a <code>Collection</code> of <code>WebappInfo</code> instances that the specified
     * user is authorized to access.
     * @param security
     * @param userLogin
     * @param serverName
     * @param menuName
     * @return A <code>Collection</code> <code>WebappInfo</code> instances that the specified
     * user is authorized to access
     */
    public static Collection<ComponentConfig.WebappInfo> getAppBarWebInfos(Security security, GenericValue userLogin, String serverName, String menuName) {
        Collection<ComponentConfig.WebappInfo> allInfos = ComponentConfig.getAppBarWebInfos(serverName, menuName);
        Collection<ComponentConfig.WebappInfo> allowedInfos = new ArrayList<ComponentConfig.WebappInfo>(allInfos.size());
        for (ComponentConfig.WebappInfo info : allInfos) {
            if (hasApplicationPermission(info, security, userLogin)) {
                allowedInfos.add(info);
            }
        }
        return allowedInfos;
    }

    public static Map<String, Object> getUserLoginSession(GenericValue userLogin) {
        Delegator delegator = userLogin.getDelegator();
        GenericValue userLoginSession;
        Map<String, Object> userLoginSessionMap = null;
        try {
            userLoginSession = userLogin.getRelatedOne("UserLoginSession", false);
            if (userLoginSession != null) {
                Object deserObj = XmlSerializer.deserialize(userLoginSession.getString("sessionData"), delegator);
                //don't check, just cast, if it fails it will get caught and reported below; if (deserObj instanceof Map)
                userLoginSessionMap = checkMap(deserObj, String.class, Object.class);
            }
        } catch (GenericEntityException ge) {
            if (Debug.warningOn()) {
                Debug.logWarning(ge, "Cannot get UserLoginSession for UserLogin ID: " + userLogin.getString("userLoginId"), module);
            }
        } catch (Exception e) {
            if (Debug.warningOn()) {
                Debug.logWarning(e, "Problems deserializing UserLoginSession", module);
            }
        }
        return userLoginSessionMap;
    }

    public static String autoChangePassword(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String userName = request.getParameter("USERNAME");
        Timestamp now = UtilDateTime.nowTimestamp();
        Integer reqToChangePwdInDays = EntityUtilProperties.getPropertyAsInteger("security", "user.change.password.days", 0);
        Integer passwordNoticePeriod = EntityUtilProperties.getPropertyAsInteger("security", "user.change.password.notification.days", 0);
        if (reqToChangePwdInDays > 0) {
            List<GenericValue> passwordHistories = null;
            try {
                passwordHistories = EntityQuery.use(delegator).from("UserLoginPasswordHistory").where("userLoginId", userName).queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get user's password history record: " + e.getMessage(), module);
            }
            if (UtilValidate.isNotEmpty(passwordHistories)) {
                GenericValue passwordHistory = EntityUtil.getFirst(EntityUtil.filterByDate(passwordHistories));
                Timestamp passwordCreationDate = passwordHistory.getTimestamp("fromDate");
                Integer passwordValidDays = reqToChangePwdInDays - passwordNoticePeriod; // Notification starts after days.
                Timestamp startNotificationFromDate = UtilDateTime.addDaysToTimestamp(passwordCreationDate, passwordValidDays);
                Timestamp passwordExpirationDate = UtilDateTime.addDaysToTimestamp(passwordCreationDate, reqToChangePwdInDays);
                if (now.after(startNotificationFromDate)) {
                    if (now.after(passwordExpirationDate)) {
                        Map<String, String> messageMap = UtilMisc.toMap("passwordExpirationDate", passwordExpirationDate.toString());
                        String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.password_expired_message", messageMap, UtilHttp.getLocale(request));
                        request.setAttribute("_ERROR_MESSAGE_", errMsg);
                        return "requirePasswordChange";
                    } else {
                        Map<String, String> messageMap = UtilMisc.toMap("passwordExpirationDate", passwordExpirationDate.toString());
                        String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.password_expiration_alert", messageMap, UtilHttp.getLocale(request));
                        request.setAttribute("_EVENT_MESSAGE_", errMsg);
                        return "success";
                    }
                }
            }
        }
        return "success";
    }
}
