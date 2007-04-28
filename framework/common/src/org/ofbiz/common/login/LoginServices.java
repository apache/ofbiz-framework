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

package org.ofbiz.common.login;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.transaction.Transaction;

import org.ofbiz.base.crypto.HashCrypt;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * <b>Title:</b> Login Services
 */
public class LoginServices {

    public static final String module = LoginServices.class.getName();
    public static final String resource = "SecurityextUiLabels";

    /** Login service to authenticate username and password
     * @return Map of results including (userLogin) GenericValue object
     */
    public static Map userLogin(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        boolean useEncryption = "true".equals(UtilProperties.getPropertyValue("security.properties", "password.encrypt"));

        // if isServiceAuth is not specified, default to not a service auth
        boolean isServiceAuth = context.get("isServiceAuth") != null && ((Boolean) context.get("isServiceAuth")).booleanValue();

        String username = (String) context.get("login.username");
        if (username == null) username = (String) context.get("username");
        String password = (String) context.get("login.password");
        if (password == null) password = (String) context.get("password");

        // get the visitId for the history entity
        String visitId = (String) context.get("visitId");

        String errMsg = "";
        if (username == null || username.length() <= 0) {
            errMsg = UtilProperties.getMessage(resource,"loginservices.username_missing", locale);
        } else if (password == null || password.length() <= 0) {
            errMsg = UtilProperties.getMessage(resource,"loginservices.password_missing", locale);
        } else {
            String realPassword = useEncryption ? LoginServices.getPasswordHash(password) : password;

            boolean repeat = true;
            // starts at zero but it incremented at the beggining so in the first pass passNumber will be 1
            int passNumber = 0;

            while (repeat) {
                repeat = false;
                // pass number is incremented here because there are continues in this loop so it may never get to the end
                passNumber++;

                GenericValue userLogin = null;

                try {
                    // only get userLogin from cache for service calls; for web and other manual logins there is less time sensitivity
                    if (isServiceAuth) {
                        userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", username));
                    } else {
                        userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", username));
                    }
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, "", module);
                }

                if (userLogin != null) {
                    String ldmStr = UtilProperties.getPropertyValue("security.properties", "login.disable.minutes");
                    long loginDisableMinutes = 30;

                    try {
                        loginDisableMinutes = Long.parseLong(ldmStr);
                    } catch (Exception e) {
                        loginDisableMinutes = 30;
                        Debug.logWarning("Could not parse login.disable.minutes from security.properties, using default of 30", module);
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
                    boolean isSystem = (isServiceAuth && userLogin.get("isSystem") != null) ?
                            "Y".equalsIgnoreCase(userLogin.getString("isSystem")) : false;

                    // grab the hasLoggedOut flag
                    boolean hasLoggedOut = userLogin.get("hasLoggedOut") != null ?
                            "Y".equalsIgnoreCase(userLogin.getString("hasLoggedOut")) : false;

                    if (UtilValidate.isEmpty(userLogin.getString("enabled")) || "Y".equals(userLogin.getString("enabled")) ||
                        (reEnableTime != null && reEnableTime.before(UtilDateTime.nowTimestamp())) || (isSystem)) {

                        String successfulLogin;

                        if (!isSystem) {
                            userLogin.set("enabled", "Y");
                        }

                        // if the password.accept.encrypted.and.plain property in security is set to true allow plain or encrypted passwords
                        // if this is a system account don't bother checking the passwords
                        if ((userLogin.get("currentPassword") != null &&
                            (realPassword.equals(userLogin.getString("currentPassword")) ||
                                ("true".equals(UtilProperties.getPropertyValue("security.properties", "password.accept.encrypted.and.plain")) && password.equals(userLogin.getString("currentPassword")))))) {
                            Debug.logVerbose("[LoginServices.userLogin] : Password Matched", module);

                            // update the hasLoggedOut flag
                            if (hasLoggedOut) {
                                userLogin.set("hasLoggedOut", "N");
                            }

                            // reset failed login count if necessry
                            Long currentFailedLogins = userLogin.getLong("successiveFailedLogins");
                            if (currentFailedLogins != null && currentFailedLogins.longValue() > 0) {
                                userLogin.set("successiveFailedLogins", new Long(0));
                            } else if (!hasLoggedOut) {                                                                                            
                                // successful login & no loggout flag, no need to change anything, so don't do the store
                                doStore = false;
                            }

                            successfulLogin = "Y";

                            if (!isServiceAuth) {
                                // get the UserLoginSession if this is not a service auth
                                Map userLoginSessionMap = getUserLoginSession(userLogin);
                                GenericValue userLoginSession = null;

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
                            if (isServiceAuth && passNumber <= 1) {
                                delegator.clearCacheLine("UserLogin", UtilMisc.toMap("userLoginId", username));
                                repeat = true;
                                continue;
                            }

                            Debug.logInfo("[LoginServices.userLogin] : Password Incorrect", module);
                            // password invalid...
                            errMsg = UtilProperties.getMessage(resource,"loginservices.password_incorrect", locale);

                            // increment failed login count
                            Long currentFailedLogins = userLogin.getLong("successiveFailedLogins");

                            if (currentFailedLogins == null) {
                                currentFailedLogins = new Long(1);
                            } else {
                                currentFailedLogins = new Long(currentFailedLogins.longValue() + 1);
                            }
                            userLogin.set("successiveFailedLogins", currentFailedLogins);

                            // if failed logins over amount in properties file, disable account
                            String mflStr = UtilProperties.getPropertyValue("security.properties", "max.failed.logins");
                            long maxFailedLogins = 3;
                            try {
                                maxFailedLogins = Long.parseLong(mflStr);
                            } catch (Exception e) {
                                maxFailedLogins = 3;
                                Debug.logWarning("Could not parse max.failed.logins from security.properties, using default of 3", module);
                            }

                            if (maxFailedLogins > 0 && currentFailedLogins.longValue() >= maxFailedLogins) {
                                userLogin.set("enabled", "N");
                                userLogin.set("disabledDateTime", UtilDateTime.nowTimestamp());
                            }

                            successfulLogin = "N";
                        }

                        // this section is being done in its own transaction rather than in the
                        //current/existing transaction because we may return error and we don't
                        //want that to stop this from getting stored
                        Transaction parentTx = null;
                        boolean beganTransaction = false;

                        try {
                            try {
                                parentTx = TransactionUtil.suspend();
                            } catch (GenericTransactionException e) {
                                Debug.logError(e, "Could not suspend transaction: " + e.getMessage(), module);
                            }                                

                            try {
                                beganTransaction = TransactionUtil.begin();
    
                                if (doStore) {
                                    userLogin.store();
                                }
    
                                if ("true".equals(UtilProperties.getPropertyValue("security.properties", "store.login.history"))) {
                                    boolean createHistory = true;
                                    
                                    // only save info on service auth if option set to true to do so
                                    if (isServiceAuth && !"true".equals(UtilProperties.getPropertyValue("security.properties", "store.login.history.on.service.auth"))) {
                                        createHistory = false;
                                    }
    
                                    if (createHistory) {
                                        Map ulhCreateMap = UtilMisc.toMap("userLoginId", username, "visitId", visitId,
                                                "fromDate", UtilDateTime.nowTimestamp(), "successfulLogin", successfulLogin);
                                        
                                        ModelEntity modelUserLogin = userLogin.getModelEntity();
                                        if (modelUserLogin.isField("partyId")) {
                                            ulhCreateMap.put("partyId", userLogin.get("partyId"));
                                        }
    
                                        // ONLY save the password if it was incorrect
                                        if ("N".equals(successfulLogin) && !"false".equals(UtilProperties.getPropertyValue("security.properties", "store.login.history.incorrect.password"))) {
                                            ulhCreateMap.put("passwordUsed", password);
                                        }
                                        
                                        //Debug.logInfo(new Exception(), "=================== Creating new UserLoginHistory at " + UtilDateTime.nowTimestamp(), module);
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
                                    Debug.logError(e2, "Could not rollback nested transaction: " + e2.getMessage(), module);
                                }
                                
                                // if doStore is true then this error should not be ignored and we shouldn't consider it a successful login if this happens as there is something very wrong lower down that will bite us again later
                                if (doStore) {
                                    return ServiceUtil.returnError(geeErrMsg);
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
                                } catch (GenericTransactionException e) {
                                    Debug.logError(e, "Could not resume parent nested transaction: " + e.getMessage(), module);
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

                        Map messageMap = UtilMisc.toMap("username", username);
                        errMsg = UtilProperties.getMessage(resource,"loginservices.account_for_user_login_id_disabled",messageMap ,locale);
                        if (disabledDateTime != null) {
                            messageMap = UtilMisc.toMap("disabledDateTime", disabledDateTime);
                            errMsg += UtilProperties.getMessage(resource,"loginservices.since_datetime",messageMap ,locale);
                        } else {
                            errMsg += ".";
                        }

                        if (loginDisableMinutes > 0 && reEnableTime != null) {
                            messageMap = UtilMisc.toMap("reEnableTime", reEnableTime);
                            errMsg += UtilProperties.getMessage(resource,"loginservices.will_be_reenabled",messageMap ,locale);
                        } else {
                            errMsg += UtilProperties.getMessage(resource,"loginservices.not_scheduled_to_be_reenabled",locale);
                        }
                    }
                } else {
                    // userLogin record not found, user does not exist
                    errMsg = UtilProperties.getMessage(resource, "loginservices.user_not_found", locale);
                    Debug.logInfo("[LoginServices.userLogin] : Invalid User : " + errMsg, module);
                }
            }
        }

        if (errMsg.length() > 0) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
        }
        return result;
    }

    /** Creates a UserLogin
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createUserLogin(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue loggedInUserLogin = (GenericValue) context.get("userLogin");
        List errorMessageList = new LinkedList();
        Locale locale = (Locale) context.get("locale");

        boolean useEncryption = "true".equals(UtilProperties.getPropertyValue("security.properties", "password.encrypt"));

        String userLoginId = (String) context.get("userLoginId");
        String partyId = (String) context.get("partyId");
        String currentPassword = (String) context.get("currentPassword");
        String currentPasswordVerify = (String) context.get("currentPasswordVerify");
        String enabled = (String) context.get("enabled");
        String passwordHint = (String) context.get("passwordHint");
        String errMsg = null;

        // security: don't create a user login if the specified partyId (if not empty) already exists
        // unless the logged in user has permission to do so (same partyId or PARTYMGR_CREATE)
        if (partyId != null && partyId.length() > 0) {
            GenericValue party = null;

            try {
                party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "", module);
            }

            if (party != null) {
                if (loggedInUserLogin != null) {
                    // <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
                    if (!partyId.equals(loggedInUserLogin.getString("partyId"))) {
                        if (!security.hasEntityPermission("PARTYMGR", "_CREATE", loggedInUserLogin)) {

                            errMsg = UtilProperties.getMessage(resource,"loginservices.party_with_specified_party_ID_exists_not_have_permission", locale);
                            errorMessageList.add(errMsg);
                        }
                    }
                } else {
                    errMsg = UtilProperties.getMessage(resource,"loginservices.must_be_logged_in_and_permission_create_login_party_ID_exists", locale);
                    errorMessageList.add(errMsg);
                }
            }
        }

        checkNewPassword(null, null, currentPassword, currentPasswordVerify, passwordHint, errorMessageList, true, locale);

        GenericValue userLoginToCreate = delegator.makeValue("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
        userLoginToCreate.set("passwordHint", passwordHint);
        userLoginToCreate.set("enabled", enabled);
        userLoginToCreate.set("partyId", partyId);
        userLoginToCreate.set("currentPassword", useEncryption ? getPasswordHash(currentPassword) : currentPassword);

        try {
            if (delegator.findByPrimaryKey(userLoginToCreate.getPrimaryKey()) != null) {
                Map messageMap = UtilMisc.toMap("userLoginId", userLoginId);
                errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_create_login_user_with_ID_exists", messageMap, locale);
                errorMessageList.add(errMsg);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", module);
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_create_login_user_read_failure", messageMap, locale);
            errorMessageList.add(errMsg);
        }

        if (errorMessageList.size() > 0) {
            return ServiceUtil.returnError(errorMessageList);
        }

        try {
            userLoginToCreate.create();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", module);
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_create_login_user_write_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /** Updates UserLogin Password info
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map updatePassword(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue loggedInUserLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        boolean useEncryption = "true".equals(UtilProperties.getPropertyValue("security.properties", "password.encrypt"));
        boolean adminUser = false;

        String userLoginId = (String) context.get("userLoginId");
        String errMsg = null;

        if (userLoginId == null || userLoginId.length() == 0) {
            userLoginId = loggedInUserLogin.getString("userLoginId");
        }

        // <b>security check</b>: userLogin userLoginId must equal userLoginId, or must have PARTYMGR_UPDATE permission
        // NOTE: must check permission first so that admin users can set own password without specifying old password
        if (!security.hasEntityPermission("PARTYMGR", "_UPDATE", loggedInUserLogin)) {
            if (!userLoginId.equals(loggedInUserLogin.getString("userLoginId"))) {
                errMsg = UtilProperties.getMessage(resource,"loginservices.not_have_permission_update_password_for_user_login", locale);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            adminUser = true;
        }

        GenericValue userLoginToUpdate = null;

        try {
            userLoginToUpdate = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_change_password_read_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (userLoginToUpdate == null) {
            Map messageMap = UtilMisc.toMap("userLoginId", userLoginId);
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_change_password_userlogin_with_id_not_exist", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        String currentPassword = (String) context.get("currentPassword");
        String newPassword = (String) context.get("newPassword");
        String newPasswordVerify = (String) context.get("newPasswordVerify");
        String passwordHint = (String) context.get("passwordHint");

        if ("true".equals(UtilProperties.getPropertyValue("security.properties", "password.lowercase"))) {
            currentPassword = currentPassword.toLowerCase();
            newPassword = newPassword.toLowerCase();
            newPasswordVerify = newPasswordVerify.toLowerCase();
        }

        List errorMessageList = new LinkedList();

        if (newPassword != null && newPassword.length() > 0) {
            checkNewPassword(userLoginToUpdate, currentPassword, newPassword, newPasswordVerify,
                passwordHint, errorMessageList, adminUser, locale);
        }

        if (errorMessageList.size() > 0) {
            return ServiceUtil.returnError(errorMessageList);
        }

        userLoginToUpdate.set("currentPassword", useEncryption ? getPasswordHash(newPassword) : newPassword, false);
        userLoginToUpdate.set("passwordHint", passwordHint, false);

        try {
            userLoginToUpdate.store();
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_change_password_write_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        result.put("updatedUserLogin", userLoginToUpdate);
        return result;
    }

    /** Updates the UserLoginId for a party, replicating password, etc from
     *    current login and expiring the old login.
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map updateUserLoginId(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue loggedInUserLogin = (GenericValue) context.get("userLogin");
        List errorMessageList = new LinkedList();
        Locale locale = (Locale) context.get("locale");

        //boolean useEncryption = "true".equals(UtilProperties.getPropertyValue("security.properties", "password.encrypt"));

        String userLoginId = (String) context.get("userLoginId");
        String errMsg = null;

        if ((userLoginId != null) && ("true".equals(UtilProperties.getPropertyValue("security.properties", "username.lowercase")))) {
            userLoginId = userLoginId.toLowerCase();
        }

        String partyId = loggedInUserLogin.getString("partyId");
        String password = loggedInUserLogin.getString("currentPassword");
        String passwordHint = loggedInUserLogin.getString("passwordHint");

        // security: don't create a user login if the specified partyId (if not empty) already exists
        // unless the logged in user has permission to do so (same partyId or PARTYMGR_CREATE)
        if (partyId != null || partyId.length() > 0) {
            //GenericValue party = null;
            //try {
            //    party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
            //} catch (GenericEntityException e) {
            //    Debug.logWarning(e, "", module);
            //}

            if (loggedInUserLogin != null) {
                // security check: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
                if (!partyId.equals(loggedInUserLogin.getString("partyId"))) {
                    errMsg = UtilProperties.getMessage(resource,"loginservices.party_with_party_id_exists_not_permission_create_user_login", locale);
                    errorMessageList.add(errMsg);
                }
            } else {
                errMsg = UtilProperties.getMessage(resource,"loginservices.must_logged_in_have_permission_create_user_login_exists", locale);
                errorMessageList.add(errMsg);
            }
        }

        GenericValue newUserLogin = null;
        boolean doCreate = true;

        // check to see if there's a matching login and use it if it's for the same party
        try {
            newUserLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", module);
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_create_login_user_read_failure", messageMap, locale);
            errorMessageList.add(errMsg);
        }

        if (newUserLogin != null) {
            if (!newUserLogin.get("partyId").equals(partyId)) {
                Map messageMap = UtilMisc.toMap("userLoginId", userLoginId);
                errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_create_login_user_with_ID_exists", messageMap, locale);
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
            Debug.logWarning(e, "", module);
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_create_login_user_write_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        loggedInUserLogin.set("enabled", "N");
        loggedInUserLogin.set("disabledDateTime", UtilDateTime.nowTimestamp());

        try {
            loggedInUserLogin.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "", module);
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_disable_old_login_user_write_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        result.put("newUserLogin", newUserLogin);
        return result;
    }

    /** Updates UserLogin Security info
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map updateUserLoginSecurity(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue loggedInUserLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String userLoginId = (String) context.get("userLoginId");
        String errMsg = null;

        if (userLoginId == null || userLoginId.length() == 0) {
            userLoginId = loggedInUserLogin.getString("userLoginId");
        }

        // <b>security check</b>: must have PARTYMGR_UPDATE permission
        if (!security.hasEntityPermission("PARTYMGR", "_UPDATE", loggedInUserLogin) && !security.hasEntityPermission("SECURITY", "_UPDATE", loggedInUserLogin)) {
            errMsg = UtilProperties.getMessage(resource,"loginservices.not_permission_update_security_info_for_user_login", locale);
            return ServiceUtil.returnError(errMsg);
        }

        GenericValue userLoginToUpdate = null;

        try {
            userLoginToUpdate = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_change_password_read_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (userLoginToUpdate == null) {
            Map messageMap = UtilMisc.toMap("userLoginId", userLoginId);
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_change_password_userlogin_with_id_not_exist", messageMap, locale);
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

        // if was disabled and we are enabling it, clear disabledDateTime
        if (!wasEnabled && "Y".equals(context.get("enabled"))) {
            userLoginToUpdate.set("disabledDateTime", null);
        }

        // if was enabled and we are disabling it, and no disabledDateTime was passed, set it to now
        if (wasEnabled && "N".equals(context.get("enabled")) && context.get("disabledDateTime") == null) {
            userLoginToUpdate.set("disabledDateTime", UtilDateTime.nowTimestamp());
        }

        try {
            userLoginToUpdate.store();
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errorMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"loginservices.could_not_change_password_write_failure", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static void checkNewPassword(GenericValue userLogin, String currentPassword, String newPassword, String newPasswordVerify, String passwordHint, List errorMessageList, boolean ignoreCurrentPassword, Locale locale) {
        boolean useEncryption = "true".equals(UtilProperties.getPropertyValue("security.properties", "password.encrypt"));

        String errMsg = null;

        if (!ignoreCurrentPassword) {
            String realPassword = currentPassword;

            if (useEncryption && currentPassword != null) {
                realPassword = LoginServices.getPasswordHash(currentPassword);
            }
            // if the password.accept.encrypted.and.plain property in security is set to true allow plain or encrypted passwords
            boolean passwordMatches = currentPassword != null && (realPassword.equals(userLogin.getString("currentPassword")) ||
                    ("true".equals(UtilProperties.getPropertyValue("security.properties", "password.accept.encrypted.and.plain")) && currentPassword.equals(userLogin.getString("currentPassword"))));

            if ((currentPassword == null) || (userLogin != null && currentPassword != null && !passwordMatches)) {
                errMsg = UtilProperties.getMessage(resource,"loginservices.old_password_not_correct_reenter", locale);
                errorMessageList.add(errMsg);
            }
        }

        if (!UtilValidate.isNotEmpty(newPassword) || !UtilValidate.isNotEmpty(newPasswordVerify)) {
            errMsg = UtilProperties.getMessage(resource,"loginservices.password_or_verify_missing", locale);
            errorMessageList.add(errMsg);
        } else if (!newPassword.equals(newPasswordVerify)) {
            errMsg = UtilProperties.getMessage(resource,"loginservices.password_did_not_match_verify_password", locale);
            errorMessageList.add(errMsg);
        }

        int minPasswordLength = 0;

        try {
            minPasswordLength = Integer.parseInt(UtilProperties.getPropertyValue("security.properties", "password.length.min", "0"));
        } catch (NumberFormatException nfe) {
            minPasswordLength = 0;
        }

        if (newPassword != null) {
            if (!(newPassword.length() >= minPasswordLength)) {
                Map messageMap = UtilMisc.toMap("minPasswordLength", Integer.toString(minPasswordLength));
                errMsg = UtilProperties.getMessage(resource,"loginservices.password_must_be_least_characters_long", messageMap, locale);
                errorMessageList.add(errMsg);
            }
            if (userLogin != null && newPassword.equalsIgnoreCase(userLogin.getString("userLoginId"))) {
                errMsg = UtilProperties.getMessage(resource,"loginservices.password_may_not_equal_username", locale);
                errorMessageList.add(errMsg);
            }
            if (UtilValidate.isNotEmpty(passwordHint) && (passwordHint.toUpperCase().indexOf(newPassword.toUpperCase()) >= 0)) {
                errMsg = UtilProperties.getMessage(resource,"loginservices.password_hint_may_not_contain_password", locale);
                errorMessageList.add(errMsg);
            }
        }
    }

    public static String getPasswordHash(String str) {
        String hashType = UtilProperties.getPropertyValue("security.properties", "password.encrypt.hash.type");

        if (hashType == null || hashType.length() == 0) {
            Debug.logWarning("Password encrypt hash type is not specified in security.properties, use SHA", module);
            hashType = "SHA";
        }

        return HashCrypt.getDigestHash(str, hashType);
    }

    public static Map getUserLoginSession(GenericValue userLogin) {
        GenericDelegator delegator = userLogin.getDelegator();
        GenericValue userLoginSession;
        Map userLoginSessionMap = null;
        try {
            userLoginSession = userLogin.getRelatedOne("UserLoginSession");
            if (userLoginSession != null) {
                Object deserObj = XmlSerializer.deserialize(userLoginSession.getString("sessionData"), delegator);
                //don't check, just cast, if it fails it will get caught and reported below; if (deserObj instanceof Map)
                userLoginSessionMap = (Map) deserObj;
            }
        } catch (GenericEntityException ge) {
            Debug.logWarning(ge, "Cannot get UserLoginSession for UserLogin ID: " +
                    userLogin.getString("userLoginId"), module);
        } catch (Exception e) {
            Debug.logWarning(e, "Problems deserializing UserLoginSession", module);
        }
        return userLoginSessionMap;
    }
}
