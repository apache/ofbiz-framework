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

package org.apache.ofbiz.common.authentication.example;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.common.authentication.api.Authenticator;
import org.apache.ofbiz.common.authentication.api.AuthenticatorException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * LocalAuthenticator
 */
public class TestFailAuthenticator implements Authenticator {

    private static final String module = TestFailAuthenticator.class.getName();
    protected Delegator delegator;
    protected LocalDispatcher dispatcher;
    protected float weight = 1;

    /**
     * Method called when authenticator is first initialized (the delegator
     * object can be obtained from the LocalDispatcher)
     *
     * @param dispatcher The LocalDispatcher to use for this Authenticator
     */
    @Override
    public void initialize(LocalDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.delegator = dispatcher.getDelegator();
        Debug.logInfo(this.getClass().getName() + " Authenticator initialized", module);
    }

    /**
     * Method to authenticate a user
     *
     * @param username      User's username
     * @param password      User's password
     * @param isServiceAuth true if authentication is for a service call
     * @return true if the user is authenticated
     * @throws org.apache.ofbiz.common.authentication.api.AuthenticatorException
     *          when a fatal error occurs during authentication
     */
    @Override
    public boolean authenticate(String username, String password, boolean isServiceAuth) throws AuthenticatorException {
        Debug.logInfo(this.getClass().getName() + " Authenticator authenticate() -- returning false", module);
        return false;
    }

    /**
     * Logs a user out
     *
     * @param username User's username
     * @throws org.apache.ofbiz.common.authentication.api.AuthenticatorException
     *          when logout fails
     */
    @Override
    public void logout(String username) throws AuthenticatorException {
        Debug.logInfo(this.getClass().getName() + " Authenticator logout()", module);
    }

    /**
     * Reads user information and syncs it to OFBiz (i.e. UserLogin, Person, etc)
     *
     * @param username User's username
     * @throws org.apache.ofbiz.common.authentication.api.AuthenticatorException
     *          user synchronization fails
     */
    @Override
    public void syncUser(String username) throws AuthenticatorException {
        Debug.logInfo(this.getClass().getName() + " Authenticator syncUser()", module);
        // no user info to sync
    }

    /**
     * Updates a user's password
     *
     * @param username    User's username
     * @param password    User's current password
     * @param newPassword User's new password
     * @throws org.apache.ofbiz.common.authentication.api.AuthenticatorException
     *          when update password fails
     */
    @Override
    public void updatePassword(String username, String password, String newPassword) throws AuthenticatorException {
        Debug.logInfo(this.getClass().getName() + " Authenticator updatePassword()", module);
    }

    /**
     * Weight of this authenticator (lower weights are run first)
     *
     * @return the weight of this Authenticator
     */
    @Override
    public float getWeight() {
        return 1;
    }

    /**
     * Is the user synchronized back to OFBiz
     *
     * @return true if the user record is copied to the OFB database
     */
    @Override
    public boolean isUserSynchronized() {
        Debug.logInfo(this.getClass().getName() + " Authenticator isUserSynchronized()", module);
        return true;
    }

    /**
     * Is this expected to be the only authenticator, if so errors will be thrown when users cannot be found
     *
     * @return true if this is expected to be the only Authenticator
     */
    @Override
    public boolean isSingleAuthenticator() {
        Debug.logInfo(this.getClass().getName() + " Authenticator isSingleAuthenticator()", module);
        return false;
    }

    /**
     * Flag to test if this Authenticator is enabled
     *
     * @return true if the Authenticator is enabled
     */
    @Override
    public boolean isEnabled() {
        return false;
    }
}
