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

import java.util.*;
import java.security.cert.X509Certificate;
import java.math.BigInteger;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.transaction.Transaction;
import javax.security.auth.x500.X500Principal;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.webapp.stats.VisitHandler;
import org.ofbiz.common.login.LoginServices;

/**
 * Common Workers
 */
public class LoginWorker {
    
    public final static String module = LoginWorker.class.getName();
    public static final String resourceWebapp = "WebappUiLabels";

    public static final String EXTERNAL_LOGIN_KEY_ATTR = "externalLoginKey";
    public static final String X509_CERT_ATTR = "SSLx509Cert";

    /** This Map is keyed by the randomly generated externalLoginKey and the value is a UserLogin GenericValue object */
    public static Map externalLoginKeys = new HashMap();
    
    public static String makeLoginUrl(PageContext pageContext) {
        return makeLoginUrl(pageContext, "checkLogin");
    }

    public static String makeLoginUrl(ServletRequest request) {
        return makeLoginUrl(request, "checkLogin");
    }
	
    public static String makeLoginUrl(PageContext pageContext, String requestName) {
        return makeLoginUrl(pageContext.getRequest(), requestName);
    }
    public static String makeLoginUrl(ServletRequest request, String requestName) {
        String queryString = null;

        Enumeration parameterNames = request.getParameterNames();

        while (parameterNames != null && parameterNames.hasMoreElements()) {
            String paramName = (String) parameterNames.nextElement();

            if (paramName != null) {
                if (queryString == null) queryString = paramName + "=" + request.getParameter(paramName);
                else queryString = queryString + "&" + paramName + "=" + request.getParameter(paramName);
            }
        }

        String loginUrl = "/" + requestName + "/" + UtilFormatOut.checkNull((String) request.getAttribute("_CURRENT_VIEW_"));

        if (queryString != null) loginUrl = loginUrl + "?" + UtilFormatOut.checkNull(queryString);

        return loginUrl;
    }
    
    /**
     * Gets (and creates if necessary) a key to be used for an external login parameter
     */
    public static String getExternalLoginKey(HttpServletRequest request) {
        //Debug.logInfo("Running getExternalLoginKey, externalLoginKeys.size=" + externalLoginKeys.size(), module);
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");

        String externalKey = (String) request.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
        if (externalKey != null) return externalKey;

        HttpSession session = request.getSession();
        synchronized (session) {
            // if the session has a previous key in place, remove it from the master list
            String sesExtKey = (String) session.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
            if (sesExtKey != null) {
                externalLoginKeys.remove(sesExtKey);
            }

            //check the userLogin here, after the old session setting is set so that it will always be cleared
            if (userLogin == null) return "";

            //no key made yet for this request, create one
            while (externalKey == null || externalLoginKeys.containsKey(externalKey)) {
                externalKey = "EL" + Long.toString(Math.round(Math.random() * 1000000)) + Long.toString(Math.round(Math.random() * 1000000));
            }

            request.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            session.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            externalLoginKeys.put(externalKey, userLogin);
            return externalKey;
        }
    }

    public static void cleanupExternalLoginKey(HttpSession session) {
        String sesExtKey = (String) session.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
        if (sesExtKey != null) {
            externalLoginKeys.remove(sesExtKey);
        }
    }

    public static void setLoggedOut(String userLoginId, GenericDelegator delegator) {
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

                GenericValue userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
                userLogin.set("hasLoggedOut", "Y");
                userLogin.store();
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
                    Debug.logVerbose("Resumed the parent transaction.", module);
                } catch (GenericTransactionException ite) {
                    Debug.logError(ite, "Cannot resume transaction: " + ite.getMessage(), module);
                }
            }
        }
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
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        HttpSession session = request.getSession();

        // anonymous shoppers are not logged in
        if (userLogin != null && "anonymous".equals(userLogin.getString("userLoginId"))) {
            userLogin = null;
        }

        // user is logged in; check to see if they have globally logged out if not
        // check if they have permission for this login attempt; if not log them out
        if (userLogin != null) {
            if (!hasBasePermission(userLogin, request) || isFlaggedLoggedOut(userLogin)) {
                Debug.logInfo("User does not have permission or is flagged as logged out", module);
                doBasicLogout(userLogin, request);
                userLogin = null;

                // have to reget this because the old session object will be invalid
                session = request.getSession();
            }
        }

        String username = null;
        String password = null;

        if (userLogin == null) {
            // check parameters
            if (username == null) username = request.getParameter("USERNAME");
            if (password == null) password = request.getParameter("PASSWORD");
            // check session attributes
            if (username == null) username = (String) session.getAttribute("USERNAME");
            if (password == null) password = (String) session.getAttribute("PASSWORD");

            if ((username != null) && ("true".equalsIgnoreCase(UtilProperties.getPropertyValue("security.properties", "username.lowercase")))) {
                username = username.toLowerCase();
            }
            if ((password != null) && ("true".equalsIgnoreCase(UtilProperties.getPropertyValue("security.properties", "password.lowercase")))) {
                password = password.toLowerCase();
            }

            // in this condition log them in if not already; if not logged in or can't log in, save parameters and return error
            if ((username == null) || (password == null) || ("error".equals(login(request, response)))) {
                Map reqParams = UtilHttp.getParameterMap(request);
                String queryString = UtilHttp.urlEncodeArgs(reqParams);
                Debug.logInfo("reqParams Map: " + reqParams, module);
                Debug.logInfo("queryString: " + queryString, module);

                // make sure this attribute is not in the request; this avoids infinite recursion when a login by less stringent criteria (like not checkout the hasLoggedOut field) passes; this is not a normal circumstance but can happen with custom code or in funny error situations when the userLogin service gets the userLogin object but runs into another problem and fails to return an error 
                request.removeAttribute("_LOGIN_PASSED_");

                session.setAttribute("_PREVIOUS_REQUEST_", request.getPathInfo());
                if (queryString != null && queryString.length() > 0) {
                    session.setAttribute("_PREVIOUS_PARAMS_", queryString);
                }

                if (Debug.infoOn()) Debug.logInfo("checkLogin: queryString=" + queryString, module);
                if (Debug.infoOn()) Debug.logInfo("checkLogin: PathInfo=" + request.getPathInfo(), module);

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

        String username = request.getParameter("USERNAME");
        String password = request.getParameter("PASSWORD");

        if (username == null) username = (String) session.getAttribute("USERNAME");
        if (password == null) password = (String) session.getAttribute("PASSWORD");
        
        // allow a username and/or password in a request attribute to override the request parameter or the session attribute; this way a preprocessor can play with these a bit...
        if (UtilValidate.isNotEmpty((String) request.getAttribute("USERNAME"))) {
            username = (String) request.getAttribute("USERNAME");
        }
        if (UtilValidate.isNotEmpty((String) request.getAttribute("PASSWORD"))) {
            password = (String) request.getAttribute("PASSWORD");
        }

        List unpwErrMsgList = FastList.newInstance();
        if (UtilValidate.isEmpty(username)) {
            unpwErrMsgList.add(UtilProperties.getMessage(resourceWebapp, "loginevents.username_was_empty_reenter", UtilHttp.getLocale(request)));
        }
        if (UtilValidate.isEmpty(password)) {
            unpwErrMsgList.add(UtilProperties.getMessage(resourceWebapp, "loginevents.password_was_empty_reenter", UtilHttp.getLocale(request)));
        }
        if (!unpwErrMsgList.isEmpty()) {
            request.setAttribute("_ERROR_MESSAGE_LIST_", unpwErrMsgList);
            return "error";
        }
        

        if ((username != null) && ("true".equalsIgnoreCase(UtilProperties.getPropertyValue("security.properties", "username.lowercase")))) {
            username = username.toLowerCase();
        }
        if ((password != null) && ("true".equalsIgnoreCase(UtilProperties.getPropertyValue("security.properties", "password.lowercase")))) {
            password = password.toLowerCase();
        }

        // get the visit id to pass to the userLogin for history
        String visitId = VisitHandler.getVisitId(session);

        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map result = null;

        try {
            result = dispatcher.runSync("userLogin", UtilMisc.toMap("login.username", username, "login.password", password, "visitId", visitId, "locale", UtilHttp.getLocale(request)));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling userLogin service", module);
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.following_error_occurred_during_login", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        if (ModelService.RESPOND_SUCCESS.equals(result.get(ModelService.RESPONSE_MESSAGE))) {
            GenericValue userLogin = (GenericValue) result.get("userLogin");
            Map userLoginSession = (Map) result.get("userLoginSession");
            return doMainLogin(request, response, userLogin, userLoginSession);
        } else {
            Map messageMap = UtilMisc.toMap("errorMessage", (String) result.get(ModelService.ERROR_MESSAGE));
            String errMsg = UtilProperties.getMessage(resourceWebapp, "loginevents.following_error_occurred_during_login", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
    }

    public static String doMainLogin(HttpServletRequest request, HttpServletResponse response, GenericValue userLogin, Map userLoginSession) {
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
        return autoLoginSet(request, response);
    }

    public static void doBasicLogin(GenericValue userLogin, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute("userLogin", userLogin);

        ModelEntity modelUserLogin = userLogin.getModelEntity();
        if (modelUserLogin.isField("partyId")) {
            // if partyId is a field, then we should have these relations defined
            try {
                GenericValue person = userLogin.getRelatedOne("Person");
                GenericValue partyGroup = userLogin.getRelatedOne("PartyGroup");
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

        doBasicLogout(userLogin, request);

        if (request.getAttribute("_AUTO_LOGIN_LOGOUT_") == null) {
            return autoLoginCheck(request, response);
        }
        return "success";
    }

    public static void doBasicLogout(GenericValue userLogin, HttpServletRequest request) {
        HttpSession session = request.getSession();

        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");

        if (security != null && userLogin != null) {
            Security.userLoginSecurityGroupByUserLoginId.remove(userLogin.getString("userLoginId"));
        }

        // set the logged out flag
        LoginWorker.setLoggedOut(userLogin.getString("userLoginId"), delegator);

        // this is a setting we don't want to lose, although it would be good to have a more general solution here...
        String currCatalog = (String) session.getAttribute("CURRENT_CATALOG_ID");
        // also make sure the delegatorName is preserved, especially so that a new Visit can be created
        String delegatorName = (String) session.getAttribute("delegatorName");
        // also save the shopping cart if we have one
        // DON'T save the cart, causes too many problems: security issues with things done in cart to easy to miss, especially bad on public systems; was put in here because of the "not me" link for auto-login stuff, but that is a small problem compared to what it causes
        //ShoppingCart shoppingCart = (ShoppingCart) session.getAttribute("shoppingCart");

        session.invalidate();
        session = request.getSession(true);

        if (currCatalog != null) session.setAttribute("CURRENT_CATALOG_ID", currCatalog);
        if (delegatorName != null) session.setAttribute("delegatorName", delegatorName);
        // DON'T save the cart, causes too many problems: if (shoppingCart != null) session.setAttribute("shoppingCart", new WebShoppingCart(shoppingCart, session));
    }

    public static String autoLoginSet(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        if (userLogin != null) {
            Cookie autoLoginCookie = new Cookie(getAutoLoginCookieName(request), userLogin.getString("userLoginId"));
            autoLoginCookie.setMaxAge(60 * 60 * 24 * 365);
            autoLoginCookie.setPath("/");
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
        if (Debug.verboseOn()) Debug.logVerbose("Cookies:" + cookies, module);
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(getAutoLoginCookieName(request))) {
                    autoUserLoginId = cookies[i].getValue();
                    break;
                }
            }
        }
        return autoUserLoginId;
    }

    public static String autoLoginCheck(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();

        return autoLoginCheck(delegator, session, getAutoUserLoginId(request));
    }

    private static String autoLoginCheck(GenericDelegator delegator, HttpSession session, String autoUserLoginId) {
        if (autoUserLoginId != null) {
            Debug.logInfo("Running autoLogin check.", module);
            try {
                GenericValue autoUserLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", autoUserLoginId));
                GenericValue person = null;
                GenericValue group = null;
                if (autoUserLogin != null) {
                    session.setAttribute("autoUserLogin", autoUserLogin);

                    ModelEntity modelUserLogin = autoUserLogin.getModelEntity();
                    if (modelUserLogin.isField("partyId")) {
                        person = delegator.findByPrimaryKey("Person", UtilMisc.toMap("partyId", autoUserLogin.getString("partyId")));
                        group = delegator.findByPrimaryKey("PartyGroup", UtilMisc.toMap("partyId", autoUserLogin.getString("partyId")));
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
            Cookie autoLoginCookie = new Cookie(getAutoLoginCookieName(request), userLogin.getString("userLoginId"));
            autoLoginCookie.setMaxAge(0);
            autoLoginCookie.setPath("/");
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

    public static String check509CertLogin(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();
        GenericValue currentUserLogin = (GenericValue) session.getAttribute("userLogin");
        if (currentUserLogin != null) {
            String hasLoggedOut = currentUserLogin.getString("hasLoggedOut");
            if (hasLoggedOut != null && "Y".equals(hasLoggedOut)) {
                currentUserLogin = null;
            }
        }

        if (currentUserLogin == null) {
            X509Certificate[] clientCerts = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate"); // 2.2 spec
            if (clientCerts == null) {
                clientCerts = (X509Certificate[]) request.getAttribute("javax.net.ssl.peer_certificates"); // 2.1 spec
            }

            if (clientCerts != null) {
                String userLoginId = null;

                for (int i = 0; i < clientCerts.length; i++) {
                    X500Principal x500 = clientCerts[i].getSubjectX500Principal();
                    Debug.log("Checking client certification for authentication: " + x500.getName(), module);
                    
                    Map x500Map = KeyStoreUtil.getCertX500Map(clientCerts[i]);
                    if (i == 0) {
                        userLoginId = (String) x500Map.get("CN");
                    }

                    try {
                        // check for a valid issuer (or generated cert data)
                        if (LoginWorker.checkValidIssuer(delegator, x500Map, clientCerts[i].getSerialNumber())) {
                            Debug.log("Looking up userLogin from CN: " + userLoginId, module);
                            
                            // CN should match the userLoginId
                            GenericValue userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
                            if (userLogin != null) {
                                String enabled = userLogin.getString("enabled");
                                if (enabled == null || "Y".equals(enabled)) {
                                    userLogin.set("hasLoggedOut", "N");
                                    userLogin.store();

                                    // login the user
                                    Map ulSessionMap = LoginServices.getUserLoginSession(userLogin);
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

        return "success";
    }

    protected static boolean checkValidIssuer(GenericDelegator delegator, Map x500Map, BigInteger serialNumber) throws GeneralException {
        List conds = FastList.newInstance();
        conds.add(new EntityConditionList(UtilMisc.toList(new EntityExpr("commonName", EntityOperator.EQUALS, x500Map.get("CN")),
                new EntityExpr("commonName", EntityOperator.EQUALS, null),
                new EntityExpr("commonName", EntityOperator.EQUALS, "")), EntityOperator.OR));

        conds.add(new EntityConditionList(UtilMisc.toList(new EntityExpr("organizationalUnit", EntityOperator.EQUALS, x500Map.get("OU")),
                new EntityExpr("organizationalUnit", EntityOperator.EQUALS, null),
                new EntityExpr("organizationalUnit", EntityOperator.EQUALS, "")), EntityOperator.OR));

        conds.add(new EntityConditionList(UtilMisc.toList(new EntityExpr("organizationName", EntityOperator.EQUALS, x500Map.get("O")),
                new EntityExpr("organizationName", EntityOperator.EQUALS, null),
                new EntityExpr("organizationName", EntityOperator.EQUALS, "")), EntityOperator.OR));

        conds.add(new EntityConditionList(UtilMisc.toList(new EntityExpr("cityLocality", EntityOperator.EQUALS, x500Map.get("L")),
                new EntityExpr("cityLocality", EntityOperator.EQUALS, null),
                new EntityExpr("cityLocality", EntityOperator.EQUALS, "")), EntityOperator.OR));

        conds.add(new EntityConditionList(UtilMisc.toList(new EntityExpr("stateProvince", EntityOperator.EQUALS, x500Map.get("ST")),
                new EntityExpr("stateProvince", EntityOperator.EQUALS, null),
                new EntityExpr("stateProvince", EntityOperator.EQUALS, "")), EntityOperator.OR));

        conds.add(new EntityConditionList(UtilMisc.toList(new EntityExpr("country", EntityOperator.EQUALS, x500Map.get("C")),
                new EntityExpr("country", EntityOperator.EQUALS, null),
                new EntityExpr("country", EntityOperator.EQUALS, "")), EntityOperator.OR));

        conds.add(new EntityConditionList(UtilMisc.toList(new EntityExpr("serialNumber", EntityOperator.EQUALS, serialNumber.toString(16)),
                new EntityExpr("serialNumber", EntityOperator.EQUALS, null),
                new EntityExpr("serialNumber", EntityOperator.EQUALS, "")), EntityOperator.OR));

        EntityConditionList condition = new EntityConditionList(conds, EntityOperator.AND);
        Debug.log("Doing issuer lookup: " + condition.toString(), module);
        long count = delegator.findCountByCondition("X509IssuerProvision", condition, null, null);
        return count > 0;
    }

    public static String checkExternalLoginKey(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();

        String externalKey = request.getParameter(LoginWorker.EXTERNAL_LOGIN_KEY_ATTR);
        if (externalKey == null) return "success";

        GenericValue userLogin = (GenericValue) LoginWorker.externalLoginKeys.get(externalKey);
        if (userLogin != null) {
            // found userLogin, do the external login...

            // if the user is already logged in and the login is different, logout the other user
            GenericValue currentUserLogin = (GenericValue) session.getAttribute("userLogin");
            if (currentUserLogin != null) {
                if (currentUserLogin.getString("userLoginId").equals(userLogin.getString("userLoginId"))) {
                    // is the same user, just carry on...
                    return "success";
                }

                // logout the current user and login the new user...
                logout(request, response);
                // ignore the return value; even if the operation failed we want to set the new UserLogin
            }

            doBasicLogin(userLogin, request);
        } else {
            Debug.logWarning("Could not find userLogin for external login key: " + externalKey, module);
        }

        return "success";
    }

    public static boolean isFlaggedLoggedOut(GenericValue userLogin) {
        if ("true".equalsIgnoreCase(UtilProperties.getPropertyValue("security.properties", "login.disable.global.logout"))) {
            return false;
        }
        if (userLogin == null || userLogin.get("userLoginId") == null) {
            return true;
        }
        // refresh the login object -- maybe cache this?
        try {
            userLogin.refreshFromCache();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Unable to refresh UserLogin", module);
        }
        return (userLogin.get("hasLoggedOut") != null ?
                "Y".equalsIgnoreCase(userLogin.getString("hasLoggedOut")) : false);
    }

    protected static boolean hasBasePermission(GenericValue userLogin, HttpServletRequest request) {
        ServletContext context = (ServletContext) request.getAttribute("servletContext");
        Security security = (Security) request.getAttribute("security");

        String serverId = (String) context.getAttribute("_serverId");
        String contextPath = request.getContextPath();

        ComponentConfig.WebappInfo info = ComponentConfig.getWebAppInfo(serverId, contextPath);
        if (security != null) {
            if (info != null) {
                String[] permissions = info.getBasePermission();
                for (int i = 0; i < permissions.length; i++) {
                    if (!"NONE".equals(permissions[i]) && !security.hasEntityPermission(permissions[i], "_VIEW", userLogin)) {
                        return false;
                    }
                }
            } else {
                Debug.logInfo("No webapp configuration found for : " + serverId + " / " + contextPath, module);
            }
        } else {
            Debug.logWarning("Received a null Security object from HttpServletRequest", module);
        }

        return true;
    }
}
