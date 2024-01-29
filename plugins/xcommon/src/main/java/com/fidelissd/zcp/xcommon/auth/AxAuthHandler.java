/*
 * *****************************************************************************************
 *  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  * Proprietary and confidential                                                           *
 *  * Written Mandeep Sidhu <mandeep.sidhu@fidelissd.com>, June 2019
 *  *****************************************************************************************
 */

package com.fidelissd.zcp.xcommon.auth;

import com.fidelissd.zcp.xcommon.util.EnvironmentUtil;
import com.fidelissd.zcp.xcommon.util.InvalidTokenException;
import com.fidelissd.zcp.xcommon.util.JWTUtils;
import org.apache.http.HttpStatus;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.*;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityConfigurationException;
import org.apache.ofbiz.security.SecurityFactory;
import org.apache.ofbiz.service.*;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.webapp.stats.VisitHandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static org.apache.ofbiz.webapp.control.LoginWorker.logout;


import java.net.UnknownHostException;

public class AxAuthHandler {
    private static final String module = AxAuthHandler.class.getName();
    public static final String resource = "SellerCentralUiLabels";
    public static final String ACCESS_TOKEN_PARAM_KEY = "accessToken";
    private static GenericDelegator delegator = (GenericDelegator) DelegatorFactory
            .getDelegator("default");
    private static LocalDispatcher dispatcher = new GenericDispatcherFactory()
            .createLocalDispatcher("default", delegator);

    /**
     * Returns HTTP Status Code 401 Unauthorized in the response header as opposed to the framework just rendering a
     * HTML screen output with HTML markup saying session has expired, this helps clients handle session expired
     * scenarios efficiently.
     *
     * @param request
     * @param response
     */
    public static void prepareInvalidSessionResponse(HttpServletRequest request, HttpServletResponse response) {
        Debug.log("Preparing session invalid response.");
        try {
            response.sendError( HttpStatus.SC_UNAUTHORIZED, UtilProperties.getMessage(resource, "SessionExpired", request.getLocale()));
        } catch (IOException e) {
            e.printStackTrace();
            Debug.logError(e, module);
        }
    }

    /**
     * Returns server root URL for zeus board react app
     *
     * @param dctx
     * @param context
     */

    public static Map<String, Object> getServerRootUrlForZeusReactBoard(DispatchContext dctx, Map<String, Object> context) throws Exception {
        Map<String, Object> serviceReult = ServiceUtil.returnSuccess();

        String serverRootUrl = "";

        EnvironmentUtil environment;
        try {
            environment = EnvironmentUtil.detectEnvironment();
            switch (environment) {
                case TEST:
                    serverRootUrl = UtilProperties.getPropertyValue("appconfig.properties", "zeus.board.root.url.test");
                    break;
                case DEV:
                    serverRootUrl = UtilProperties.getPropertyValue("appconfig.properties", "zeus.board.root.url.dev");
                    break;
                case PROD:
                    serverRootUrl = UtilProperties.getPropertyValue("appconfig.properties", "zeus.board.root.url.prod");
                    break;
                default:
                    throw new org.apache.commons.lang.NotImplementedException();
            }
        } catch (UnknownHostException e) {
            Debug.logError(e, e.getMessage(), module);
            throw new Exception("Unknown host found");
        }

        serviceReult.put("serverRootUrl", serverRootUrl);
        return serviceReult;
    }

    /**
     * Returns server root URL for project board app
     *
     * @param dctx
     * @param context
     */

    public static Map<String, Object> getServerRootUrlForProjectBoard(DispatchContext dctx, Map<String, Object> context) throws Exception {
        Map<String, Object> serviceReult = ServiceUtil.returnSuccess();

        String serverRootUrl = "";

        EnvironmentUtil environment;
        try {
            environment = EnvironmentUtil.detectEnvironment();
            switch (environment) {
                case TEST:
                    serverRootUrl = UtilProperties.getPropertyValue("appconfig.properties", "project.board.root.url.test");
                    break;
                case DEV:
                    serverRootUrl = UtilProperties.getPropertyValue("appconfig.properties", "project.board.root.url.dev");
                    break;
                case PROD:
                    serverRootUrl = UtilProperties.getPropertyValue("appconfig.properties", "zeus.board.root.url.prod");
                    break;
                default:
                    throw new org.apache.commons.lang.NotImplementedException();
            }
        } catch (UnknownHostException e) {
            Debug.logError(e, e.getMessage(), module);
            throw new Exception("Unknown host found");
        }

        serviceReult.put("serverRootUrl", serverRootUrl);
        return serviceReult;
    }

    /**
     * Returns server root URL for partner (Sellercentral)
     *
     * @param dctx
     * @param context
     */

    public static Map<String, Object> getServerRootUrlForPartner(DispatchContext dctx, Map<String, Object> context) throws Exception {
        Map<String, Object> serviceReult = ServiceUtil.returnSuccess();

        String serverRootUrl = "";

        EnvironmentUtil environment;
        try {
            environment = EnvironmentUtil.detectEnvironment();
            switch (environment) {
                case TEST:
                    serverRootUrl = UtilProperties.getPropertyValue("appconfig.properties", "partner.root.url.test");
                    break;
                case DEV:
                    serverRootUrl = UtilProperties.getPropertyValue("appconfig.properties", "partner.root.url.dev");
                    break;
                case PROD:
                    serverRootUrl = UtilProperties.getPropertyValue("appconfig.properties", "partner.root.url.prod");
                    break;
                default:
                    throw new org.apache.commons.lang.NotImplementedException();
            }
        } catch (UnknownHostException e) {
            Debug.logError(e, e.getMessage(), module);
            throw new Exception("Unknown host found");
        }

        serviceReult.put("serverRootUrl", serverRootUrl);
        return serviceReult;
    }


    public static String checkAccessTokenLogin(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();

        String accessToken = request.getParameter(ACCESS_TOKEN_PARAM_KEY);
        if (accessToken == null) return "success";

        try {
            byte[] decodedAccessTokenBytes = Base64.getDecoder().decode(accessToken.getBytes());
            String decodedAccessToken = new String(decodedAccessTokenBytes);
            //GenericValue userAccessViewRecord = EntityQuery.use(delegator).from("UserAccessView").where("accessToken", accessToken).filterByDate().queryFirst();

            Map<String,Object> jwtMap = JWTUtils.parseJwt(decodedAccessToken);
            if (UtilValidate.isEmpty(jwtMap)){
                Debug.logError("Unable to authorise the logged in user. Please validate the access token header value.", module);
                return "success";
            } else {
                String tokenType = (String) jwtMap.get("tokenType");
                if(UtilValidate.isEmpty(tokenType) || !"ACCESS".equalsIgnoreCase(tokenType)) {
                    Debug.logWarning("Invalid Access Token, please ensure you are not passing in a refresh token instead.", module);
                    return "success";
                } else {
                    Debug.logVerbose("The access token is active.", module);
                    String userLoginId = (String) jwtMap.get("userLoginId");
                    String tenantId = (String) jwtMap.get("tenantId");

                    GenericDelegator tenantDelegator = delegator;
                    LocalDispatcher tenantDispatcher = dispatcher;

                    if(UtilValidate.isNotEmpty(tenantId)) {
                        tenantDelegator = (GenericDelegator) DelegatorFactory.getDelegator("default#" + tenantId);
                        if (UtilValidate.isEmpty(tenantDelegator)) {
                            Debug.logWarning("Invalid tenantId found " + tenantId, module);
                            return "success";
                        }
                        tenantDispatcher = new GenericDispatcherFactory().createLocalDispatcher("default#"+tenantId, tenantDelegator);
                    }
                        GenericValue userLogin = EntityQuery.use(tenantDelegator).from("UserLogin").where("userLoginId", userLoginId).cache(true).queryFirst();

                    if(userLogin!=null){
                        ServletContext servletContext = session.getServletContext();
                        tenantDispatcher = WebAppUtil.makeWebappDispatcher(servletContext, tenantDelegator);
                        setWebContextObjects(request, response, tenantDelegator, tenantDispatcher);
                        logout(request, response);
                        // if the user is already logged in and the login is different, logout the other user
                        Debug.logWarning("Doing basic login now for access token key: " + accessToken, module);
                        doWrappedLogin(userLogin, request);
                    }else{
                        Debug.logWarning("Could not find userLogin for access token key: " + accessToken, module);
                    }

                }
            }
        } catch (IllegalArgumentException | InvalidTokenException e) {
            Debug.logWarning("Unable to authorise the logged in user. Please validate the access token header value.", module);
        } catch (GenericEntityException e) {
            Debug.logWarning("An Error occurred while trying to validate the access token: " + e.getMessage(), module);
        }

        return "success";
    }


    private static void setWebContextObjects(HttpServletRequest request, HttpServletResponse response, Delegator delegator, LocalDispatcher dispatcher) {
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
    }

    private static void doWrappedLogin(GenericValue userLogin, HttpServletRequest request) {
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
}
