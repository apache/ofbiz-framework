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

package org.apache.ofbiz.common.login;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transaction;

import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.authentication.AuthHelper;
import org.apache.ofbiz.common.authentication.api.AuthenticatorException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.tomcat.util.res.StringManager;

/**
 * <b>Title:</b> Login Services
 */
public class LoginServices {

    private static final String MODULE = LoginServices.class.getName();
    private static final String RESOURCE = "SecurityextUiLabels";

    /**
     * Login service to authenticate username and password
     * @return Map of results including (userLogin) GenericValue object
     */
    public static Map<String, Object> userLogin(DispatchContext ctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();

        // load the external auth modules -- note: this will only run once and cache the objects
        if (!AuthHelper.authenticatorsLoaded()) {
            AuthHelper.loadAuthenticators(dispatcher);
        }

        // Authenticate to LDAP if configured to do so
        // TODO: this should be moved to using the NEW Authenticator API
        if ("true".equals(EntityUtilProperties.getPropertyValue("security", "security.ldap.enable", delegator))) {
            if (!LdapAuthenticationServices.userLogin(ctx, context)) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.ldap_authentication_failed", locale);
                if ("true".equals(EntityUtilProperties.getPropertyValue("security", "security.ldap.fail.login", delegator))) {
                    return ServiceUtil.returnError(errMsg);
                }
                Debug.logInfo(errMsg, MODULE);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        boolean useEncryption = "true".equals(EntityUtilProperties.getPropertyValue("security", "password.encrypt", delegator));

        // if isServiceAuth is not specified, default to not a service auth
        boolean isServiceAuth = context.get("isServiceAuth") != null && (Boolean) context.get("isServiceAuth");

        String username = (String) context.get("login.username");
        if (username == null) {
            username = (String) context.get("username");
        }
        String password = (String) context.get("login.password");
        if (password == null) {
            password = (String) context.get("password");
        }
        String jwtToken = (String) context.get("login.token");
        if (jwtToken == null) {
            jwtToken = (String) context.get("token");
        }

        // get the visitId for the history entity
        String visitId = (String) context.get("visitId");

        String errMsg = "";
        if (UtilValidate.isEmpty(username)) {
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.username_missing", locale);
        } else if (UtilValidate.isEmpty(password) && UtilValidate.isEmpty(jwtToken)) {
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_missing", locale);
        } else {

            if ("true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "username.lowercase", delegator))) {
                username = username.toLowerCase(Locale.getDefault());
            }
            if ("true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "password.lowercase", delegator))) {
                password = password.toLowerCase(Locale.getDefault());
            }

            boolean repeat = true;
            // starts at zero but it incremented at the beginning so in the first pass passNumber will be 1
            int passNumber = 0;

            while (repeat) {
                repeat = false;
                // pass number is incremented here because there are continues in this loop so it may never get to the end
                passNumber++;

                GenericValue userLogin = null;

                try {
                    // only get userLogin from cache for service calls; for web and other manual logins there is less time sensitivity
                    userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", username).cache(isServiceAuth).queryOne();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, "", MODULE);
                }

                // see if any external auth modules want to sync the user info
                if (userLogin == null) {
                    try {
                        AuthHelper.syncUser(username);
                    } catch (AuthenticatorException e) {
                        Debug.logWarning(e, MODULE);
                    }

                    // check the user login object again
                    try {
                        userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", username).cache(isServiceAuth).queryOne();
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, "", MODULE);
                    }
                }

                if (userLogin != null) {
                    String ldmStr = EntityUtilProperties.getPropertyValue("security", "login.disable.minutes", delegator);
                    long loginDisableMinutes;

                    try {
                        loginDisableMinutes = Long.parseLong(ldmStr);
                    } catch (Exception e) {
                        loginDisableMinutes = 30;
                        Debug.logWarning("Could not parse login.disable.minutes from security.properties, using default of 30", MODULE);
                    }

                    Timestamp disabledDateTime = userLogin.getTimestamp("disabledDateTime");
                    Timestamp reEnableTime = null;

                    if (loginDisableMinutes > 0 && disabledDateTime != null) {
                        reEnableTime = new Timestamp(disabledDateTime.getTime() + loginDisableMinutes * 60000);
                    }

                    boolean doStore = true;
                    // we might change & store this userLogin, so we should clone it here to get a mutable copy
                    userLogin = GenericValue.create(userLogin);

                    // get the is system flag -- system accounts can only be used for service authentication
                    boolean isSystem = (isServiceAuth && userLogin.get("isSystem") != null) ? "Y".equalsIgnoreCase(userLogin.getString("isSystem"))
                                                                                            : false;

                    // grab the hasLoggedOut flag
                    Boolean hasLoggedOut = userLogin.getBoolean("hasLoggedOut");

                    if ((UtilValidate.isEmpty(userLogin.getString("enabled")) || "Y".equals(userLogin.getString("enabled"))
                            || (reEnableTime != null && reEnableTime.before(UtilDateTime.nowTimestamp())) || (isSystem))
                            && UtilValidate.isEmpty(userLogin.getString("disabledBy"))) {
                        String successfulLogin;
                        if (!isSystem) {
                            userLogin.set("enabled", "Y");
                            userLogin.set("disabledBy", null);
                        }
                        // attempt to authenticate with Authenticator class(es)
                        boolean authFatalError = false;
                        boolean externalAuth = false;
                        try {
                            externalAuth = AuthHelper.authenticate(username, password, isServiceAuth);
                        } catch (AuthenticatorException e) {
                            // fatal error -- or single authenticator found -- fail now
                            Debug.logWarning(e, MODULE);
                            authFatalError = true;

                        }

                        // check whether to sign in with Tomcat SSO
                        boolean useTomcatSSO = EntityUtilProperties.propertyValueEquals("security", "security.login.tomcat.sso", "true");
                        HttpServletRequest request = (javax.servlet.http.HttpServletRequest) context.get("request");
                        // when request is not supplied, we will treat that SSO is not required as
                        // in the usage of userLogin service in ICalWorker.java and XmlRpcEventHandler.java.
                        useTomcatSSO = useTomcatSSO && (request != null);

                        // resolve the key for decrypt the token and control the validity
                        boolean jwtTokenValid = SecurityUtil.authenticateUserLoginByJWT(delegator, username, jwtToken);

                        // if the password.accept.encrypted.and.plain property in security is set to true allow plain or encrypted passwords
                        // if this is a system account don't bother checking the passwords
                        // if externalAuth passed; this is run as well
                        if ((!authFatalError && externalAuth) || (useTomcatSSO && tomcatSSOLogin(request, username, password))
                                || (jwtToken != null && jwtTokenValid)
                                || (password != null && checkPassword(userLogin.getString("currentPassword"), useEncryption, password))) {
                            Debug.logVerbose("[LoginServices.userLogin] : Password Matched or Token Validated", MODULE);

                            // update the hasLoggedOut flag
                            if (hasLoggedOut == null || hasLoggedOut) {
                                userLogin.set("hasLoggedOut", "N");
                            }

                            // reset failed login count if necessary
                            Long currentFailedLogins = userLogin.getLong("successiveFailedLogins");
                            if (currentFailedLogins != null && currentFailedLogins > 0) {
                                userLogin.set("successiveFailedLogins", 0L);
                            } else if (hasLoggedOut != null && !hasLoggedOut) {
                                // successful login & no logout flag, no need to change anything, so don't do the store
                                doStore = false;
                            }

                            successfulLogin = "Y";

                            if (!isServiceAuth) {
                                // get the UserLoginSession if this is not a service auth
                                Map<?, ?> userLoginSessionMap = LoginWorker.getUserLoginSession(userLogin);

                                // return the UserLoginSession Map
                                if (userLoginSessionMap != null) {
                                    result.put("userLoginSession", userLoginSessionMap);
                                }
                            }

                            result.put("userLogin", userLogin);
                            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                        } else {
                            // password is incorrect, but this may be the result of a stale cache entry,
                            // so lets clear the cache and try again if this is the first pass
                            // but only if authFatalError is not true; this would mean the single authenticator failed
                            if (!authFatalError && isServiceAuth && passNumber <= 1) {
                                delegator.clearCacheLine("UserLogin", UtilMisc.toMap("userLoginId", username));
                                repeat = true;
                                continue;
                            }
                            Debug.logInfo("[LoginServices.userLogin] : Password Incorrect", MODULE);
                            // password invalid...
                            if (password != null) {
                                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_incorrect", locale);
                            } else if (jwtToken != null) {
                                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.token_incorrect", locale);
                            }
                            // increment failed login count
                            Long currentFailedLogins = userLogin.getLong("successiveFailedLogins");

                            if (currentFailedLogins == null) {
                                currentFailedLogins = 1L;
                            } else {
                                currentFailedLogins = currentFailedLogins + 1;
                            }
                            userLogin.set("successiveFailedLogins", currentFailedLogins);

                            // if failed logins over amount in properties file, disable account
                            String mflStr = EntityUtilProperties.getPropertyValue("security", "max.failed.logins", delegator);
                            long maxFailedLogins = 3;
                            try {
                                maxFailedLogins = Long.parseLong(mflStr);
                            } catch (Exception e) {
                                maxFailedLogins = 3;
                                Debug.logWarning("Could not parse max.failed.logins from security.properties, using default of 3", MODULE);
                            }

                            if (maxFailedLogins > 0 && currentFailedLogins >= maxFailedLogins) {
                                userLogin.set("enabled", "N");
                                userLogin.set("disabledDateTime", UtilDateTime.nowTimestamp());
                            }

                            successfulLogin = "N";
                        }

                        // this section is being done in its own transaction rather than in the
                        // current/existing transaction because we may return error and we don't
                        // want that to stop this from getting stored
                        Transaction parentTx = null;
                        boolean beganTransaction = false;

                        try {
                            try {
                                parentTx = TransactionUtil.suspend();
                            } catch (GenericTransactionException e) {
                                Debug.logError(e, "Could not suspend transaction: " + e.getMessage(), MODULE);
                            }

                            try {
                                beganTransaction = TransactionUtil.begin();

                                if (doStore) {
                                    userLogin.store();
                                }

                                if ("true".equals(EntityUtilProperties.getPropertyValue("security", "store.login.history", delegator))) {
                                    boolean createHistory = true;

                                    // only save info on service auth if option set to true to do so
                                    if (isServiceAuth && !"true".equals(
                                            EntityUtilProperties.getPropertyValue("security", "store.login.history.on.service.auth", delegator))) {
                                        createHistory = false;
                                    }

                                    if (createHistory) {
                                        Map<String, Object> ulhCreateMap = UtilMisc.toMap("userLoginId", username, "visitId", visitId, "fromDate",
                                                UtilDateTime.nowTimestamp(), "successfulLogin", successfulLogin);

                                        ModelEntity modelUserLogin = userLogin.getModelEntity();
                                        if (modelUserLogin.isField("partyId")) {
                                            ulhCreateMap.put("partyId", userLogin.get("partyId"));
                                        }

                                        // ONLY save the password if it was incorrect
                                        if ("N".equals(successfulLogin) && !"false".equals(EntityUtilProperties.getPropertyValue("security",
                                                "store.login.history.incorrect.password", delegator))) {
                                            ulhCreateMap.put("passwordUsed", password);
                                        }

                                        delegator.create("UserLoginHistory", ulhCreateMap);
                                    }
                                }
                            } catch (GenericEntityException e) {
                                String geeErrMsg = "Error saving UserLoginHistory";
                                if (doStore) {
                                    geeErrMsg += " and updating login status to reset hasLoggedOut, unsuccessful login count, etc.";
                                }
                                geeErrMsg += ": " + e.toString();
                                try {
                                    TransactionUtil.rollback(beganTransaction, geeErrMsg, e);
                                } catch (GenericTransactionException e2) {
                                    Debug.logError(e2, "Could not rollback nested transaction: " + e2.getMessage(), MODULE);
                                }

                                // if doStore is true then this error should not be ignored and we shouldn't consider it a successful login if this
                                // happens as there is something very wrong lower down that will bite us again later
                                if (doStore) {
                                    return ServiceUtil.returnError(geeErrMsg);
                                }
                            } finally {
                                try {
                                    TransactionUtil.commit(beganTransaction);
                                } catch (GenericTransactionException e) {
                                    Debug.logError(e, "Could not commit nested transaction: " + e.getMessage(), MODULE);
                                }
                            }
                        } finally {
                            // resume/restore parent transaction
                            if (parentTx != null) {
                                try {
                                    TransactionUtil.resume(parentTx);
                                    Debug.logVerbose("Resumed the parent transaction.", MODULE);
                                } catch (GenericTransactionException e) {
                                    Debug.logError(e, "Could not resume parent nested transaction: " + e.getMessage(), MODULE);
                                }
                            }
                        }
                    } else {
                        // account is disabled, but this may be the result of a stale cache entry,
                        // so lets clear the cache and try again if this is the first pass
                        if (isServiceAuth && passNumber <= 1) {
                            delegator.clearCacheLine("UserLogin", UtilMisc.toMap("userLoginId", username));
                            repeat = true;
                            continue;
                        }
                        Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("username", username);
                        errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.account_for_user_login_id_disabled", messageMap, locale);
                        if (disabledDateTime != null) {
                            messageMap = UtilMisc.<String, Object>toMap("disabledDateTime", disabledDateTime);
                            errMsg += " " + UtilProperties.getMessage(RESOURCE, "loginservices.since_datetime", messageMap, locale);
                        } else {
                            errMsg += ".";
                        }

                        if (loginDisableMinutes > 0 && reEnableTime != null) {
                            messageMap = UtilMisc.<String, Object>toMap("reEnableTime", reEnableTime);
                            errMsg += " " + UtilProperties.getMessage(RESOURCE, "loginservices.will_be_reenabled", messageMap, locale);
                        } else {
                            errMsg += " " + UtilProperties.getMessage(RESOURCE, "loginservices.not_scheduled_to_be_reenabled", locale);
                        }
                    }
                } else {
                    // no userLogin object; there may be a non-syncing authenticator
                    boolean externalAuth = false;
                    try {
                        externalAuth = AuthHelper.authenticate(username, password, isServiceAuth);
                    } catch (AuthenticatorException e) {
                        errMsg = e.getMessage();
                        Debug.logError(e, "External Authenticator had fatal exception : " + e.getMessage(), MODULE);
                    }
                    if (externalAuth) {
                        // external auth passed - create a placeholder object for session
                        userLogin = delegator.makeValue("UserLogin");
                        userLogin.set("userLoginId", username);
                        userLogin.set("enabled", "Y");
                        userLogin.set("hasLoggedOut", "N");
                        result.put("userLogin", userLogin);
                        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                        // TODO: more than this is needed to support 100% external authentication
                        // TODO: party + security information is needed; Userlogin will need to be stored
                    } else {
                        // userLogin record not found, user does not exist
                        errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.user_not_found", locale);
                        Debug.logInfo("[LoginServices.userLogin] Invalid User : '" + username + "'; " + errMsg, MODULE);
                    }
                }
            }
        }

        if (errMsg.length() > 0) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
        }
        return result;
    }

    /**
     * Login service to authenticate a username without password, storing history
     *
     * @return Map of results including (userLogin) GenericValue object
     */
    public static Map<String, Object> userImpersonate(DispatchContext ctx, Map<String, ?> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = ctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String userLoginIdToImpersonate = (String) context.get("userLoginIdToImpersonate");
        GenericValue originUserLogin = (GenericValue) context.get("userLogin");
        // get the visitId for the history entity
        String visitId = (String) context.get("visitId");

        if ("true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "username.lowercase", delegator))) {
            userLoginIdToImpersonate = userLoginIdToImpersonate.toLowerCase();
        }

        GenericValue userLogin;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginIdToImpersonate).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        // check impersonation controls
        String errorMessage = checkImpersonationControls(delegator, originUserLogin, userLogin, locale);
        if (errorMessage != null) {
            return ServiceUtil.returnError(errorMessage);
        }

        // return the UserLoginSession Map
        Map<String, Object> userLoginSessionMap = LoginWorker.getUserLoginSession(userLogin);
        if (userLoginSessionMap != null) {
            result.put("userLoginSession", userLoginSessionMap);
        }

        // grab the hasLoggedOut flag
        boolean hasLoggedOut = "Y".equalsIgnoreCase(userLogin.getString("hasLoggedOut"));
        if (hasLoggedOut || UtilValidate.isEmpty(userLogin.getString("hasLoggedOut"))) {
            userLogin.set("hasLoggedOut", "N");
            try {
                userLogin.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // Log impersonation in UserLoginHistory
        Map<String, Object> historyCreateMap = UtilMisc.toMap("userLoginId", userLoginIdToImpersonate);
        historyCreateMap.put("visitId", visitId);
        historyCreateMap.put("fromDate", UtilDateTime.nowTimestamp());
        historyCreateMap.put("successfulLogin", "Y");
        historyCreateMap.put("partyId", userLogin.get("partyId"));
        historyCreateMap.put("originUserLoginId", originUserLogin.get("userLoginId"));
        // End impersonation in one hour max
        historyCreateMap.put("thruDate", UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), Calendar.HOUR, 1));
        try {
            delegator.create("UserLoginHistory", historyCreateMap);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        result.put("userLogin", userLogin);
        result.put("originUserLogin", originUserLogin);
        return result;
    }

    /**
     * Return error message if a needed control has failed : userLoginToImpersonate must exist Impersonation have to be enabled Check
     * userLoginIdToImpersonate is active, not Admin and not equals to userLogin Check userLogin has enough permission
     *
     * @param delegator
     * @param userLogin
     * @param userLoginToImpersonate
     * @param locale
     * @return
     */
    private static String checkImpersonationControls(Delegator delegator, GenericValue userLogin, GenericValue userLoginToImpersonate,
            Locale locale) {
        if (userLoginToImpersonate == null) {
            return UtilProperties.getMessage(RESOURCE, "loginservices.username_missing", locale);
        }
        String userLoginId = userLogin.getString("userLoginId");
        String userLoginIdToImpersonate = userLoginToImpersonate.getString("userLoginId");

        if (UtilProperties.getPropertyAsBoolean("security", "security.disable.impersonation", true)) {
            return UtilProperties.getMessage(RESOURCE, "loginevents.impersonation_disabled", locale);
        }

        if (!LoginWorker.isUserLoginActive(userLoginToImpersonate)) {
            Map<String, Object> messageMap = UtilMisc.toMap("username", userLoginIdToImpersonate);
            return UtilProperties.getMessage(RESOURCE, "loginservices.account_for_user_login_id_disabled", messageMap, locale);
        }

        if (SecurityUtil.hasUserLoginAdminPermission(delegator, userLoginIdToImpersonate)) {
            return UtilProperties.getMessage(RESOURCE, "loginevents.impersonate_notAdmin", locale);
        }

        if (userLoginIdToImpersonate.equals(userLoginId)) {
            return UtilProperties.getMessage(RESOURCE, "loginevents.impersonate_yourself", locale);
        }

        // Cannot impersonate more privileged user
        List<String> missingNeededPermissions = SecurityUtil.hasUserLoginMorePermissionThan(delegator, userLoginId, userLoginIdToImpersonate);
        if (UtilValidate.isNotEmpty(missingNeededPermissions)) {
            String missingPermissionListString = missingNeededPermissions.stream().collect(Collectors.joining(", "));
            return UtilProperties.getMessage(RESOURCE, "loginevents.impersonate_notEnoughPermission",
                    UtilMisc.toMap("missingPermissions", missingPermissionListString), locale);
        }

        return null;
    }

    public static void createUserLoginPasswordHistory(GenericValue userLogin) throws GenericEntityException {
        int passwordChangeHistoryLimit = 0;
        Delegator delegator = userLogin.getDelegator();
        String userLoginId = userLogin.getString("userLoginId");
        String currentPassword = userLogin.getString("currentPassword");
        try {
            passwordChangeHistoryLimit = EntityUtilProperties.getPropertyAsInteger("security", "password.change.history.limit", 0);
        } catch (NumberFormatException nfe) {
            // No valid value is found so don't bother to save any password history
            passwordChangeHistoryLimit = 0;
        }
        if (passwordChangeHistoryLimit == 0 || passwordChangeHistoryLimit < 0) {
            // Not saving password history, so return from here.
            return;
        }
        EntityQuery eq = EntityQuery.use(delegator).from("UserLoginPasswordHistory").where("userLoginId", userLoginId).orderBy("-fromDate")
                .cursorScrollInsensitive();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        try (EntityListIterator eli = eq.queryIterator()) {
            GenericValue pwdHist;
            if ((pwdHist = eli.next()) != null) {
                // updating password so set end date on previous password in history
                pwdHist.set("thruDate", nowTimestamp);
                pwdHist.store();
                // check if we have hit the limit on number of password changes to be saved. If we did then delete the oldest password from history.
                eli.last();
                int rowIndex = eli.currentIndex();
                if (rowIndex == passwordChangeHistoryLimit) {
                    eli.afterLast();
                    pwdHist = eli.previous();
                    pwdHist.remove();
                }
            }
        }

        // save this password in history
        GenericValue userLoginPwdHistToCreate = delegator.makeValue("UserLoginPasswordHistory",
                UtilMisc.toMap("userLoginId", userLoginId, "fromDate", nowTimestamp));
        userLoginPwdHistToCreate.set("currentPassword", currentPassword);
        userLoginPwdHistToCreate.create();
    }

    /**
     * Creates a UserLogin
     * @param ctx
     *            The DispatchContext that this service is operating in
     * @param context
     *            Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createUserLogin(DispatchContext ctx, Map<String, ?> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue loggedInUserLogin = (GenericValue) context.get("userLogin");
        List<String> errorMessageList = new LinkedList<>();
        Locale locale = (Locale) context.get("locale");

        boolean useEncryption = "true".equals(EntityUtilProperties.getPropertyValue("security", "password.encrypt", delegator));

        String userLoginId = (String) context.get("userLoginId");
        String partyId = (String) context.get("partyId");
        String currentPassword = (String) context.get("currentPassword");
        String currentPasswordVerify = (String) context.get("currentPasswordVerify");
        String enabled = (String) context.get("enabled");
        String passwordHint = (String) context.get("passwordHint");
        String requirePasswordChange = (String) context.get("requirePasswordChange");
        String externalAuthId = (String) context.get("externalAuthId");
        String errMsg = null;

        // security: don't create a user login if the specified partyId (if not empty) already exists
        // unless the logged in user has permission to do so (same partyId or PARTYMGR_CREATE)
        if (UtilValidate.isNotEmpty(partyId)) {
            GenericValue party = null;

            try {
                party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "", MODULE);
            }

            if (party != null) {
                if (loggedInUserLogin != null) {
                    // <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
                    if (!partyId.equals(loggedInUserLogin.getString("partyId"))) {
                        if (!security.hasEntityPermission("PARTYMGR", "_CREATE", loggedInUserLogin)) {

                            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.party_with_specified_party_ID_exists_not_have_permission",
                                    locale);
                            errorMessageList.add(errMsg);
                        }
                    }
                } else {
                    errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.must_be_logged_in_and_permission_create_login_party_ID_exists",
                            locale);
                    errorMessageList.add(errMsg);
                }
            }
        }

        GenericValue userLoginToCreate = delegator.makeValue("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
        checkNewPassword(userLoginToCreate, null, currentPassword, currentPasswordVerify, passwordHint, errorMessageList, true, locale);
        userLoginToCreate.set("externalAuthId", externalAuthId);
        userLoginToCreate.set("passwordHint", passwordHint);
        userLoginToCreate.set("enabled", enabled);
        userLoginToCreate.set("requirePasswordChange", requirePasswordChange);
        userLoginToCreate.set("currentPassword", useEncryption ? HashCrypt.cryptUTF8(getHashType(), null, currentPassword) : currentPassword);
        try {
            userLoginToCreate.set("partyId", partyId);
        } catch (Exception e) {
            // Will get thrown in framework-only installation
            Debug.logInfo(e, "Exception thrown while setting UserLogin partyId field: ", MODULE);
        }

        try {
            EntityCondition condition = EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("userLoginId"), EntityOperator.EQUALS,
                    EntityFunction.UPPER(userLoginId));
            if (UtilValidate.isNotEmpty(EntityQuery.use(delegator).from("UserLogin").where(condition).queryList())) {
                Map<String, String> messageMap = UtilMisc.toMap("userLoginId", userLoginId);
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_create_login_user_with_ID_exists", messageMap, locale);
                errorMessageList.add(errMsg);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_create_login_user_read_failure", messageMap, locale);
            errorMessageList.add(errMsg);
        }

        if (errorMessageList.size() > 0) {
            return ServiceUtil.returnError(errorMessageList);
        }

        try {
            userLoginToCreate.create();
            createUserLoginPasswordHistory(userLoginToCreate);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_create_login_user_write_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates UserLogin Password info
     * @param ctx
     *            The DispatchContext that this service is operating in
     * @param context
     *            Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updatePassword(DispatchContext ctx, Map<String, ?> context) {
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue loggedInUserLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil
                .returnSuccess(UtilProperties.getMessage(RESOURCE, "loginevents.password_was_changed_with_success", locale));

        // load the external auth modules -- note: this will only run once and cache the objects
        if (!AuthHelper.authenticatorsLoaded()) {
            AuthHelper.loadAuthenticators(ctx.getDispatcher());
        }

        boolean useEncryption = "true".equals(EntityUtilProperties.getPropertyValue("security", "password.encrypt", delegator));
        boolean adminUser = false;

        String userLoginId = (String) context.get("userLoginId");
        String errMsg = null;

        if (UtilValidate.isEmpty(userLoginId)) {
            userLoginId = loggedInUserLogin.getString("userLoginId");
        }

        GenericValue userLoginToUpdate;

        try {
            userLoginToUpdate = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_change_password_read_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        // <b>security check</b>: userLogin userLoginId must equal userLoginId, or must have PARTYMGR_UPDATE permission
        // NOTE: must check permission first so that admin users can set own password without specifying old password
        // TODO: change this security group because we can't use permission groups defined in the applications from the framework.
        if (!security.hasEntityPermission("PARTYMGR", "_UPDATE", loggedInUserLogin)) {
            if (!userLoginId.equals(loggedInUserLogin.getString("userLoginId"))) {
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.not_have_permission_update_password_for_user_login", locale);
                return ServiceUtil.returnError(errMsg);
            }
            if (UtilValidate.isNotEmpty(context.get("login.token"))) {
                adminUser = SecurityUtil.authenticateUserLoginByJWT(delegator, userLoginId, (String) context.get("login.token"));
            }
        } else {
            adminUser = true;
        }

        String currentPassword = (String) context.get("currentPassword");
        String newPassword = (String) context.get("newPassword");
        String newPasswordVerify = (String) context.get("newPasswordVerify");
        String passwordHint = (String) context.get("passwordHint");

        if (userLoginToUpdate == null) {
            // this may be a full external authenticator; first try authenticating
            boolean authenticated = false;
            try {
                authenticated = AuthHelper.authenticate(userLoginId, currentPassword, true);
            } catch (AuthenticatorException e) {
                // safe to ignore this; but we'll log it just in case
                Debug.logWarning(e, e.getMessage(), MODULE);
            }

            // call update password if auth passed
            if (authenticated) {
                try {
                    AuthHelper.updatePassword(userLoginId, currentPassword, newPassword);
                } catch (AuthenticatorException e) {
                    Debug.logError(e, e.getMessage(), MODULE);
                    Map<String, String> messageMap = UtilMisc.toMap("userLoginId", userLoginId);
                    errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_change_password_userlogin_with_id_not_exist", messageMap,
                            locale);
                    return ServiceUtil.returnError(errMsg);
                }
                // result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
                result.put("updatedUserLogin", null);
                return result;
            }
            Map<String, String> messageMap = UtilMisc.toMap("userLoginId", userLoginId);
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_change_password_userlogin_with_id_not_exist", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if ("true".equals(EntityUtilProperties.getPropertyValue("security", "password.lowercase", delegator))) {
            currentPassword = currentPassword.toLowerCase(Locale.getDefault());
            newPassword = newPassword.toLowerCase(Locale.getDefault());
            newPasswordVerify = newPasswordVerify.toLowerCase(Locale.getDefault());
        }

        List<String> errorMessageList = new LinkedList<>();
        if (newPassword != null) {
            checkNewPassword(userLoginToUpdate, currentPassword, newPassword, newPasswordVerify, passwordHint, errorMessageList, adminUser, locale);
        }

        if (errorMessageList.size() > 0) {
            return ServiceUtil.returnError(errorMessageList);
        }

        String externalAuthId = userLoginToUpdate.getString("externalAuthId");
        if (UtilValidate.isNotEmpty(externalAuthId)) {
            // external auth is set; don't update the database record
            try {
                AuthHelper.updatePassword(externalAuthId, currentPassword, newPassword);
            } catch (AuthenticatorException e) {
                Debug.logError(e, e.getMessage(), MODULE);
                Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_change_password_write_failure", messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            userLoginToUpdate.set("currentPassword", useEncryption ? HashCrypt.cryptUTF8(getHashType(), null, newPassword) : newPassword, false);
            userLoginToUpdate.set("passwordHint", passwordHint, false);
            // optional parameter in service definition "requirePasswordChange" to update a password to a new generated value that has to be changed
            // by the user
            userLoginToUpdate.set("requirePasswordChange", ("Y".equals(context.get("requirePasswordChange")) ? "Y" : "N"));

            try {
                userLoginToUpdate.store();
                createUserLoginPasswordHistory(userLoginToUpdate);
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_change_password_write_failure", messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }
        }

        result.put("updatedUserLogin", userLoginToUpdate);
        return result;
    }

    /**
     * Updates the UserLoginId for a party, replicating password, etc from current login and expiring the old login.
     * @param ctx
     *            The DispatchContext that this service is operating in
     * @param context
     *            Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateUserLoginId(DispatchContext ctx, Map<String, ?> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        Delegator delegator = ctx.getDelegator();
        GenericValue loggedInUserLogin = (GenericValue) context.get("userLogin");
        List<String> errorMessageList = new LinkedList<>();
        Locale locale = (Locale) context.get("locale");

        String userLoginId = (String) context.get("userLoginId");
        String errMsg = null;

        if ((userLoginId != null) && ("true".equals(EntityUtilProperties.getPropertyValue("security", "username.lowercase", delegator)))) {
            userLoginId = userLoginId.toLowerCase(Locale.getDefault());
        }

        String partyId = loggedInUserLogin.getString("partyId");
        String password = loggedInUserLogin.getString("currentPassword");
        String passwordHint = loggedInUserLogin.getString("passwordHint");

        // security: don't create a user login if the specified partyId (if not empty) already exists
        // unless the logged in user has permission to do so (same partyId or PARTYMGR_CREATE)
        if (UtilValidate.isNotEmpty(partyId)) {
            if (!loggedInUserLogin.isEmpty()) {
                // security check: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
                if (!partyId.equals(loggedInUserLogin.getString("partyId"))) {
                    errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.party_with_party_id_exists_not_permission_create_user_login", locale);
                    errorMessageList.add(errMsg);
                }
            } else {
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.must_logged_in_have_permission_create_user_login_exists", locale);
                errorMessageList.add(errMsg);
            }
        }

        GenericValue newUserLogin = null;
        boolean doCreate = true;

        // check to see if there's a matching login and use it if it's for the same party
        try {
            newUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_create_login_user_read_failure", messageMap, locale);
            errorMessageList.add(errMsg);
        }

        if (newUserLogin != null) {
            if (!newUserLogin.get("partyId").equals(partyId)) {
                Map<String, String> messageMap = UtilMisc.toMap("userLoginId", userLoginId);
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_create_login_user_with_ID_exists", messageMap, locale);
                errorMessageList.add(errMsg);
            } else {
                doCreate = false;
            }
        } else {
            newUserLogin = delegator.makeValue("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
        }

        newUserLogin.set("passwordHint", passwordHint);
        newUserLogin.set("partyId", partyId);
        newUserLogin.set("currentPassword", password);
        newUserLogin.set("enabled", "Y");
        newUserLogin.set("disabledDateTime", null);

        if (errorMessageList.size() > 0) {
            return ServiceUtil.returnError(errorMessageList);
        }

        try {
            if (doCreate) {
                newUserLogin.create();
            } else {
                newUserLogin.store();
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_create_login_user_write_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        // Deactivate 'old' UserLogin and do not set disabledDateTime here, otherwise the 'old' UserLogin would be reenabled by next login
        loggedInUserLogin.set("enabled", "N");
        loggedInUserLogin.set("disabledDateTime", null);

        try {
            loggedInUserLogin.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_disable_old_login_user_write_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        result.put("newUserLogin", newUserLogin);
        return result;
    }

    /**
     * Updates UserLogin Security info
     * @param ctx
     *            The DispatchContext that this service is operating in
     * @param context
     *            Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateUserLoginSecurity(DispatchContext ctx, Map<String, ?> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue loggedInUserLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String userLoginId = (String) context.get("userLoginId");
        String errMsg = null;

        if (UtilValidate.isEmpty(userLoginId)) {
            userLoginId = loggedInUserLogin.getString("userLoginId");
        }

        // <b>security check</b>: must have PARTYMGR_UPDATE permission
        if (!security.hasEntityPermission("PARTYMGR", "_UPDATE", loggedInUserLogin)
                && !security.hasEntityPermission("SECURITY", "_UPDATE", loggedInUserLogin)) {
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.not_permission_update_security_info_for_user_login", locale);
            return ServiceUtil.returnError(errMsg);
        }

        GenericValue userLoginToUpdate = null;

        try {
            userLoginToUpdate = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_change_password_read_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (userLoginToUpdate == null) {
            Map<String, String> messageMap = UtilMisc.toMap("userLoginId", userLoginId);
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_change_password_userlogin_with_id_not_exist", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        boolean wasEnabled = !"N".equals(userLoginToUpdate.get("enabled"));

        if (context.containsKey("enabled")) {
            userLoginToUpdate.set("enabled", context.get("enabled"), true);
        }
        if (context.containsKey("disabledDateTime")) {
            userLoginToUpdate.set("disabledDateTime", context.get("disabledDateTime"), true);
        }
        if (context.containsKey("successiveFailedLogins")) {
            userLoginToUpdate.set("successiveFailedLogins", context.get("successiveFailedLogins"), true);
        }
        if (context.containsKey("externalAuthId")) {
            userLoginToUpdate.set("externalAuthId", context.get("externalAuthId"), true);
        }
        if (context.containsKey("userLdapDn")) {
            userLoginToUpdate.set("userLdapDn", context.get("userLdapDn"), true);
        }
        if (context.containsKey("requirePasswordChange")) {
            userLoginToUpdate.set("requirePasswordChange", context.get("requirePasswordChange"), true);
        }

        // if was disabled and we are enabling it, clear disabledDateTime
        if (!wasEnabled && "Y".equals(context.get("enabled"))) {
            userLoginToUpdate.set("disabledDateTime", null);
            userLoginToUpdate.set("disabledBy", null);
        }

        if ("N".equals(context.get("enabled"))) {
            userLoginToUpdate.set("disabledBy", loggedInUserLogin.getString("userLoginId"));
        }

        try {
            userLoginToUpdate.store();
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.could_not_change_password_write_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static void checkNewPassword(GenericValue userLogin, String currentPassword, String newPassword, String newPasswordVerify,
            String passwordHint, List<String> errorMessageList, boolean ignoreCurrentPassword, Locale locale) {
        Delegator delegator = userLogin.getDelegator();
        boolean useEncryption = "true".equals(EntityUtilProperties.getPropertyValue("security", "password.encrypt", delegator));

        String errMsg = null;

        // if it's a system account (aka adminUser) don't bother checking the passwords
        if (!ignoreCurrentPassword) {
            // if the password.accept.encrypted.and.plain property in security is set to true allow plain or encrypted passwords
            boolean passwordMatches = checkPassword(userLogin.getString("currentPassword"), useEncryption, currentPassword);
            if ((currentPassword == null) || (!passwordMatches)) {
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.old_password_not_correct_reenter", locale);
                errorMessageList.add(errMsg);
            }
            if (checkPassword(userLogin.getString("currentPassword"), useEncryption, newPassword)) {
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.new_password_is_equal_to_old_password", locale);
                errorMessageList.add(errMsg);
            }

        }

        if (UtilValidate.isEmpty(newPassword) || UtilValidate.isEmpty(newPasswordVerify)) {
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_or_verify_missing", locale);
            errorMessageList.add(errMsg);
        } else if (!newPassword.equals(newPasswordVerify)) {
            errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_did_not_match_verify_password", locale);
            errorMessageList.add(errMsg);
        }

        int passwordChangeHistoryLimit = 0;
        try {
            passwordChangeHistoryLimit = EntityUtilProperties.getPropertyAsInteger("security", "password.change.history.limit", 0);
        } catch (NumberFormatException nfe) {
            // No valid value is found so don't bother to save any password history
            passwordChangeHistoryLimit = 0;
        }
        Debug.logInfo(" password.change.history.limit is set to " + passwordChangeHistoryLimit, MODULE);
        if (passwordChangeHistoryLimit > 0) {
            Debug.logInfo(" checkNewPassword Checking if user is tyring to use old password " + passwordChangeHistoryLimit, MODULE);
            try {
                List<GenericValue> pwdHistList = EntityQuery.use(delegator).from("UserLoginPasswordHistory")
                        .where("userLoginId", userLogin.getString("userLoginId")).orderBy("-fromDate").queryList();
                for (GenericValue pwdHistValue : pwdHistList) {
                    if (checkPassword(pwdHistValue.getString("currentPassword"), useEncryption, newPassword)) {
                        Map<String, Integer> messageMap = UtilMisc.toMap("passwordChangeHistoryLimit", passwordChangeHistoryLimit);
                        errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_must_be_different_from_last_passwords", messageMap,
                                locale);
                        errorMessageList.add(errMsg);
                        break;
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "", MODULE);
                Map<String, String> messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
                errMsg = UtilProperties.getMessage(RESOURCE, "loginevents.error_accessing_password_change_history", messageMap, locale);
            }
        }
        int minPasswordLength = 0;

        try {
            minPasswordLength = EntityUtilProperties.getPropertyAsInteger("security", "password.length.min", 0);
        } catch (NumberFormatException nfe) {
            minPasswordLength = 0;
        }

        if (newPassword != null) {
            // Matching password with pattern
            String passwordPattern = EntityUtilProperties.getPropertyValue("security", "security.login.password.pattern", "^.*(?=.{5,}).*$",
                    delegator);
            boolean usePasswordPattern = UtilProperties.getPropertyAsBoolean("security", "security.login.password.pattern.enable", true);
            if (usePasswordPattern) {
                Pattern pattern = Pattern.compile(passwordPattern);
                Matcher matcher = pattern.matcher(newPassword);
                boolean matched = matcher.matches();
                if (!matched) {
                    // This is a mix to handle the OOTB pattern which is only a fixed length
                    Map<String, String> messageMap = UtilMisc.toMap("minPasswordLength", Integer.toString(minPasswordLength));
                    String passwordPatternMessage = EntityUtilProperties.getPropertyValue("security", "security.login.password.pattern.description",
                            "loginservices.password_must_be_least_characters_long", delegator);
                    errMsg = UtilProperties.getMessage(RESOURCE, passwordPatternMessage, messageMap, locale);
                    errorMessageList.add(errMsg);
                }
            } else {
                if (!(newPassword.length() >= minPasswordLength)) {
                    Map<String, String> messageMap = UtilMisc.toMap("minPasswordLength", Integer.toString(minPasswordLength));
                    errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_must_be_least_characters_long", messageMap, locale);
                    errorMessageList.add(errMsg);
                }
            }
            if (newPassword.equalsIgnoreCase(userLogin.getString("userLoginId"))) {
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_may_not_equal_username", locale);
                errorMessageList.add(errMsg);
            }
            if (UtilValidate.isNotEmpty(passwordHint)
                    && (passwordHint.toUpperCase(Locale.getDefault()).indexOf(newPassword.toUpperCase(Locale.getDefault())) >= 0)) {
                errMsg = UtilProperties.getMessage(RESOURCE, "loginservices.password_hint_may_not_contain_password", locale);
                errorMessageList.add(errMsg);
            }
        }
    }

    public static String getHashType() {
        String hashType = UtilProperties.getPropertyValue("security", "password.encrypt.hash.type");

        if (UtilValidate.isEmpty(hashType)) {
            Debug.logWarning("Password encrypt hash type is not specified in security.properties, use SHA", MODULE);
            hashType = "SHA";
        }

        return hashType;
    }

    public static boolean checkPassword(String oldPassword, boolean useEncryption, String currentPassword) {
        boolean passwordMatches = false;
        if (oldPassword != null) {
            if (useEncryption) {
                passwordMatches = HashCrypt.comparePassword(oldPassword, getHashType(), currentPassword);
            } else {
                passwordMatches = oldPassword.equals(currentPassword);
            }
        }
        if (!passwordMatches && "true".equals(UtilProperties.getPropertyValue("security", "password.accept.encrypted.and.plain"))) {
            passwordMatches = currentPassword.equals(oldPassword);
        }
        return passwordMatches;
    }

    private static boolean tomcatSSOLogin(HttpServletRequest request, String userName, String currentPassword) {
        try {
            request.login(userName, currentPassword);
        } catch (ServletException e) {

            StringManager sm = StringManager.getManager("org.apache.catalina.connector");
            if (sm.getString("coyoteRequest.alreadyAuthenticated").equals(e.getMessage())) {
                return true;
            } else {
                Debug.logError(e, MODULE);
                return false;
            }
        }
        return true;
    }
}
