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

package org.ofbiz.crowd;

import java.rmi.RemoteException;
import java.util.Map;
import java.io.Serializable;
import java.sql.Timestamp;

import javax.transaction.Transaction;

import org.ofbiz.common.authentication.api.Authenticator;
import org.ofbiz.common.authentication.api.AuthenticatorException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.crowd.user.UserWrapper;
import javolution.util.FastMap;

/**
 * CrowdAuthenticator
 */
public class CrowdAuthenticator extends CrowdWorker implements Authenticator {

    private static final String module = CrowdAuthenticator.class.getName();
    private static final String props = "crowd.properties";

    protected LocalDispatcher dispatcher;
    protected Delegator delegator;

    /**
     * Method called when authenticator is first initialized (the delegator
     * object can be obtained from the LocalDispatcher)
     *
     * @param dispatcher The LocalDispatcher to use for this Authenticator
     */
    public void initialize(LocalDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.delegator = dispatcher.getDelegator();
    }

    /**
     * Method to authenticate a user
     *
     * @param username      User's username
     * @param password      User's password
     * @param isServiceAuth true if authentication is for a service call
     * @return true if the user is authenticated
     * @throws org.ofbiz.common.authentication.api.AuthenticatorException
     *          when a fatal error occurs during authentication
     */
    public boolean authenticate(String username, String password, boolean isServiceAuth) throws AuthenticatorException {
        String token;
        try {
            token = callAuthenticate(username, password);
        } catch (RemoteException e) {
            throw new AuthenticatorException(e.getMessage(), e);
        }
        Debug.logInfo("Crowd auth called; returned token: " + token, module);
        return token != null;
    }

    /**
     * Logs a user out
     *
     * @param username User's username
     * @throws org.ofbiz.common.authentication.api.AuthenticatorException
     *          when logout fails
     */
    public void logout(String username) throws AuthenticatorException {
    }

    /**
     * Reads user information and syncs it to OFBiz (i.e. UserLogin, Person, etc)
     *
     * @param username User's username
     * @throws org.ofbiz.common.authentication.api.AuthenticatorException
     *          user synchronization fails
     */
    public void syncUser(String username) throws AuthenticatorException {
        UserWrapper user;
        try {
            user = callGetUser(username);
        } catch (RemoteException e) {
            throw new AuthenticatorException(e.getMessage(), e);
        }

        GenericValue system;
        try {
            system = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), true);
        } catch (GenericEntityException e) {
            throw new AuthenticatorException(e.getMessage(), e);
        }

        GenericValue userLogin;
        try {
            userLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", user.getName()), false);
        } catch (GenericEntityException e) {
            throw new AuthenticatorException(e.getMessage(), e);
        }

        // suspend the current transaction and load the user
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

                if (userLogin == null) {
                    // create the user
                    createUser(user, system);
                } else {
                    // update the user information
                    updateUser(user, system, userLogin);
                }

            } catch (GenericTransactionException e) {
                Debug.logError(e, "Could not suspend transaction: " + e.getMessage(), module);
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
    }

    private void createUser(UserWrapper user, GenericValue system) throws AuthenticatorException {
        // create person + userLogin
        Map<String, Serializable> createPersonUlMap = FastMap.newInstance();
        createPersonUlMap.put("firstName", user.getUserAttributeMapper().getFirstName());
        createPersonUlMap.put("lastName", user.getUserAttributeMapper().getLastName());
        createPersonUlMap.put("externalAuthId", user.getName());
        createPersonUlMap.put("externalId", user.getName());
        createPersonUlMap.put("userLoginId", user.getName());
        createPersonUlMap.put("currentPassword", "[EXTERNAL]");
        createPersonUlMap.put("currentPasswordVerify", "[EXTERNAL]");
        createPersonUlMap.put("userLogin", system);
        Map<String, Object> createPersonResult;
        try {
            createPersonResult = dispatcher.runSync("createPersonAndUserLogin", createPersonUlMap);
        } catch (GenericServiceException e) {
            throw new AuthenticatorException(e.getMessage(), e);
        }
        if (ServiceUtil.isError(createPersonResult)) {
            throw new AuthenticatorException(ServiceUtil.getErrorMessage(createPersonResult));
        }
        String partyId = (String) createPersonResult.get("partyId");

        // create email
        Map<String, Serializable> createEmailMap = FastMap.newInstance();
        createEmailMap.put("emailAddress", user.getUserAttributeMapper().getEmail());
        createEmailMap.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");
        createEmailMap.put("partyId", partyId);
        createEmailMap.put("userLogin", system);
        Map<String, Object> createEmailResult;
        try {
            createEmailResult = dispatcher.runSync("createPartyEmailAddress", createEmailMap);
        } catch (GenericServiceException e) {
            throw new AuthenticatorException(e.getMessage(), e);
        }
        if (ServiceUtil.isError(createEmailResult)) {
            throw new AuthenticatorException(ServiceUtil.getErrorMessage(createEmailResult));
        }

        // create security group(s)
        Timestamp now = UtilDateTime.nowTimestamp();
        for (String securityGroup : user.getUserGroupMapper().getSecurityGroups()) {
            // check and make sure the security group exists
            GenericValue secGroup = null;
            try {
                secGroup = delegator.findOne("SecurityGroup", UtilMisc.toMap("groupId", securityGroup), true);
            } catch (GenericEntityException e) {
                Debug.logError(e, e.getMessage(), module);
            }

            // add it to the user if it exists
            if (secGroup != null) {
                Map<String, Serializable> createSecGrpMap = FastMap.newInstance();
                createSecGrpMap.put("userLoginId", user.getName());
                createSecGrpMap.put("groupId", securityGroup);
                createSecGrpMap.put("fromDate", now);
                createSecGrpMap.put("userLogin", system);

                Map<String, Object> createSecGrpResult;
                try {
                    createSecGrpResult = dispatcher.runSync("addUserLoginToSecurityGroup", createSecGrpMap);
                } catch (GenericServiceException e) {
                    throw new AuthenticatorException(e.getMessage(), e);
                }
                if (ServiceUtil.isError(createSecGrpResult)) {
                    throw new AuthenticatorException(ServiceUtil.getErrorMessage(createSecGrpResult));
                }
            }
        }
    }

    private void updateUser(UserWrapper user, GenericValue system, GenericValue userLogin) throws AuthenticatorException {
        // TODO implement me
    }

    /**
     * Updates a user's password
     *
     * @param username    User's username
     * @param password    User's current password
     * @param newPassword User's new password
     * @throws org.ofbiz.common.authentication.api.AuthenticatorException
     *          when update password fails
     */
    public void updatePassword(String username, String password, String newPassword) throws AuthenticatorException {
        Debug.logInfo("Calling Crowd:updatePassword() - " + newPassword, module);
        try {
            callUpdatePassword(username, newPassword);
        } catch (RemoteException e) {
            throw new AuthenticatorException(e.getMessage(), e);
        }
    }

    /**
     * Weight of this authenticator (lower weights are run first)
     *
     * @return the weight of this Authenicator
     */
    public float getWeight() {
        return 0;
    }

    /**
     * Is the user synchronzied back to OFBiz
     *
     * @return true if the user record is copied to the OFB database
     */
    public boolean isUserSynchronized() {
        return true;
    }

    /**
     * Is this expected to be the only authenticator, if so errors will be thrown when users cannot be found
     *
     * @return true if this is expected to be the only Authenticator
     */
    public boolean isSingleAuthenticator() {
        return false;
    }

    /**
     * Flag to test if this Authenticator is enabled
     *
     * @return true if the Authenticator is enabled
     */
    public boolean isEnabled() {
        return "true".equalsIgnoreCase(UtilProperties.getPropertyValue(props, "crowd.authenticator.enabled", "false"));
    }
}
