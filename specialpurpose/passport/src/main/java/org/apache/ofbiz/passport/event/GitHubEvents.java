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
package org.apache.ofbiz.passport.event;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.ofbiz.passport.user.GitHubAuthenticator;
import org.apache.ofbiz.passport.util.PassportUtil;
import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.JSONConverters.JSONToMap;
import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.authentication.api.AuthenticatorException;
import org.apache.ofbiz.common.login.LoginServices;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * GitHubEvents - Events for GitHub login.
 * 
 * Refs: https://developer.github.com/v3/oauth/
 * 
 */
public class GitHubEvents {

    public static final String module = GitHubEvents.class.getName();
    
    public static final String resource = "PassportUiLabels";
    
    public static final String AuthorizeUri = "/login/oauth/authorize";
    
    public static final String TokenServiceUri = "/login/oauth/access_token";
    
    public static final String UserApiUri = "/user";

    public static final String DEFAULT_SCOPE = "user,gist";
    
    public static final String ApiEndpoint = "https://api.github.com";
    
    public static final String TokenEndpoint = "https://github.com";
    
    public static final String SESSION_GITHUB_STATE = "_GITHUB_STATE_";

    public static final String envPrefix = UtilProperties.getPropertyValue(GitHubAuthenticator.props, "github.env.prefix", "test");

    /**
     * Redirect to GitHub login page.
     * 
     * @return 
     */
    public static String gitHubRedirect(HttpServletRequest request, HttpServletResponse response) {
        GenericValue oauth2GitHub = getOAuth2GitHubConfig(request);
        if (UtilValidate.isEmpty(oauth2GitHub)) {
            return "error";
        }
        
        String clientId = oauth2GitHub.getString(PassportUtil.COMMON_CLIENT_ID);
        String returnURI = oauth2GitHub.getString(PassportUtil.COMMON_RETURN_RUL);
        
        // Get user authorization code
        try {
            String state = System.currentTimeMillis() + String.valueOf((new Random(10)).nextLong());
            request.getSession().setAttribute(SESSION_GITHUB_STATE, state);
            String redirectUrl = TokenEndpoint + AuthorizeUri
                    + "?client_id=" + clientId
                    + "&scope=" + DEFAULT_SCOPE
                    + "&redirect_uri=" + URLEncoder.encode(returnURI, "UTF-8")
                    + "&state=" + state;
            Debug.logInfo("Request to GitHub: " + redirectUrl, module);
            response.sendRedirect(redirectUrl);
        } catch (NullPointerException e) {
            String errMsg = UtilProperties.getMessage(resource, "RedirectToGitHubOAuth2NullException", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        } catch (IOException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.toString());
            String errMsg = UtilProperties.getMessage(resource, "RedirectToGitHubOAuth2Error", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        
        return "success";
    }

    /**
     * Parse GitHub login response and login the user if possible.
     * 
     * @return 
     */
    public static String parseGitHubResponse(HttpServletRequest request, HttpServletResponse response) {
        String authorizationCode = request.getParameter(PassportUtil.COMMON_CODE);
        String state = request.getParameter(PassportUtil.COMMON_STATE);
        if (!state.equals(request.getSession().getAttribute(SESSION_GITHUB_STATE))) {
            String errMsg = UtilProperties.getMessage(resource, "GitHubFailedToMatchState", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        if (UtilValidate.isEmpty(authorizationCode)) {
            String error = request.getParameter(PassportUtil.COMMON_ERROR);
            String errorDescpriton = request.getParameter(PassportUtil.COMMON_ERROR_DESCRIPTION);
            String errMsg = null;
            try {
                errMsg = UtilProperties.getMessage(resource, "FailedToGetGitHubAuthorizationCode", UtilMisc.toMap(PassportUtil.COMMON_ERROR, error, PassportUtil.COMMON_ERROR_DESCRIPTION, URLDecoder.decode(errorDescpriton, "UTF-8")), UtilHttp.getLocale(request));
            } catch (UnsupportedEncodingException e) {
                errMsg = UtilProperties.getMessage(resource, "GetGitHubAuthorizationCodeError", UtilHttp.getLocale(request));
            }
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        Debug.logInfo("GitHub authorization code: " + authorizationCode, module);
        
        GenericValue oauth2GitHub = getOAuth2GitHubConfig(request);
        if (UtilValidate.isEmpty(oauth2GitHub)) {
            String errMsg = UtilProperties.getMessage(resource, "GetOAuth2GitHubConfigError", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String clientId = oauth2GitHub.getString(PassportUtil.COMMON_CLIENT_ID);
        String secret = oauth2GitHub.getString(PassportUtil.COMMON_CLIENT_SECRET);
        String returnURI = oauth2GitHub.getString(PassportUtil.COMMON_RETURN_RUL);
        
        // Grant token from authorization code and oauth2 token
        // Use the authorization code to obtain an access token
        String accessToken = null;
        String tokenType = null;
        
        try {
            URI uri = new URIBuilder()
                    .setScheme(TokenEndpoint.substring(0, TokenEndpoint.indexOf(":")))
                    .setHost(TokenEndpoint.substring(TokenEndpoint.indexOf(":") + 3))
                    .setPath(TokenServiceUri)
                    .setParameter("client_id", clientId)
                    .setParameter("client_secret", secret)
                    .setParameter("code", authorizationCode)
                    .setParameter("redirect_uri", returnURI)
                    .build();
            HttpPost postMethod = new HttpPost(uri);
            CloseableHttpClient jsonClient = HttpClients.custom().build();
            // Debug.logInfo("GitHub get access token query string: " + postMethod.getURI(), module);
            postMethod.setConfig(PassportUtil.StandardRequestConfig);
            postMethod.setHeader(PassportUtil.ACCEPT_HEADER, "application/json");
            CloseableHttpResponse postResponse = jsonClient.execute(postMethod);
            String responseString = new BasicResponseHandler().handleResponse(postResponse);
            // Debug.logInfo("GitHub get access token response code: " + postResponse.getStatusLine().getStatusCode(), module);
            // Debug.logInfo("GitHub get access token response content: " + responseString, module);
            if (postResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Debug.logInfo("Json Response from GitHub: " + responseString, module);
                JSON jsonObject = JSON.from(responseString);
                JSONToMap jsonMap = new JSONToMap();
                Map<String, Object> userMap = jsonMap.convert(jsonObject);
                accessToken = (String) userMap.get("access_token");
                tokenType = (String) userMap.get("token_type");
                // Debug.logInfo("Generated Access Token : " + accessToken, module);
                // Debug.logInfo("Token Type: " + tokenType, module);
            } else {
                String errMsg = UtilProperties.getMessage(resource, "GetOAuth2GitHubAccessTokenError", UtilMisc.toMap("error", responseString), UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } catch (UnsupportedEncodingException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (IOException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (ConversionException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } catch (URISyntaxException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
		}
        
        // Get User Profile
        HttpGet getMethod = new HttpGet(ApiEndpoint + UserApiUri);
        Map<String, Object> userInfo = null;
        try {
            userInfo = GitHubAuthenticator.getUserInfo(getMethod, accessToken, tokenType, UtilHttp.getLocale(request));
        } catch (AuthenticatorException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        } finally {
            getMethod.releaseConnection();
        }
        // Debug.logInfo("GitHub User Info:" + userInfo, module);
        
        // Store the user info and check login the user
        return checkLoginGitHubUser(request, userInfo, accessToken);
    }

    private static String checkLoginGitHubUser(HttpServletRequest request, Map<String, Object> userInfo, String accessToken) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        String gitHubUserId = (String) userInfo.get("login");
        GenericValue gitHubUser = null;
        try {
            gitHubUser = delegator.findOne("GitHubUser", UtilMisc.toMap("gitHubUserId", gitHubUserId), false);
        } catch (GenericEntityException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        if (UtilValidate.isNotEmpty(gitHubUser)) {
            boolean dataChanged = false;
            if (!accessToken.equals(gitHubUser.getString("accessToken"))) {
                gitHubUser.set("accessToken", accessToken);
                dataChanged = true;
            }
            if (!envPrefix.equals(gitHubUser.getString("envPrefix"))) {
                gitHubUser.set("envPrefix", envPrefix);
                dataChanged = true;
            }
            if (!productStoreId.equals(gitHubUser.getString("productStoreId"))) {
                gitHubUser.set("productStoreId", productStoreId);
                dataChanged = true;
            }
            if (dataChanged) {
                try {
                    gitHubUser.store();
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(), module);
                }
            }
        } else {
            gitHubUser = delegator.makeValue("GitHubUser", UtilMisc.toMap("accessToken", accessToken, 
                                                                          "productStoreId", productStoreId, 
                                                                          "envPrefix", envPrefix, 
                                                                          "gitHubUserId", gitHubUserId));
            try {
                gitHubUser.create();
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
            }
        }
        try {
            GenericValue userLogin = EntityUtil.getFirst(delegator.findByAnd("UserLogin", UtilMisc.toMap("externalAuthId", gitHubUserId), null, false));
            GitHubAuthenticator authn = new GitHubAuthenticator();
            authn.initialize(dispatcher);
            if (UtilValidate.isEmpty(userLogin)) {
                String userLoginId = authn.createUser(userInfo);
                userLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", userLoginId), false);
            }
            String autoPassword = RandomStringUtils.randomAlphanumeric(EntityUtilProperties.getPropertyAsInteger("security", "password.length.min", 5).intValue());
            boolean useEncryption = "true".equals(UtilProperties.getPropertyValue("security", "password.encrypt"));
            userLogin.set("currentPassword", useEncryption ? HashCrypt.digestHash(LoginServices.getHashType(), null, autoPassword) : autoPassword);
            userLogin.store();
            request.setAttribute("USERNAME", userLogin.getString("userLoginId"));
            request.setAttribute("PASSWORD", autoPassword);
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

    public static GenericValue getOAuth2GitHubConfig(HttpServletRequest request) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        try {
            return getOAuth2GitHubConfig(delegator, productStoreId);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.toString());
            String errMsg = UtilProperties.getMessage(resource, "GetOAuth2GitHubError", messageMap, UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
        }
        return null;
    }
    
    public static GenericValue getOAuth2GitHubConfig(Delegator delegator, String productStoreId) throws GenericEntityException {
        return EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("OAuth2GitHub", UtilMisc.toMap("productStoreId", productStoreId), null, false)));
    }
}
