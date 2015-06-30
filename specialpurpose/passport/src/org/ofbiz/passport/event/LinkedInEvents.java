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
package org.ofbiz.passport.event;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.ofbiz.passport.user.LinkedInAuthenticator;
import org.ofbiz.passport.util.PassportUtil;
import org.ofbiz.base.conversion.ConversionException;
import org.ofbiz.base.conversion.JSONConverters.JSONToMap;
import org.ofbiz.base.crypto.HashCrypt;
import org.ofbiz.base.lang.JSON;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.authentication.api.AuthenticatorException;
import org.ofbiz.common.login.LoginServices;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * LinkedEvents - Events for LinkedIn login.
 * 
 * Refs: https://developer.linkedin.com/documents/authentication
 * 
 */
public class LinkedInEvents {

    public static final String module = LinkedInEvents.class.getName();
    
    public static final String resource = "PassportUiLabels";
    
    public static final String AuthorizeUri = "/uas/oauth2/authorization";
    
    public static final String TokenServiceUri = "/uas/oauth2/accessToken";
    
    public static final String UserApiUri = "/v1/people/~";

    public static final String DEFAULT_SCOPE = "r_basicprofile%20r_emailaddress";
    
    public static final String TokenEndpoint = "https://www.linkedin.com";
    
    public static final String SESSION_LINKEDIN_STATE = "_LINKEDIN_STATE_";

    public static final String envPrefix = UtilProperties.getPropertyValue(LinkedInAuthenticator.props, "linkedin.env.prefix", "test");

    /**
     * Redirect to LinkedIn login page.
     * 
     * @return 
     */
    public static String linkedInRedirect(HttpServletRequest request, HttpServletResponse response) {
        GenericValue oauth2LinkedIn = getOAuth2LinkedInConfig(request);
        if (UtilValidate.isEmpty(oauth2LinkedIn)) {
            return "error";
        }
        
        String clientId = oauth2LinkedIn.getString(PassportUtil.ApiKeyLabel);
        String returnURI = oauth2LinkedIn.getString(envPrefix + PassportUtil.ReturnUrlLabel);
        
        // Get user authorization code
        try {
            String state = System.currentTimeMillis() + String.valueOf((new Random(10)).nextLong());
            request.getSession().setAttribute(SESSION_LINKEDIN_STATE, state);
            String redirectUrl = TokenEndpoint + AuthorizeUri
                    + "?client_id=" + clientId
                    + "&response_type=code"
                    + "&scope=" + DEFAULT_SCOPE
                    + "&redirect_uri=" + URLEncoder.encode(returnURI, "UTF-8")
                    + "&state=" + state;
            response.sendRedirect(redirectUrl);
        } catch (NullPointerException e) {
            String errMsg = UtilProperties.getMessage(resource, "RedirectToLinkedInOAuth2NullException", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        } catch (IOException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.toString());
            String errMsg = UtilProperties.getMessage(resource, "RedirectToLinkedInOAuth2Error", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        
        return "success";
    }

    /**
     * Parse LinkedIn login response and login the user if possible.
     * 
     * @return 
     */
    public static String parseLinkedInResponse(HttpServletRequest request, HttpServletResponse response) {
        String authorizationCode = request.getParameter(PassportUtil.COMMON_CODE);
        String state = request.getParameter(PassportUtil.COMMON_STATE);
        if (!state.equals(request.getSession().getAttribute(SESSION_LINKEDIN_STATE))) {
            String errMsg = UtilProperties.getMessage(resource, "LinkedInFailedToMatchState", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        if (UtilValidate.isEmpty(authorizationCode)) {
            String error = request.getParameter(PassportUtil.COMMON_ERROR);
            String errorDescpriton = request.getParameter(PassportUtil.COMMON_ERROR_DESCRIPTION);
            String errMsg = null;
            try {
                errMsg = UtilProperties.getMessage(resource, "FailedToGetLinkedInAuthorizationCode", UtilMisc.toMap(PassportUtil.COMMON_ERROR, error, PassportUtil.COMMON_ERROR_DESCRIPTION, URLDecoder.decode(errorDescpriton, "UTF-8")), UtilHttp.getLocale(request));
            } catch (UnsupportedEncodingException e) {
                errMsg = UtilProperties.getMessage(resource, "GetLinkedInAuthorizationCodeError", UtilHttp.getLocale(request));
            }
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        // Debug.logInfo("LinkedIn authorization code: " + authorizationCode, module);
        
        GenericValue oauth2LinkedIn = getOAuth2LinkedInConfig(request);
        if (UtilValidate.isEmpty(oauth2LinkedIn)) {
            String errMsg = UtilProperties.getMessage(resource, "GetOAuth2LinkedInConfigError", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String clientId = oauth2LinkedIn.getString(PassportUtil.ApiKeyLabel);
        String secret = oauth2LinkedIn.getString(PassportUtil.SecretKeyLabel);
        String returnURI = oauth2LinkedIn.getString(envPrefix + PassportUtil.ReturnUrlLabel);
        
        // Grant token from authorization code and oauth2 token
        // Use the authorization code to obtain an access token
        String accessToken = null;
        
        HttpClient jsonClient = new HttpClient();
        PostMethod postMethod = new PostMethod(TokenEndpoint + TokenServiceUri);
        try {
            HttpMethodParams params = new HttpMethodParams();
            String queryString = "client_id=" + clientId
                    + "&client_secret=" + secret
                    + "&grant_type=authorization_code"
                    + "&code=" + authorizationCode
                    + "&redirect_uri=" + URLEncoder.encode(returnURI, "UTF-8");
            // Debug.logInfo("LinkedIn get access token query string: " + queryString, module);
            postMethod.setQueryString(queryString);
            params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
            postMethod.setParams(params);
            jsonClient.executeMethod(postMethod);
            // Debug.logInfo("LinkedIn get access token response code: " + postMethod.getStatusCode(), module);
            // Debug.logInfo("LinkedIn get access token response content: " + postMethod.getResponseBodyAsString(1024), module);
            if (postMethod.getStatusCode() == HttpStatus.SC_OK) {
                // Debug.logInfo("Json Response from LinkedIn: " + postMethod.getResponseBodyAsString(1024), module);
                JSON jsonObject = JSON.from(postMethod.getResponseBodyAsString(1024));
                JSONToMap jsonMap = new JSONToMap();
                Map<String, Object> userMap = jsonMap.convert(jsonObject);
                accessToken = (String) userMap.get("access_token");
                // Debug.logInfo("Generated Access Token : " + accessToken, module);
            } else {
                String errMsg = UtilProperties.getMessage(resource, "GetOAuth2LinkedInAccessTokenError", UtilMisc.toMap("error", postMethod.getResponseBodyAsString()), UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } catch (UnsupportedEncodingException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (HttpException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (IOException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (ConversionException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } finally {
            postMethod.releaseConnection();
        }
        
        // Get User Profile
        GetMethod getMethod = new GetMethod(TokenEndpoint + UserApiUri + "?oauth2_access_token=" + accessToken);
        Document userInfo = null;
        try {
            userInfo = LinkedInAuthenticator.getUserInfo(getMethod, UtilHttp.getLocale(request));
        } catch (HttpException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (IOException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (AuthenticatorException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (SAXException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (ParserConfigurationException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } finally {
            getMethod.releaseConnection();
        }
        // Debug.logInfo("LinkedIn User Info:" + userInfo, module);
        
        // Store the user info and check login the user
        return checkLoginLinkedInUser(request, userInfo, accessToken);
    }

    private static String checkLoginLinkedInUser(HttpServletRequest request, Document userInfo, String accessToken) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        String linkedInUserId = LinkedInAuthenticator.getLinkedInUserId(userInfo);
        GenericValue linkedInUser = null;
        try {
            linkedInUser = delegator.findOne("LinkedInUser", UtilMisc.toMap("linkedInUserId", linkedInUserId), false);
        } catch (GenericEntityException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        if (UtilValidate.isNotEmpty(linkedInUser)) {
            boolean dataChanged = false;
            if (!accessToken.equals(linkedInUser.getString("accessToken"))) {
                linkedInUser.set("accessToken", accessToken);
                dataChanged = true;
            }
            if (!envPrefix.equals(linkedInUser.getString("envPrefix"))) {
                linkedInUser.set("envPrefix", envPrefix);
                dataChanged = true;
            }
            if (!productStoreId.equals(linkedInUser.getString("productStoreId"))) {
                linkedInUser.set("productStoreId", productStoreId);
                dataChanged = true;
            }
            if (dataChanged) {
                try {
                    linkedInUser.store();
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(), module);
                }
            }
        } else {
            linkedInUser = delegator.makeValue("LinkedInUser", UtilMisc.toMap("accessToken", accessToken, 
                                                                          "productStoreId", productStoreId, 
                                                                          "envPrefix", envPrefix, 
                                                                          "linkedInUserId", linkedInUserId));
            try {
                linkedInUser.create();
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
            }
        }
        try {
            GenericValue userLogin = EntityUtil.getFirst(delegator.findByAnd("UserLogin", UtilMisc.toMap("externalAuthId", linkedInUserId), null, false));
            LinkedInAuthenticator authn = new LinkedInAuthenticator();
            authn.initialize(dispatcher);
            if (UtilValidate.isEmpty(userLogin)) {
                String userLoginId = authn.createUser(userInfo);
                userLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", userLoginId), false);
            }
            String password = PassportUtil.randomString();
            boolean useEncryption = "true".equals(UtilProperties.getPropertyValue("security.properties", "password.encrypt"));
            userLogin.set("currentPassword", useEncryption ? HashCrypt.digestHash(LoginServices.getHashType(), null, password) : password);
            userLogin.store();
            request.setAttribute("USERNAME", userLogin.getString("userLoginId"));
            request.setAttribute("PASSWORD", password);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (AuthenticatorException e) {
            Debug.logError(e.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        }
        return "success";
    }

    public static GenericValue getOAuth2LinkedInConfig(HttpServletRequest request) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        try {
            return getOAuth2LinkedInConfig(delegator, productStoreId);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.toString());
            String errMsg = UtilProperties.getMessage(resource, "GetOAuth2LinkedInError", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
        }
        return null;
    }
    
    public static GenericValue getOAuth2LinkedInConfig(Delegator delegator, String productStoreId) throws GenericEntityException {
        return EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("OAuth2LinkedIn", UtilMisc.toMap("productStoreId", productStoreId), null, false)));
    }
}
